package com.yzq.cordova_webcontainer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.yzq.cordova_webcontainer.core.CordovaJsInterface
import com.yzq.cordova_webcontainer.core.CordovaWebviewChormeClient
import com.yzq.cordova_webcontainer.core.CordovaWebviewClient
import com.yzq.cordova_webcontainer.data.DocumentReadyState
import com.yzq.cordova_webcontainer.observer.PageObserver
import org.apache.cordova.*
import org.apache.cordova.customer.constant.PluginMessageId
import org.apache.cordova.customer.data.PlugnExecResult
import org.apache.cordova.customer.data.PlugnExecute
import org.apache.cordova.customer.listener.PageScrollChangedListener
import org.apache.cordova.engine.SystemWebView
import org.apache.cordova.engine.SystemWebViewEngine
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 * @description 自定义的webContainer, 摆脱了必须继承CordovaActivity的限制
 * 简单理解为就是一个去除了跟Activity有关联的方法的一个轻量的webview容器，适用于一些比较个性化的场景，可以像使用Webview控件一样的方式来使用
 */
class CordovaWebContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {


    companion object {
        const val TAG = "CordovaWebContainer"
    }

    private val isInitialized: AtomicBoolean = AtomicBoolean(false)


    private val documentJsInterface: CordovaJsInterface = DocumentJsInterface()
    lateinit var hostActivity: AppCompatActivity

    private var pageTitle: String = ""

    // The webview for our app
    private lateinit var appView: CordovaWebView

    // Keep app running when pause is received. (default = true)
    // If true, then the JavaScript and native code continue to run in the background
    // when another application (activity) is started.
    private var keepRunning = true

    // 从 config.xml 读取的配置
    private lateinit var preferences: CordovaPreferences
    private lateinit var launchUrl: String
    private lateinit var pluginEntries: ArrayList<PluginEntry>
    private lateinit var cordovaInterface: CordovaInterfaceImpl


    private val pageObserverList = mutableListOf<PageObserver>()
    val webview: SystemWebView
        get() = appView.view as SystemWebView


    private lateinit var _webViewEngine: SystemWebViewEngine
    val webViewEngine: SystemWebViewEngine
        get() = _webViewEngine

    private lateinit var _webviewClient: CordovaWebviewClient
    val webviewClient
        get() = _webviewClient


    private lateinit var _webChromeClient: CordovaWebviewChormeClient
    val webChromeClient
        get() = _webChromeClient

    fun init(appCompatActivity: AppCompatActivity, logLevel: Int = LOG.ERROR) {
        if (isInitialized.get()) {
            return
        }
        isInitialized.set(true)
        hostActivity = appCompatActivity

        // 读取config.xml配置
        loadConfig()
//        val logLevel = preferences.getString("loglevel", "ERROR")
        LOG.setLogLevel(logLevel)
        LOG.i(
            TAG,
            "init: Apache Cordova native platform version ${CordovaWebView.CORDOVA_VERSION} is starting"
        )
        cordovaInterface = makeCordovaInterface()
        /*初始化webview*/
        initWebView()
        LOG.i(TAG, "CordovaWebContainer init complete")
    }


    private fun initWebView() {
        appView = makeWebView()
        createViews()
        if (!appView.isInitialized) {
            appView.init(cordovaInterface, pluginEntries, preferences)
        }
        cordovaInterface.onCordovaInit(appView.pluginManager)

        // Wire the hardware volume controls to control media if desired.
        val volumePref = preferences.getString("DefaultVolumeStream", "")
        if ("media" == volumePref.lowercase()) {
            hostActivity.volumeControlStream = AudioManager.STREAM_MUSIC
        }

        _webviewClient = CordovaWebviewClient(webViewEngine)
        webview.webViewClient = _webviewClient

        _webChromeClient = CordovaWebviewChormeClient(webViewEngine)
        webview.webChromeClient = _webChromeClient

        addJavascriptInterface(documentJsInterface)

        /*处理宿主的生命周期*/
        handleHostLifecycle()


    }

    fun setWebviewClient(webviewClient: CordovaWebviewClient) {
        _webviewClient = webviewClient
        webview.webViewClient = _webviewClient
    }

    fun setWebviewChormeClient(webviewChormeClient: CordovaWebviewChormeClient) {
        _webChromeClient = webviewChormeClient
        webview.webChromeClient = _webChromeClient
    }

    fun addJavascriptInterface(jsInterface: CordovaJsInterface) {
        webview.addJavascriptInterface(jsInterface, jsInterface.getJsName())
    }

    /**
     * Handle host lifecycle
     * 宿主的生命周期处理
     */
    private fun handleHostLifecycle() {
        hostActivity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                pageObserverList.forEach {
                    it.onHostCreate(owner, hostActivity)
                }
            }

            override fun onStart(owner: LifecycleOwner) {
                appView.handleStart()
                pageObserverList.forEach {
                    it.onHostStart(owner, hostActivity)
                }
            }

