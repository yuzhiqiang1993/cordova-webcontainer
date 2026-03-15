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
import androidx.lifecycle.LifecycleOwner
import com.yzq.cordova_webcontainer.config.ConfigBridge
import com.yzq.cordova_webcontainer.config.CordovaWebContainerConfig
import com.yzq.cordova_webcontainer.core.CordovaJsInterface
import com.yzq.cordova_webcontainer.core.CordovaWebviewChromeClient
import com.yzq.cordova_webcontainer.core.CordovaWebviewClient
import com.yzq.cordova_webcontainer.core.injection.CordovaInject
import com.yzq.cordova_webcontainer.core.whitelist.CordovaWhitelistInterceptor
import com.yzq.cordova_webcontainer.data.DocumentReadyState
import com.yzq.cordova_webcontainer.lifecycle.HostLifecycleBinder
import com.yzq.cordova_webcontainer.message.PluginMessageHost
import com.yzq.cordova_webcontainer.message.PluginMessageRouter
import com.yzq.cordova_webcontainer.observer.PageObserver
import com.yzq.cordova_webcontainer.observer.PageObserverDispatcher
import com.yzq.cordova_webcontainer.state.ContainerStateController
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

    private val stateController = ContainerStateController(TAG)
    private val documentJsInterface: CordovaJsInterface = DocumentJsInterface()
    private val pageObserverDispatcher = PageObserverDispatcher()

    private lateinit var hostActivity: AppCompatActivity
    private var hostFragment: Fragment? = null
    private lateinit var hostLifecycleOwner: LifecycleOwner
    private lateinit var appView: CordovaWebView
    private lateinit var preferences: CordovaPreferences
    private lateinit var launchUrl: String
    private lateinit var pluginEntries: ArrayList<PluginEntry>
    private lateinit var cordovaInterface: ContainerCordovaInterface
    private lateinit var _webViewEngine: SystemWebViewEngine
    private lateinit var _webviewClient: CordovaWebviewClient
    private lateinit var _webChromeClient: CordovaWebviewChromeClient
    private lateinit var resultCoordinator: ActivityResultCoordinator

    private var pageTitle: String = ""
    private var keepRunning = true
    private var cordovaInject: CordovaInject? = null
    private var deferredRestoreState: Bundle? = null

    val activity: AppCompatActivity
        get() = hostActivity

    val fragment: Fragment?
        get() = hostFragment

    val webview: SystemWebView
        get() = appView.view as SystemWebView

    val webViewEngine: SystemWebViewEngine
        get() = _webViewEngine

    val webviewClient: CordovaWebviewClient
        get() = _webviewClient

    val webChromeClient: CordovaWebviewChromeClient
        get() = _webChromeClient

    private val messageHost = object : PluginMessageHost {
        override val launchUrl: String
            get() = this@CordovaWebContainer.launchUrl

        override var pageTitle: String
            get() = this@CordovaWebContainer.pageTitle
            set(value) {
                this@CordovaWebContainer.pageTitle = value
            }

        override val pageObserverDispatcher: PageObserverDispatcher
            get() = this@CordovaWebContainer.pageObserverDispatcher

        override fun onPageStarted() {
            injectDocumentHooks()
        }

        override fun onPageFinished() {
            loadDocumentTitle()
        }

        override fun onReadyStateChange(data: Any?) {
            this@CordovaWebContainer.notifyReadyStateObservers(data)
        }

        override fun onReceivedError(data: Any?) {
            this@CordovaWebContainer.onReceivedError(data)
        }

        override fun onWindowError(data: Any?) {
            this@CordovaWebContainer.onWindowError(data)
        }
    }
    private val messageRouter = PluginMessageRouter(messageHost)

    fun init(fragment: Fragment, logLevel: Int = LOG.ERROR) {
        init(fragment, null, logLevel)
    }

    fun init(fragment: Fragment, savedInstanceState: Bundle?, logLevel: Int = LOG.ERROR) {
        hostFragment = fragment
        hostLifecycleOwner = fragment.viewLifecycleOwner
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
        if (!stateController.beginInit()) {
            return
        }

        try {
            hostActivity = appCompatActivity
            if (!this::hostLifecycleOwner.isInitialized) {
                hostLifecycleOwner = appCompatActivity
            }

            val initialRestoreState = savedInstanceState ?: deferredRestoreState
            deferredRestoreState = null

            loadConfig()
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
            initWebView()

            stateController.markReady()
            LOG.i(TAG, "CordovaWebContainer init complete")
        } catch (t: Throwable) {
            handleInitFailure(t)
            throw t
        }
    }

    private fun handleInitFailure(error: Throwable) {
        runCatching {
            if (this::appView.isInitialized) {
                appView.handleDestroy()
            }
        }
        runCatching { cordovaInject?.destroy() }
        if (this::resultCoordinator.isInitialized) {
            resultCoordinator.clear()
        }
        if (this::cordovaInterface.isInitialized) {
            cordovaInterface.clearActivityResultState()
        }
        cordovaInject = null
        deferredRestoreState = null
        stateController.markDestroyed()
        LOG.e(TAG, "CordovaWebContainer init failed", error)
    }

    private fun initWebView() {
        appView = makeWebView()
        createViews()
        if (!appView.isInitialized) {
            appView.init(cordovaInterface, pluginEntries, preferences)
        }
        resultCoordinator.onWebViewInitialized(appView.pluginManager)

        if (CordovaWebContainerConfig.ENABLE_CORDOVA_API_WHITELIST) {
            CordovaWebContainerConfig.cordovaWhitelistConfig?.let {
                appView.pluginManager.setApiInterceptor(CordovaWhitelistInterceptor(it))
            }
        }

        val volumePref = preferences.getString("DefaultVolumeStream", "")
        if ("media" == volumePref.lowercase()) {
            hostActivity.volumeControlStream = AudioManager.STREAM_MUSIC
        }

        cordovaInject = CordovaInject(hostActivity, this)

        _webviewClient = CordovaWebviewClient(webViewEngine)
        _webviewClient.interceptRequest { _, request, _ ->
            val url = request.url.toString()
            cordovaInject?.interceptResource(url)
        }
        webview.webViewClient = _webviewClient

        _webChromeClient = CordovaWebviewChromeClient(webViewEngine)
        webview.webChromeClient = _webChromeClient
        webview.addJavascriptInterface(documentJsInterface, documentJsInterface.getJsName())

        HostLifecycleBinder(
            owner = hostLifecycleOwner,
            activity = hostActivity,
            pageObserverDispatcher = pageObserverDispatcher,
            onStart = { appView.handleStart() },
            onResume = { appView.handleResume(keepRunning) },
            onPause = {
                val keepRunningNew = keepRunning || cordovaInterface.hasActivityResultCallback()
                appView.handlePause(keepRunningNew)
            },
            onStop = { appView.handleStop() },
            onDestroy = ::destroyContainer,
        ).bind()
    }

    fun setWebviewClient(webviewClient: CordovaWebviewClient) {
        stateController.requireReady()
        _webviewClient = webviewClient
        webview.webViewClient = _webviewClient
    }

    fun setWebviewChromeClient(webviewChromeClient: CordovaWebviewChromeClient) {
        stateController.requireReady()
        _webChromeClient = webviewChromeClient
        webview.webChromeClient = _webChromeClient
    }

    fun addJavascriptInterface(jsInterface: CordovaJsInterface) {
        stateController.requireReady()
        webview.addJavascriptInterface(jsInterface, jsInterface.getJsName())
    }

    private fun loadConfig() {
        val parser = ConfigXmlParser()
        parser.parse(hostActivity)
        preferences = parser.preferences
        preferences.setPreferencesBundle(hostActivity.intent.extras)
        launchUrl = parser.launchUrl
        pluginEntries = parser.pluginEntries
        ConfigBridge.setupConfig(parser)
    }

    @SuppressLint("ResourceType")
    private fun createViews() {
        appView.view.id = View.generateViewId()
        appView.view.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        removeAllViews()
        addView(
            appView.view,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        if (preferences.contains("BackgroundColor")) {
            kotlin.runCatching {
                val backgroundColor = preferences.getInteger("BackgroundColor", Color.WHITE)
                appView.view.setBackgroundColor(backgroundColor)
            }
        }
        appView.view.requestFocusFromTouch()
    }

    private fun makeWebView(): CordovaWebView {
        return CordovaWebViewImpl(makeWebViewEngine())
    }

    private fun makeWebViewEngine(): CordovaWebViewEngine {
        _webViewEngine =
            CordovaWebViewImpl.createEngine(hostActivity, preferences) as SystemWebViewEngine
        return _webViewEngine
    }

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
            permissions: Array<out String>,
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
            requestCode: Int,
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

    @Throws(RuntimeException::class)
    fun loadUrl(url: String) {
        stateController.requireReady()
        keepRunning = preferences.getBoolean("KeepRunning", true)
        if (url.isEmpty()) {
            LOG.e(TAG, "url不能为空，请检查")
            return
        }
        launchUrl = url
        appView.loadUrlIntoView(launchUrl, true)
    }

    fun loadDefaultUrl() {
        stateController.requireReady()
        loadUrl(launchUrl)
    }

    private fun handlePluginMessage(id: String, data: Any?): Any {
        return messageRouter.dispatch(id, data)
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
            val jsonObject = JSONObject(data as String)
            val msg = jsonObject.getString("msg")
            val url = jsonObject.getString("url")
            val lineNo = jsonObject.getInt("lineNo")
            val columnNo = jsonObject.getInt("columnNo")
            pageObserverDispatcher.onWindowError(url, msg, lineNo, columnNo)
        }.onFailure {
            LOG.e(TAG, "onWindowError parse error", it)
        }
    }

    private fun loadDocumentTitle() {
        webview.evaluateJavascript("document.title") {
            LOG.i(TAG, "getDocumentTitle:$it ")
            if (it.isNotEmpty()) {
                pageTitle = it
            }
        }
    }

    private fun injectDocumentHooks() {
        webview.evaluateJavascript(
            """
                if (!window.__cordovaReadyStateHooked) {
                    window.__cordovaReadyStateHooked = true;
                    document.addEventListener('readystatechange', function () {
                        window.${documentJsInterface.getJsName()}.readyStateChange(document.readyState)
                    });
                }
            """.trimIndent(),
            null,
        )

        webview.evaluateJavascript(
            """
                if (!window.__cordovaErrorHooked) {
                    window.__cordovaErrorHooked = true;
                    window.onerror = function (msg, url, lineNo, columnNo, error) {
                        const data = {msg, url, lineNo, columnNo};
                        var stringify = JSON.stringify(data);
                        window.${documentJsInterface.getJsName()}.windowOnError(stringify);
                    };
                }
            """.trimIndent(),
            null,
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
        if (!stateController.isCordovaReady(this::cordovaInterface.isInitialized)) {
            return
        }
        resultCoordinator.onSaveInstanceState(outState)
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (!stateController.isCordovaReady(this::cordovaInterface.isInitialized)) {
            deferredRestoreState = savedInstanceState
            return
        }
        val pluginManager = if (this::appView.isInitialized) appView.pluginManager else null
        resultCoordinator.restoreInstanceState(savedInstanceState, pluginManager)
    }

    fun startActivityForResult(requestCode: Int) {
        if (!stateController.isCordovaReady(this::cordovaInterface.isInitialized)) {
            return
        }
        cordovaInterface.rememberActivityResultRequestCode(requestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (!stateController.isCordovaReady(this::cordovaInterface.isInitialized)) {
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
        if (!stateController.isCordovaReady(this::cordovaInterface.isInitialized)) {
            return
        }
        kotlin.runCatching {
            cordovaInterface.onRequestPermissionResult(requestCode, permissions, grantResults)
        }.onFailure {
            LOG.i(TAG, "JSONException: Parameters fed into the method are not valid")
            it.printStackTrace()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!stateController.isWebViewReady(this::appView.isInitialized)) {
            return
        }
        appView.pluginManager?.onConfigurationChanged(newConfig)
    }

    fun addPageObserver(pageObserver: PageObserver) {
        pageObserverDispatcher.add(pageObserver)
    }

    private fun destroyContainer() {
        if (stateController.isDestroyed()) {
            return
        }

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
        stateController.markDestroyed()
    }

    fun canGoBack(): Boolean {
        stateController.requireReady()
        return webview.canGoBack()
    }

    fun goBack() {
        stateController.requireReady()
        pageObserverDispatcher.goBack()
        webview.goBack()
    }

    fun canGoForward(): Boolean {
        stateController.requireReady()
        return webview.canGoForward()
    }

    fun goForward() {
        stateController.requireReady()
        pageObserverDispatcher.goForward()
        webview.goForward()
    }

    fun clearCache(includeDiskFiles: Boolean) {
        stateController.requireReady()
        pageObserverDispatcher.clearCache(includeDiskFiles)
        webview.clearCache(includeDiskFiles)
    }

    fun reload() {
        stateController.requireReady()
        pageObserverDispatcher.reload()
        webview.reload()
    }

    fun clearHistory() {
        stateController.requireReady()
        pageObserverDispatcher.clearHistory()
        webview.clearHistory()
    }

    fun setOnPageScrollChangedListener(listener: PageScrollChangedListener) {
        stateController.requireReady()
        webview.setOnPageScrollChangedListener(listener)
    }
}
