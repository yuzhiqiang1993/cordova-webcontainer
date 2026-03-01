package com.yzq.cordova_webcontainer.core.injection

import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.net.toUri
import com.yzq.cordova_webcontainer.CordovaWebContainer
import com.yzq.cordova_webcontainer.config.CordovaWebContainerConfig
import com.yzq.cordova_webcontainer.observer.PageObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream


/**
 * @description: Cordova 注入
 * @author : yuzhiqiang
 */

class CordovaInject internal constructor(
    private val context: Context,
    private val webContainer: CordovaWebContainer
) {
    companion object {
        private const val TAG = "CordovaInject"
        private val CORDOVA_FILES = setOf("cordova.js", "cordova_plugins.js")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var initStartTime = 0L

    @Volatile
    private var cordovaJsBase64: String? = null

    @Volatile
    private var pluginsJsBase64: String? = null


    private val preloadDeferred = CompletableDeferred<Unit>()

    init {
        initStartTime = System.currentTimeMillis()
        if (CordovaWebContainerConfig.isLogEnable) {
            Log.d(TAG, "CordovaInject 初始化开始")
        }
        preloadJsResources()
        setupAutoInjection()
        if (CordovaWebContainerConfig.isLogEnable) {
            Log.d(
                TAG,
                "Cordova 注入已启用，资源目录: ${CordovaWebContainerConfig.CORDOVA_ASSET_DIR}, 初始化耗时: ${System.currentTimeMillis() - initStartTime}ms"
            )
        }
    }

    /**
     * 预加载 Cordova 核心的 js 资源到内存
     */
    private fun preloadJsResources() {
        scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                // 加载核心 JS 文件
                cordovaJsBase64 = loadAndEncodeAsset("cordova.js")
                pluginsJsBase64 = loadAndEncodeAsset("cordova_plugins.js")

                preloadDeferred.complete(Unit)
                if (CordovaWebContainerConfig.isLogEnable) {
                    Log.d(
                        TAG,
                        "核心 JS 资源预加载完成，耗时: ${System.currentTimeMillis() - startTime}ms"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "JS 资源预加载失败，耗时: ${System.currentTimeMillis() - startTime}ms", e)
                preloadDeferred.completeExceptionally(e)
            }
        }
    }

    /**
     * 加载 asset 文件内容
     */
    private fun loadAssetContent(fileName: String): String? {
        val assetPath = "${CordovaWebContainerConfig.CORDOVA_ASSET_DIR}/$fileName"
        return try {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            if (CordovaWebContainerConfig.isLogEnable) {
                Log.w(TAG, "加载 asset 失败: $assetPath", e)
            }
            null
        }
    }

    private fun loadAndEncodeAsset(fileName: String): String? {
        val startTime = System.currentTimeMillis()
        val assetPath = "${CordovaWebContainerConfig.CORDOVA_ASSET_DIR}/$fileName"
        return try {
            val content = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val encodeStart = System.currentTimeMillis()
            val result = Base64.encodeToString(
                content.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            if (CordovaWebContainerConfig.isLogEnable) {
                Log.d(
                    TAG,
                    "加载 $assetPath 成功: ${content.length} 字符, IO耗时: ${encodeStart - startTime}ms, Base64耗时: ${System.currentTimeMillis() - encodeStart}ms, 总耗时: ${System.currentTimeMillis() - startTime}ms"
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "加载 $assetPath 失败, 耗时: ${System.currentTimeMillis() - startTime}ms", e)
            null
        }
    }

    /**
     * 拦截 Cordova 资源请求
     * @param url 请求的资源 URL
     * @return 拦截成功返回 WebResourceResponse，否则返回 null
     */
    fun interceptResource(url: String): WebResourceResponse? {
        var response = interceptCordovaResource(url)
        if (response == null) {
            response = interceptLocalhostResource(url)
        }

        if (CordovaWebContainerConfig.isLogEnable && response != null) {
            Log.d(TAG, "拦截资源成功: $url")
        }
        return response
    }

    private fun interceptLocalhostResource(url: String): WebResourceResponse? {
        if (url.startsWith("http://localhost/")) {
            val assetPath = url.substringAfter("http://localhost/")

            // 忽略 favicon.ico，防止加载本地网页时可能出现 SystemWebViewClient 报错
            if (assetPath.endsWith("favicon.ico")) {
                return WebResourceResponse("image/x-icon", "UTF-8", null)
            }

            return try {
                val stream = context.assets.open(assetPath)
                val mimeType = getMimeType(assetPath)
                WebResourceResponse(mimeType, "UTF-8", stream)
            } catch (e: Exception) {
                if (CordovaWebContainerConfig.isLogEnable) {
                    Log.w(TAG, "Localhost 资源加载失败: $assetPath", e)
                }
                null
            }
        }
        return null
    }

    private fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        val mimeType = if (extension.isNullOrEmpty()) {
            null
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return mimeType ?: "application/octet-stream"
    }

    private fun interceptCordovaResource(url: String): WebResourceResponse? {
        // 拦截插件
        if (url.contains("/plugins/")) {
            val pluginPath = url.substringAfter("/plugins/").substringBefore("?")
            val fullPath = "plugins/$pluginPath"

            if (CordovaWebContainerConfig.isLogEnable) {
                Log.d(TAG, "interceptCordovaResource: 命中插件路径规则: $pluginPath, 完整路径: $fullPath")
            }
            // 尝试直接加载
            val startTime = System.currentTimeMillis()
            val content = loadAssetContent(fullPath)
            if (content != null) {
                if (CordovaWebContainerConfig.isLogEnable) {
                    Log.d(
                        TAG,
                        "interceptCordovaResource: 插件资源读取成功, 耗时: ${System.currentTimeMillis() - startTime}ms, 大小: ${content.length} 字符"
                    )
                }
                val mimeType = getMimeType(fullPath)
                return WebResourceResponse(
                    mimeType,
                    "UTF-8",
                    ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
                )
            } else {
                Log.e(
                    TAG,
                    "插件资源读取失败 (文件可能不存在): $fullPath"
                )
            }
            return null
        }

        // 拦截 cordova.js 和 cordova_plugins.js
        val fileName = url.substringAfterLast("/").substringBefore("?")
        if (fileName in CORDOVA_FILES) {
            if (CordovaWebContainerConfig.isLogEnable) {
                Log.d(TAG, "interceptCordovaResource: 命中核心文件规则: $fileName")
            }
            val content = loadAssetContent(fileName)
            if (content != null) {
                val mimeType = getMimeType(fileName)
                return WebResourceResponse(
                    mimeType,
                    "UTF-8",
                    ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
                )
            }
        }

        return null
    }


    /**
     * 自动注入
     */
    private fun setupAutoInjection() {
        if (CordovaWebContainerConfig.isLogEnable) {
            Log.d(TAG, "注册 PageObserver 监听器")
        }
        webContainer.addPagePbserver(object : PageObserver {
            override fun onPageStarted(url: String) {
                if (shouldInject(url)) {
                    injectWithPageStarted(url)
                }
            }

            override fun onPageFinished(url: String) {
                if (shouldInject(url)) {
                    injectWithPageFinished(url)
                }
            }
        })
    }

    private fun shouldInject(url: String): Boolean {
        // 排除无效或特殊的 scheme
        if (url.isEmpty() || url == "about:blank") return false
        val scheme = url.toUri().scheme?.lowercase()
        return scheme != "javascript" && scheme != "data"
    }

    private fun injectWithPageStarted(url: String) {
        scope.launch(Dispatchers.Main) {
            try {
                // 等待预加载完成
                if (!preloadDeferred.isCompleted) {
                    if (CordovaWebContainerConfig.isLogEnable) {
                        Log.d(TAG, "资源预加载未完成，挂起等待: $url")
                    }
                }
                preloadDeferred.await()

                val cordovaBase64 = cordovaJsBase64
                val pluginsBase64 = pluginsJsBase64

                if (cordovaBase64 == null) {
                    Log.e(TAG, "Cordova JS 未加载，跳过注入: $url")
                    return@launch
                }

                val webView = webContainer.webViewEngine.view as? WebView ?: run {
                    Log.e(TAG, "WebView 获取失败，无法注入: $url")
                    return@launch
                }

                val script = buildInjectScript(cordovaBase64, pluginsBase64)
                val startTime = System.currentTimeMillis()
                webView.evaluateJavascript(script) { result ->
                    val duration = System.currentTimeMillis() - startTime
                    if (result == null) {
                        Log.e(
                            TAG,
                            "早期注入失败，将在 onPageFinished 重试: $url, 耗时: ${duration}ms"
                        )
                    } else {
                        if (CordovaWebContainerConfig.isLogEnable) {
                            Log.d(TAG, "早期注入成功: $url, 耗时: ${duration}ms")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "等待资源加载失败，无法早期注入: $url", e)
            }
        }
    }

    private fun injectWithPageFinished(url: String) {
        val webView = webContainer.webViewEngine.view as? WebView ?: run {
            Log.e(TAG, "WebView 获取失败，无法注入: $url")
            return
        }

        webView.evaluateJavascript("(function(){ return window.__cordovaInjected; })()") { result ->
            val alreadyInjected = result == "true"
            if (alreadyInjected) {
                if (CordovaWebContainerConfig.isLogEnable) {
                    Log.d(TAG, "Cordova 已注入，跳过兜底注入: $url")
                }
                return@evaluateJavascript
            }

            if (CordovaWebContainerConfig.isLogEnable) {
                Log.w(TAG, "Cordova 未注入，执行兜底注入: $url")
            }

            val cordovaBase64 = cordovaJsBase64
            val pluginsBase64 = pluginsJsBase64

            if (cordovaBase64 == null) {
                Log.e(TAG, "Cordova JS 未加载，兜底注入失败: $url")
                return@evaluateJavascript
            }

            val script = buildInjectScript(cordovaBase64, pluginsBase64)
            val startTime = System.currentTimeMillis()
            webView.evaluateJavascript(script) { injectResult ->
                val duration = System.currentTimeMillis() - startTime
                if (injectResult == null) {
                    Log.e(TAG, "兜底注入失败: $url, 耗时: ${duration}ms")
                } else {
                    if (CordovaWebContainerConfig.isLogEnable) {
                        Log.d(TAG, "兜底注入成功: $url, 耗时: ${duration}ms")
                    }
                }
            }
        }
    }

    private fun buildInjectScript(cordovaBase64: String, pluginsBase64: String?): String {
        val pluginsJs = if (pluginsBase64 != null) {
            """eval(atob('$pluginsBase64'));"""
        } else {
            ""
        }

        return """
            |(function() {
            |    if (window.__cordovaInjected) return;
            |    window.__cordovaInjected = true;
            |    if (typeof cordova === 'undefined') {
            |        eval(atob('$cordovaBase64'));
            |        $pluginsJs
            |    }
            |})();
        """.trimMargin()
    }

    fun destroy() {
        scope.cancel()
        cordovaJsBase64 = null
        pluginsJsBase64 = null
        if (CordovaWebContainerConfig.isLogEnable) {
            Log.d(TAG, "CordovaInject 已销毁")
        }
    }
}
