package com.yzq.cordova_webcontainer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.yzq.cordova_webcontainer.config.CordovaWebContainerConfig
import com.yzq.cordova_webcontainer.core.CordovaJsInterface
import com.yzq.cordova_webcontainer.core.CordovaWebviewChromeClient
import com.yzq.cordova_webcontainer.core.CordovaWebviewClient
import com.yzq.cordova_webcontainer.core.injection.CordovaInject
import com.yzq.cordova_webcontainer.core.whitelist.CordovaWhitelistInterceptor
import com.yzq.cordova_webcontainer.data.DocumentReadyState
import com.yzq.cordova_webcontainer.observer.PageObserver
import com.yzq.cordova_webcontainer.observer.PageObserverDispatcher
import org.apache.cordova.Config
import org.apache.cordova.ConfigXmlParser
import org.apache.cordova.CordovaInterfaceImpl
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaPreferences
import org.apache.cordova.CordovaWebView
import org.apache.cordova.CordovaWebViewEngine
import org.apache.cordova.CordovaWebViewImpl
import org.apache.cordova.LOG
import org.apache.cordova.PluginEntry
import org.apache.cordova.customer.constant.PluginMessageId
import org.apache.cordova.customer.data.PlugnExecResult
import org.apache.cordova.customer.data.PlugnExecute
import org.apache.cordova.customer.listener.PageScrollChangedListener
import org.apache.cordova.engine.SystemWebView
import org.apache.cordova.engine.SystemWebViewEngine
import org.json.JSONObject

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

    private var isInitialized = false


    private val documentJsInterface: CordovaJsInterface = DocumentJsInterface()
    lateinit var hostActivity: AppCompatActivity
    var hostFragment: Fragment? = null
    private lateinit var hostLifecycleOwner: LifecycleOwner

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
    private lateinit var cordovaInterface: ContainerCordovaInterface


    private val pageObserverDispatcher = PageObserverDispatcher()
    val webview: SystemWebView
        get() = appView.view as SystemWebView


    private lateinit var _webViewEngine: SystemWebViewEngine
    val webViewEngine: SystemWebViewEngine
        get() = _webViewEngine

    private lateinit var _webviewClient: CordovaWebviewClient
    val webviewClient
        get() = _webviewClient


    private lateinit var _webChromeClient: CordovaWebviewChromeClient
    val webChromeClient
        get() = _webChromeClient

    private var cordovaInject: CordovaInject? = null
    private var deferredRestoreState: Bundle? = null
    private lateinit var resultCoordinator: ActivityResultCoordinator

    fun init(fragment: Fragment, logLevel: Int = LOG.ERROR) {
        init(fragment, null, logLevel)
    }

    fun init(fragment: Fragment, savedInstanceState: Bundle?, logLevel: Int = LOG.ERROR) {
        this.hostFragment = fragment
        this.hostLifecycleOwner = fragment.viewLifecycleOwner
        init(fragment.requireActivity() as AppCompatActivity, savedInstanceState, logLevel)
    }

    fun init(appCompatActivity: AppCompatActivity, logLevel: Int = LOG.ERROR) {
        init(appCompatActivity, null, logLevel)
    }

    fun init(
        appCompatActivity: AppCompatActivity,
        savedInstanceState: Bundle?,
        logLevel: Int = LOG.ERROR,
    ) {
        if (isInitialized) {
            return
        }
        isInitialized = true
        hostActivity = appCompatActivity
        if (!this::hostLifecycleOwner.isInitialized) {
            hostLifecycleOwner = appCompatActivity
        }

        val initialRestoreState = savedInstanceState ?: deferredRestoreState
        deferredRestoreState = null

        // 读取config.xml配置
        loadConfig()
//        val logLevel = preferences.getString("loglevel", "ERROR")
        LOG.setLogLevel(logLevel)
        LOG.i(
            TAG,
            "init: Apache Cordova native platform version ${CordovaWebView.CORDOVA_VERSION} is starting"
        )
        cordovaInterface = makeCordovaInterface()
        resultCoordinator = ActivityResultCoordinator(
            hostActivity = hostActivity,
            hostLifecycleOwner = hostLifecycleOwner,
            launcherKeyFactory = ::buildLauncherKey,
            delegate = cordovaInterface,
            logTag = TAG,
        )
        resultCoordinator.prepare(initialRestoreState)
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
        resultCoordinator.onWebViewInitialized(appView.pluginManager)

        // 初始化鉴权白名单拦截器
        if (CordovaWebContainerConfig.ENABLE_CORDOVA_API_WHITELIST) {
            CordovaWebContainerConfig.cordovaWhitelistConfig?.let {
                appView.pluginManager.setApiInterceptor(CordovaWhitelistInterceptor(it))
            }
        }

        // Wire the hardware volume controls to control media if desired.
        val volumePref = preferences.getString("DefaultVolumeStream", "")
        if ("media" == volumePref.lowercase()) {
            hostActivity.volumeControlStream = AudioManager.STREAM_MUSIC
        }

        // 初始化前端自动注入
        cordovaInject = CordovaInject(hostActivity, this)

        _webviewClient = CordovaWebviewClient(webViewEngine)
        _webviewClient.interceptRequest { _, request, _ ->
            val url = request.url.toString()
            cordovaInject?.interceptResource(url)
        }
        webview.webViewClient = _webviewClient

        _webChromeClient = CordovaWebviewChromeClient(webViewEngine)
        webview.webChromeClient = _webChromeClient

        addJavascriptInterface(documentJsInterface)

        /*处理宿主的生命周期*/
        handleHostLifecycle()


    }

    fun setWebviewClient(webviewClient: CordovaWebviewClient) {
        _webviewClient = webviewClient
        webview.webViewClient = _webviewClient
    }

    fun setWebviewChromeClient(webviewChromeClient: CordovaWebviewChromeClient) {
        _webChromeClient = webviewChromeClient
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
        hostLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                pageObserverDispatcher.onHostCreate(owner, hostActivity)
            }

            override fun onStart(owner: LifecycleOwner) {
                appView.handleStart()
                pageObserverDispatcher.onHostStart(owner, hostActivity)
            }

            override fun onResume(owner: LifecycleOwner) {
                appView.handleResume(keepRunning)
                pageObserverDispatcher.onHostResume(owner, hostActivity)
            }

            override fun onPause(owner: LifecycleOwner) {
                val keepRunningNew = keepRunning || cordovaInterface.hasActivityResultCallback()
                appView.handlePause(keepRunningNew)
                pageObserverDispatcher.onHostPause(owner, hostActivity)
            }

            override fun onStop(owner: LifecycleOwner) {
                appView.handleStop()
                pageObserverDispatcher.onHostStop(owner, hostActivity)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                pageObserverDispatcher.onHostDestroy(owner, hostActivity)
                destroyContainer()
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
    private fun makeCordovaInterface(): ContainerCordovaInterface {
        return ContainerCordovaInterface(hostActivity)
    }

    private fun buildLauncherKey(suffix: String): String {
        val containerKey = if (id != View.NO_ID) {
            runCatching { resources.getResourceEntryName(id) }.getOrDefault(id.toString())
        } else {
            System.identityHashCode(this).toString()
        }
        val hostKey = buildString {
            append(hostActivity::class.java.name)
            hostFragment?.let {
                append(':')
                append(it::class.java.name)
                if (!it.tag.isNullOrEmpty()) {
                    append(':')
                    append(it.tag)
                }
            }
        }
        return "CordovaWebContainer:$hostKey:$containerKey:$suffix"
    }

    private inner class ContainerCordovaInterface(activity: AppCompatActivity) : CordovaInterfaceImpl(activity),
        ActivityResultDelegate {
        private var pendingActivityResultRequestCode: Int? = null
        override fun onMessage(id: String, data: Any?): Any {
            return handlePluginMessage(id, data)
        }

        override fun requestPermissions(
            plugin: CordovaPlugin,
            requestCode: Int,
            permissions: Array<out String>
        ) {
            resultCoordinator.launchPermissionRequest(plugin, requestCode, permissions)
        }

        override fun registerPermissionRequest(
            plugin: CordovaPlugin,
            requestCode: Int,
            permissions: Array<out String>,
        ): PendingPermissionRequest {
            val mappedRequestCode = permissionResultCallbacks.registerCallback(plugin, requestCode)
            val pendingPermissions = Array(permissions.size) { index -> permissions[index] }
            return PendingPermissionRequest(
                mappedRequestCode = mappedRequestCode,
                permissions = pendingPermissions,
            )
        }

        override fun dispatchPermissionResult(
            mappedRequestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray,
        ) {
            onRequestPermissionResult(mappedRequestCode, permissions, grantResults)
        }

        override fun dispatchActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
            onActivityResult(requestCode, resultCode, intent)
        }

        override fun startActivityForResult(
            command: CordovaPlugin,
            intent: Intent,
            requestCode: Int
        ) {
            resultCoordinator.launchActivityForResult(command, intent, requestCode)
        }

        override fun prepareActivityResult(command: CordovaPlugin, requestCode: Int) {
            setActivityResultCallback(command)
            rememberActivityResultRequestCode(requestCode)
        }

        override fun rememberActivityResultRequestCode(requestCode: Int) {
            pendingActivityResultRequestCode = requestCode
            activityResultRequestCode = requestCode
        }

        override fun consumeActivityResultRequestCode(): Int {
            val requestCode = pendingActivityResultRequestCode ?: activityResultRequestCode
            pendingActivityResultRequestCode = null
            activityResultRequestCode = 0
            return requestCode
        }

        fun clearActivityResultRequestCode(requestCode: Int) {
            if (pendingActivityResultRequestCode == requestCode) {
                pendingActivityResultRequestCode = null
            }
            if (activityResultRequestCode == requestCode) {
                activityResultRequestCode = 0
            }
        }

        override fun savedActivityResultRequestCode(): Int? {
            return pendingActivityResultRequestCode
        }

        override fun clearPendingActivityResult() {
            activityResultCallback = null
            clearActivityResultState()
        }

        fun clearActivityResultState() {
            pendingActivityResultRequestCode = null
            activityResultRequestCode = 0
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
    private fun handlePluginMessage(id: String, data: Any?): Any {
        when (id) {
            PluginMessageId.onPageStarted -> {
                handleReadyStateChange()
                (data as? String)?.let(pageObserverDispatcher::onPageStarted)
            }

            PluginMessageId.onPageFinished -> {
                getDocumentTitle()
                (data as? String)?.let(pageObserverDispatcher::onPageFinished)
            }

            PluginMessageId.onProgressChanged -> {
                (data as? Int)?.let(pageObserverDispatcher::onProgressChanged)
            }

            PluginMessageId.onReceivedTitle -> {
                val title = data as? String ?: return "handlePluginMessage"
                pageTitle = title
                if (pageTitle != launchUrl) {
                    pageObserverDispatcher.onReceivedTitle(title)
                }
            }

            PluginMessageId.onNavigationAttempt -> {
                (data as? String)?.let(pageObserverDispatcher::onNavigationAttempt)
            }

            PluginMessageId.onOverrideUrlLoading -> {
                (data as? String)?.let(pageObserverDispatcher::onOverrideUrlLoading)
            }

            PluginMessageId.shouldAllowNavigation -> {
                (data as? String)?.let(pageObserverDispatcher::shouldAllowNavigation)
            }

            PluginMessageId.shouldOpenExternalUrl -> {
                (data as? String)?.let(pageObserverDispatcher::shouldOpenExternalUrl)
            }

            PluginMessageId.pluginExecute -> {
                val pluginExecute = (data as? PlugnExecute)?.apply {
                    if (url.isBlank()) {
                        url = launchUrl
                    }
                } ?: PlugnExecute()
                pageObserverDispatcher.onPluginExecute(pluginExecute)
            }

            PluginMessageId.pluginResult -> {
                val pluginExecResult = (data as? PlugnExecResult)?.apply {
                    if (url.isBlank()) {
                        url = launchUrl
                    }
                } ?: PlugnExecResult()
                pageObserverDispatcher.onPluginExecResult(pluginExecResult)
            }

            PluginMessageId.readyStateChange -> {
                notifyReadyStateObservers(data)
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

    private fun notifyReadyStateObservers(data: Any?) {
        val readyState = when (data as? String) {
            DocumentReadyState.loading.event -> DocumentReadyState.loading
            DocumentReadyState.interactive.event -> DocumentReadyState.interactive
            DocumentReadyState.complete.event -> DocumentReadyState.complete
            else -> return
        }
        pageObserverDispatcher.onReadyStateChange(readyState, launchUrl)
    }

    private fun onWindowError(data: Any?) {
        kotlin.runCatching {
            /*{"msg":"Uncaught Error: test error","url":"https://localhost/js/index.js","lineNo":45,"columnNo":5} */
            val jsonObject = JSONObject(data as String)
            val msg = jsonObject.getString("msg")
            val url = jsonObject.getString("url")
            val lineNo = jsonObject.getInt("lineNo")
            val columnNo = jsonObject.getInt("columnNo")
            pageObserverDispatcher.onWindowError(url, msg, lineNo, columnNo)
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

    private fun onReceivedError(data: Any?) {
        kotlin.runCatching {
            val jsonObject = data as JSONObject
            val errorCode = jsonObject.getInt("errorCode")
            val description = jsonObject.getString("description")
            val url = jsonObject.getString("url")
            pageObserverDispatcher.onPageError(errorCode, description, url)
        }
    }


    fun onSaveInstanceState(outState: Bundle?) {
        if (!this::resultCoordinator.isInitialized) {
            return
        }
        resultCoordinator.onSaveInstanceState(outState)
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (!this::resultCoordinator.isInitialized) {
            deferredRestoreState = savedInstanceState
            return
        }
        val pluginManager = if (this::appView.isInitialized) appView.pluginManager else null
        resultCoordinator.restoreInstanceState(savedInstanceState, pluginManager)
    }

    fun startActivityForResult(requestCode: Int) {
        if (!this::cordovaInterface.isInitialized) {
            return
        }
        cordovaInterface.rememberActivityResultRequestCode(requestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (!this::cordovaInterface.isInitialized) {
            return
        }
        cordovaInterface.clearActivityResultRequestCode(requestCode)
        cordovaInterface.onActivityResult(requestCode, resultCode, intent)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (!this::cordovaInterface.isInitialized) {
            return
        }
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
        if (!this::appView.isInitialized) {
            return
        }
        val pm = appView.pluginManager
        pm?.onConfigurationChanged(newConfig)
    }


    fun addPageObserver(pageObserver: PageObserver) {
        pageObserverDispatcher.add(pageObserver)
    }


    private fun destroyContainer() {
        if (this::appView.isInitialized) {
            appView.handleDestroy()
        }
        pageObserverDispatcher.clear()

        cordovaInject?.destroy()
        cordovaInject = null
        if (this::resultCoordinator.isInitialized) {
            resultCoordinator.clear()
        }
        if (this::cordovaInterface.isInitialized) {
            cordovaInterface.clearActivityResultState()
        }
        deferredRestoreState = null
    }

    fun canGoBack() = webview.canGoBack()
    fun goBack() {
        pageObserverDispatcher.goBack()
        webview.goBack()
    }

    fun canGoForward() = webview.canGoForward()
    fun goForward() {
        pageObserverDispatcher.goForward()
        webview.goForward()
    }

    fun clearCache(includeDiskFiles: Boolean) {
        pageObserverDispatcher.clearCache(includeDiskFiles)
        webview.clearCache(includeDiskFiles)
    }

    fun reload() {
        pageObserverDispatcher.reload()
        webview.reload()
    }

    fun clearHistory() {
        pageObserverDispatcher.clearHistory()
        webview.clearHistory()
    }


    fun setOnPageScrollChangedListener(listener: PageScrollChangedListener) {
        webview.setOnPageScrollChangedListener(listener)
    }

}
