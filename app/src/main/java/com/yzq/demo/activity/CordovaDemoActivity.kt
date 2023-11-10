package com.yzq.demo.activity


import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.apache.cordova.CordovaActivity

/**
 * @description 官方默认使用方式，继承CordovaActivity
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */
class CordovaDemoActivity : CordovaActivity() {
    companion object {

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, CordovaDemoActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadUrl(launchUrl)
    }
}