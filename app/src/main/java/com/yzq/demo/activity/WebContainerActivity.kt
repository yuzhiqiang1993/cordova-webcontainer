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
 * @description 继承自WebcontainerActivity的使用示例
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 * @date    2023/3/20
 * @time    13:41
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
                init(
                    this@WebContainerActivity,
                    this@WebContainerActivity
                )

                /*可选拦截请求*/
                webviewClient.interceptRequest { view, request, response ->
                    val url = request.url.toString()
                    Log.i(TAG, "interceptRequest:$url")
                    return@interceptRequest response
                }


                setWebviewClient(object : CordovaWebviewClient(webViewEngine) {
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        handler?.proceed()
                    }
                })


                /*可选 处理准备load的url*/
                webviewClient.overrideUrlLoading { view, request ->
                    Log.i(TAG, "overrideUrlLoading:${request.url}")
                    request.url.toString().let {
                        if (it.startsWith("kfcknight://")) {
                            return@overrideUrlLoading true
                        }
                    }
                    return@overrideUrlLoading false
                }

                val testJs = object : CordovaJsInterface("test") {
                    @JavascriptInterface
                    fun methordA() {
                    }
                }
                addJavascriptInterface(testJs)
                webContainer.webview.evaluateJavascript(testJs.getJsName(), null)


                /*可选自定义webSettings*/
                webview.settings.javaScriptEnabled = true
                webview.settings.setSupportZoom(false)
                /**
                 * 需要对页面进行监听的话可以加PageObserver
                 */
                addPagePbserver(object : PageObserver {
                    override fun onPageStarted(url: String) {
                        toolbar.title = "加载中..."
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
                        Log.i(TAG, "pluginExecStart: $plugnExecute")
                    }

                    override fun pluginExecResult(plugnExecResult: PlugnExecResult) {
                        Log.i(TAG, "pluginExecFinish: $plugnExecResult")
                    }

                    override fun onWindowError(
                        url: String,
                        msg: String,
                        lineNo: Int,
                        columnNo: Int,
                    ) {
                        Log.i(
                            TAG,
                            "onWindowError: url:$url,msg:$msg,lineNo:$lineNo,columnNo:$columnNo"
                        )
                    }


                })
            }
        }
        return binding.webContainer
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = "https://baidu.com/"
        binding.webContainer.loadUrl(url)
        binding.webContainer.setOnPageScrollChangedListener { xOffset, yOffset, oldxOffset, oldyOffset ->
            Log.i(TAG, "yOffset:$yOffset,oldyOffset:$oldyOffset")
        }
        binding.reloadFab.setOnClickListener {
            binding.webContainer.reload()

        }
    }

}