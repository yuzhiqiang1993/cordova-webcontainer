package com.yzq.demo.activity

import android.content.Context
import android.content.Intent
import android.net.http.SslError
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.widget.Toast
import com.yzq.cordova_webcontainer.CordovaWebContainer
import com.yzq.cordova_webcontainer.CordovaWebContainerActivity
import com.yzq.cordova_webcontainer.core.CordovaWebviewClient
import com.yzq.cordova_webcontainer.observer.PageObserver
import com.yzq.demo.databinding.ActivityGeolocationTestBinding
import org.apache.cordova.customer.data.PlugnExecResult
import org.apache.cordova.customer.data.PlugnExecute


/**
 * @description 展示对 Cordova Geolocation 插件全生命周期的控制和交互测试
 * 对应加载本地静态 `geolocation_test.html` 的测试网页。
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

class GeolocationTestActivity : CordovaWebContainerActivity() {
    private lateinit var binding: ActivityGeolocationTestBinding
    private val TAG = "GeolocationTestActivity"

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GeolocationTestActivity::class.java))
        }
    }


    /*布局初始化*/
    override fun initContentView() {
        binding = ActivityGeolocationTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }


    /*初始化 WebContainer 控制器*/
    override fun initWebContainer(): CordovaWebContainer {
        with(binding) {

            webContainer.run {
                /**
                 * 注入宿主
                 */
                init(this@GeolocationTestActivity)

                setWebviewClient(object : CordovaWebviewClient(webViewEngine) {
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        handler?.proceed()
                    }
                })


                /*配置定位需要的基础信息支持*/
                webview.settings.javaScriptEnabled = true
                webview.settings.setSupportZoom(false)

                // 注入页面及结果监听
                addPagePbserver(object : PageObserver {
                    override fun onPageStarted(url: String) {
                        Log.i(TAG, "定位系统测试 onPageStarted: $url")
                    }

                    override fun onProgressChanged(newProgress: Int) {
                        // 隐藏进度条或展现交互
                    }

                    override fun onReceivedTitle(title: String) {
                        Log.i(TAG, "onReceivedTitle: $title")
                    }

                    override fun onPageFinished(url: String) {
                        Log.i(TAG, "定位演示就绪 onPageFinished: $url")
                    }

                    override fun pluginExecute(plugnExecute: PlugnExecute) {
                        Log.i(TAG, "底座侦测 Cordova Plugin 唤起调用 -> $plugnExecute")
                    }

                    override fun pluginExecResult(plugnExecResult: PlugnExecResult) {
                        Log.i(TAG, "底座侦测 Cordova Plugin 返回结果 -> $plugnExecResult")
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

        // 挂载定位测试专用H5原型页
        binding.webContainer.loadUrl("https://localhost/geolocation_test.html")

        // 绑定权限清除重置交互事件
        binding.btnClearPermission.setOnClickListener {
            GeolocationPermissions.getInstance().clearAll()
            Toast.makeText(this, "网页定位权限记录已全部清除", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearNativePermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
            Toast.makeText(this, "请在权限管理中关闭位置信息权限", Toast.LENGTH_SHORT).show()
        }
    }
}
