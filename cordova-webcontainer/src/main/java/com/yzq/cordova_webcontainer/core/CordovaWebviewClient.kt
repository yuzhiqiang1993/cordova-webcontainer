package com.yzq.cordova_webcontainer.core

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import org.apache.cordova.engine.SystemWebViewClient
import org.apache.cordova.engine.SystemWebViewEngine


/**
 * @description 自定义的WebviewClient
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

open class CordovaWebviewClient(
    parentEngine: SystemWebViewEngine,
) : SystemWebViewClient(parentEngine) {

    private val TAG = "CordovaWebviewClient"
    private var yumInterceptRequest: CordovaInterceptRequest = null
    private var yumOverrideUrlLoading: CordovaOverrideUrlLoading = null


    /**
     * Intercept request
     * shouldInterceptRequest 拦截，如果自己setWebviewClient，该拦截无效
     * @param block
     */
    fun interceptRequest(block: CordovaInterceptRequest) {
        this.yumInterceptRequest = block
    }

    fun overrideUrlLoading(block: CordovaOverrideUrlLoading) {
        this.yumOverrideUrlLoading = block
    }


    /**
     * Should intercept request
     * 拦截webview中发出的一些请求
     * @param view
     * @param request
     * @return
     */
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {

        /**
         * 这里要执行一下父类的shouldInterceptRequest方法,
         * 内部调用了插件的getPathHandler方法，不写的话所有插件中的getPathHandler不会被调用
         */
        val resourceResponse = super.shouldInterceptRequest(view, request)
        if (yumInterceptRequest != null) {
            return yumInterceptRequest!!.invoke(view, request, resourceResponse)
        }

        return resourceResponse
    }


    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        if (yumOverrideUrlLoading != null) {
            return yumOverrideUrlLoading!!.invoke(view, request)
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

}