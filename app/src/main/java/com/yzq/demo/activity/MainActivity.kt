package com.yzq.demo.activity

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.yzq.cordova_webcontainer.observer.PageObserver
import com.yzq.demo.databinding.ActivityMainBinding
import org.apache.cordova.LOG


/**
 * @description 核心优势演示：直接将 Cordova 作为轻量级 View 嵌入原生页面。
 * 摆脱了官方强制继承 CordovaActivity 的束缚，可以将其放在任意 ViewGroup 内。
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.run {
            btnCordovaActivitySample.setOnClickListener {
                CordovaDemoActivity.startActivity(this@MainActivity)
            }
            btnContainerActivitySample.setOnClickListener {
                WebContainerActivity.startActivity(this@MainActivity)
            }
            btnGeolocationSample.setOnClickListener {
                GeolocationTestActivity.startActivity(this@MainActivity)
            }
            /* ---- 核心演示：初始化 CordovaWebContainer ---- */
            // 传入当前 Activity 以绑定生命周期
            webContainer.init(this@MainActivity, LOG.VERBOSE)
            
            // 添加页面观察者，按需监听关注的事件即可（PageObserver 接口默认空实现，无需堆砌模板代码）
            webContainer.addPageObserver(object : PageObserver {
                override fun onPageStarted(url: String) {
                    LOG.i(TAG, "纯 View 模式：页面开始加载 -> $url")
                }

                override fun onReceivedTitle(title: String) {
                    LOG.i(TAG, "纯 View 模式：收到页面标题 -> $title")
                }

                override fun pluginExecute(plugnExecute: org.apache.cordova.customer.data.PlugnExecute) {
                    LOG.i(TAG, "纯 View 模式：拦截到 JS 插件调用 -> ${plugnExecute.pluginName}.${plugnExecute.action}")
                }
            })

            /* ---- 加载目标连接 ---- */
            webContainer.loadUrl()
        }

    }


    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        binding.webContainer.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.webContainer.onActivityResult(requestCode, resultCode, data)

    }

    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        binding.webContainer.startActivityForResult(requestCode)
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        binding.webContainer.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}