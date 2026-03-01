package com.yzq.demo.activity

import android.content.Context
import android.content.Intent
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebView
import com.yzq.cordova_webcontainer.CordovaWebContainer
import com.yzq.cordova_webcontainer.CordovaWebContainerActivity
import com.yzq.cordova_webcontainer.core.CordovaJsInterface
import com.yzq.cordova_webcontainer.core.CordovaWebviewClient
import com.yzq.cordova_webcontainer.observer.PageObserver
import com.yzq.demo.databinding.ActivityWebContainerBinding
import org.apache.cordova.customer.data.PlugnExecResult
import org.apache.cordova.customer.data.PlugnExecute


/**
 * @description 继承自 CordovaWebContainerActivity 的快速使用示例
 * 核心优势演示：
 * 1. 内部已完美接管 Cordova 引擎的生命周期和 Permission/Result 调度机制
 * 2. 提供了极其暴露且灵活的资源拦截接口 (InterceptRequest) 与页面跳转拦截 (OverrideUrlLoading)
 * 3. 无需书写笨重的 Cordova Plugin 也可以像普通 WebView 一样混编注入底层 JS Bridge
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

class WebContainerActivity : CordovaWebContainerActivity() {
    private lateinit var binding: ActivityWebContainerBinding
    private val TAG = "WebContainerActivity"

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, WebContainerActivity::class.java))
        }
    }


    /*布局初始化*/
    override fun initContentView() {
        binding = ActivityWebContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }


    /*初始化Webcontainer控件*/
    override fun initWebContainer(): CordovaWebContainer {
        with(binding) {

            webContainer.run {
                /**
                 * 初始化
                 */
                init(this@WebContainerActivity)

                setWebviewClient(object : CordovaWebviewClient(webViewEngine) {
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        handler?.proceed()
                    }
                })

                /* ---- 优势 1：灵活接管 WebView 资源网络请求 ---- */
                // 支持对任意的请求做本地代理或离线缓存等处理
                webContainer.webviewClient.interceptRequest { view, request, response ->
                    val url = request.url.toString()
                    Log.i(TAG, "interceptRequest 离线及资源拦截点: $url")
                    return@interceptRequest response
                }


                /* ---- 优势 2：轻松控制 URL 路由跳转 ---- */
                webviewClient.overrideUrlLoading { view, request ->
                    Log.i(TAG, "overrideUrlLoading 拦截到页面内跳转动作: ${request.url}")
                    request.url.toString().let {
                        if (it.startsWith("baidu://")) {
                            Log.i(TAG, "已成功拦截目标 Scheme，阻止引擎默认跳转")
                            return@overrideUrlLoading true
                        }
                    }
                    return@overrideUrlLoading false
                }

                /* ---- 优势 3：无缝混编原生极简 JS Bridge ---- */
                // 抛开繁杂的 Cordova Plugin 配置，针对简单的业务依然支持直接绑定轻量对象
                val testJs = object : CordovaJsInterface("test") {
                    @JavascriptInterface
                    fun methordA() {
                        Log.i(TAG, "内置的原生 JavascriptInterface 被回调")
                    }
                }
                addJavascriptInterface(testJs)
                webContainer.webview.evaluateJavascript(testJs.getJsName(), null)


                /*可选自定义webSettings*/
                webview.settings.javaScriptEnabled = true
                webview.settings.setSupportZoom(false)
                /* ---- 优势 4：深度而细致地接管各状态分发 ---- */
                // 仅需重写需要监听的状态项，即可追踪到包括 JS Error 以及定制 Plugin 进出结果的底层信息
                addPageObserver(object : PageObserver {
                    override fun onPageStarted(url: String) {
                        Log.i(TAG, "onPageStarted: $url")
                    }

                    override fun onProgressChanged(newProgress: Int) {
                        binding.progressbar.progress = newProgress
                        if (newProgress >= 100) {
                            binding.progressbar.visibility = View.GONE
                        }
                    }

                    override fun onReceivedTitle(title: String) {
                        Log.i(TAG, "onReceivedTitle: $title")
                        toolbar.title = title
                    }

                    override fun onPageFinished(url: String) {
                        Log.i(TAG, "onPageFinished: $url")
                    }

                    override fun pluginExecute(plugnExecute: PlugnExecute) {
                        Log.i(TAG, "统一监听：拦截到了 Cordova Plugin 调用 -> $plugnExecute")
                    }

                    override fun pluginExecResult(plugnExecResult: PlugnExecResult) {
                        Log.i(TAG, "统一监听：拦截到了 Cordova Plugin 响应 -> $plugnExecResult")
                    }

                    override fun onWindowError(
                        url: String,
                        msg: String,
                        lineNo: Int,
                        columnNo: Int,
                    ) {
                        Log.e(
                            TAG,
                            "JS 报错捕获: url:$url, msg:$msg, lineNo:$lineNo, columnNo:$columnNo"
                        )
                    }
                })
            }
        }
        return binding.webContainer
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val url = "https://baidu.com/"
//        binding.webContainer.loadUrl(url)
        binding.webContainer.loadUrl()
        binding.webContainer.setOnPageScrollChangedListener { xOffset, yOffset, oldxOffset, oldyOffset ->
            Log.i(TAG, "yOffset:$yOffset,oldyOffset:$oldyOffset")
        }
        binding.reloadFab.setOnClickListener {
            binding.webContainer.reload()

        }
    }

}