            override fun onResume(owner: LifecycleOwner) {
                appView.handleResume(keepRunning)
                pageObserverList.forEach {
                    it.onHostResume(owner, hostActivity)
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                val keepRunningNew = keepRunning || cordovaInterface.activityResultCallback != null
                appView.handlePause(keepRunningNew)
                pageObserverList.forEach {
                    it.onHostPause(owner, hostActivity)
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                appView.handleStop()
                pageObserverList.forEach {
                    it.onHostStop(owner, hostActivity)
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                pageObserverList.forEach {
                    it.onHostDestory(owner, hostActivity)
                }
                destory()
            }
        })
    }


    private fun loadConfig() {
        val parser = ConfigXmlParser()
        parser.parse(hostActivity)
        preferences = parser.preferences
        preferences.setPreferencesBundle(hostActivity.intent.extras)
        launchUrl = parser.launchUrl
        pluginEntries = parser.pluginEntries
        //        Config.parser = parser;
        kotlin.runCatching {
            val parserField = Config::class.java.getDeclaredField("parser")
            parserField.isAccessible = true
            parserField[null] = parser
        }
    }

    @SuppressLint("ResourceType")
    private fun createViews() {
        /*源码这里设置了个id 不知道有啥用*/
        appView.view.id = 100
        appView.view.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        removeAllViews()
        this.addView(
            appView.view,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        if (preferences.contains("BackgroundColor")) {
            kotlin.runCatching {
                val backgroundColor = preferences.getInteger("BackgroundColor", Color.WHITE)
                appView.view.setBackgroundColor(backgroundColor)
            }
        }
        /*获取焦点*/
        appView.view.requestFocusFromTouch()
    }


    /**
     * 创建的webview对象
     *
     * @return CordovaWebView
     */
    private fun makeWebView(): CordovaWebView {
        return CordovaWebViewImpl(makeWebViewEngine())
    }

    private fun makeWebViewEngine(): CordovaWebViewEngine {
        _webViewEngine =
            CordovaWebViewImpl.createEngine(hostActivity, preferences) as SystemWebViewEngine
        return _webViewEngine
    }

    /**
     * 接收插件发送的消息
     *
     * @return
     */
    private fun makeCordovaInterface(): CordovaInterfaceImpl {
        return object : CordovaInterfaceImpl(hostActivity) {
            override fun onMessage(id: String, data: Any): Any {
                return handlePluginMessage(id, data)
            }
        }
    }

    /**
     * Load the url into the webview.
     */
    @Throws(RuntimeException::class)
    fun loadUrl(url: String = launchUrl) {
        keepRunning = preferences.getBoolean("KeepRunning", true)
        if (url.isEmpty()) {
            LOG.e(TAG, "url不能为空，请检查")
            return
        }
        launchUrl = url
        appView.loadUrlIntoView(launchUrl, true)

    }


    /**
     *  Handle message
     *  处理插件发出的消息
     * @param id
     * @param data
     * @return
     */
    private fun handlePluginMessage(id: String, data: Any): Any {
        LOG.i(TAG, "handlePluginMessage:id: ${id},  data:$data")
        when (id) {
            PluginMessageId.onPageStarted -> {
                handleReadyStateChange()
                pageObserverList.forEach {
                    it.onPageStarted(data as String)
                }
            }

            PluginMessageId.onPageFinished -> {
                getDocumentTitle()
                pageObserverList.forEach {
                    it.onPageFinished(data as String)
                }
            }

            PluginMessageId.onProgressChanged -> {
                pageObserverList.forEach {
                    it.onProgressChanged(data as Int)
                }
            }

            PluginMessageId.onReceivedTitle -> {
                pageTitle = data as String
                if (pageTitle != launchUrl) {
                    pageObserverList.forEach {
                        it.onReceivedTitle(data)
                    }
                }

            }

            PluginMessageId.onNavigationAttempt -> {
                pageObserverList.forEach {
                    it.onNavigationAttempt(data as String)
                }
            }

            PluginMessageId.onOverrideUrlLoading -> {
                pageObserverList.forEach {
                    it.onOverrideUrlLoading(data as String)
                }
            }

            PluginMessageId.shouldAllowNavigation -> {
                pageObserverList.forEach {
                    it.shouldAllowNavigation(data as String)
                }
            }

            PluginMessageId.shouldOpenExternalUrl -> {
                pageObserverList.forEach {
                    it.shouldOpenExternalUrl(data as String)
                }
            }

            PluginMessageId.pluginExecute -> {
                val plugnExecute = kotlin.runCatching {
                    (data as PlugnExecute).apply { url = launchUrl }
                }.getOrDefault(PlugnExecute())

                pageObserverList.forEach {
                    it.pluginExecute(plugnExecute)
                }
            }

            PluginMessageId.pluginResult -> {
                val plugnExecResult = kotlin.runCatching {
                    (data as PlugnExecResult).apply { url = launchUrl }
                }.getOrDefault(PlugnExecResult())
                pageObserverList.forEach {
                    it.pluginExecResult(plugnExecResult)
                }
            }

            PluginMessageId.readyStateChange -> {
                pageObserverList.forEach {
                    when (data as String) {
                        DocumentReadyState.loading.event -> {
                            it.readyStateChange(DocumentReadyState.loading, launchUrl)
                        }

                        DocumentReadyState.interactive.event -> {
                            it.readyStateChange(DocumentReadyState.interactive, launchUrl)
                        }

                        DocumentReadyState.complete.event -> {
                            it.readyStateChange(DocumentReadyState.complete, launchUrl)
                        }
                    }

                }
            }

            PluginMessageId.onReceivedError -> {
                onReceivedError(data)
            }

            PluginMessageId.windowOnError -> {
                onWindowError(data)
            }

            else -> {
            }
        }
        return "handlePluginMessage"
    }

    private fun onWindowError(data: Any) {
        kotlin.runCatching {
            /*{"msg":"Uncaught Error: test error","url":"https://localhost/js/index.js","lineNo":45,"columnNo":5} */
            val jsonObject = JSONObject(data as String)
            val msg = jsonObject.getString("msg")
            val url = jsonObject.getString("url")
            val lineNo = jsonObject.getInt("lineNo")
            val columnNo = jsonObject.getInt("columnNo")
            pageObserverList.forEach {
                it.onWindowError(url, msg, lineNo, columnNo)
            }
        }.onFailure {
            it.printStackTrace()
        }

    }

    private fun getDocumentTitle() {
        webview.evaluateJavascript(
            "document.title".trimIndent()
        ) {
            LOG.i(TAG, "getDocumentTitle:$it ")
            if (it.isNotEmpty()) {
                this.pageTitle = it
            }

        }
    }

    private fun handleReadyStateChange() {
        webview.evaluateJavascript(
            """
                document.addEventListener('readystatechange', function () {
                    window.${documentJsInterface.getJsName()}.readyStateChange(document.readyState)
                });
            """.trimIndent(), null
        )

        webview.evaluateJavascript(
            """
                window.onerror = function (msg, url, lineNo, columnNo, error) {
                    const data = {msg, url, lineNo, columnNo};
                    var stringify = JSON.stringify(data);
                    window.${documentJsInterface.getJsName()}.windowOnError(stringify);
                };
        """.trimIndent(), null
        )
    }


    inner class DocumentJsInterface : CordovaJsInterface("DocumentJsInterface") {
        @JavascriptInterface
        fun readyStateChange(event: String = "") {
            appView.pluginManager.postMessage(PluginMessageId.readyStateChange, event)
        }

        @JavascriptInterface
        fun windowOnError(data: String) {
            appView.pluginManager.postMessage(PluginMessageId.windowOnError, data)
        }

    }

    private fun onReceivedError(data: Any) {
        kotlin.runCatching {
            val jsonObject = data as JSONObject
            val errorCode = jsonObject.getInt("errorCode")
            val description = jsonObject.getString("description")
            val url = jsonObject.getString("url")
            pageObserverList.forEach {
                it.onPageError(errorCode, description, url)
            }
        }
    }


    fun onSaveInstanceState(outState: Bundle?) {
        cordovaInterface.onSaveInstanceState(outState)
    }

    fun startActivityForResult(requestCode: Int) {
        cordovaInterface.setActivityResultRequestCode(requestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        cordovaInterface.onActivityResult(requestCode, resultCode, intent)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        kotlin.runCatching {
            cordovaInterface.onRequestPermissionResult(requestCode, permissions, grantResults)
        }.onFailure {
            LOG.i(TAG, "JSONException: Parameters fed into the method are not valid")
            it.printStackTrace()
        }
    }

    /**
     * Called by the system when the device configuration changes while your activity is running.
     * onConfigurationChanged 被调用 例如语言、屏幕方向等发生变化
     *
     * @param newConfig The new device configuration
     */
    public override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val pm = appView.pluginManager
        pm?.onConfigurationChanged(newConfig)
    }


    fun addPagePbserver(pageObserver: PageObserver) {
        this.pageObserverList.add(pageObserver)
    }


    private fun destory() {
        appView.handleDestroy()
        this.pageObserverList.clear()
    }

    fun canGoBack() = webview.canGoBack()
    fun goBack() {
        pageObserverList.forEach {
            it.goBack()
        }
        webview.goBack()
    }

    fun canGoForward() = webview.canGoForward()
    fun goForward() {
        pageObserverList.forEach {
            it.goForward()
        }
        webview.goForward()
    }

    fun clearCache(includeDiskFiles: Boolean) {
        pageObserverList.forEach {
            it.clearCache(includeDiskFiles)
        }
        webview.clearCache(includeDiskFiles)
    }

    fun reload() {
        pageObserverList.forEach {
            it.reload()
        }
        webview.reload()
    }

    fun clearHistory() {
        pageObserverList.forEach {
            it.clearHistory()
        }
        webview.clearHistory()
    }


    fun setOnPageScrollChangedListener(listener: PageScrollChangedListener) {
        webview.setOnPageScrollChangedListener(listener)
    }

}