package com.yzq.demo.activity


import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.apache.cordova.CordovaActivity

/**
 * @description 默认继承 CordovaActivity 的使用方式
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 * @date    2023/3/15
 * @time    15:54
 */
class CordovaDemoActivity : CordovaActivity() {
    companion object {

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, CordovaDemoActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        loadUrl("https://www.baidu.com/")
        loadUrl(launchUrl)
    }
}