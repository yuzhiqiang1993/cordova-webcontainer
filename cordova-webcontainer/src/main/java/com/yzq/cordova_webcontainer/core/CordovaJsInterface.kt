package com.yzq.cordova_webcontainer.core

import android.webkit.JavascriptInterface


/**
 * @description JS接口
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

open class CordovaJsInterface(private val jsName: String) {
    @JavascriptInterface
    fun getJsName(): String {
        return jsName
    }
}