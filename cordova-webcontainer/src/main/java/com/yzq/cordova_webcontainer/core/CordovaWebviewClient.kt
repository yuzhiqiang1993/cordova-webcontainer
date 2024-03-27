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
    private var interceptRequest: CordovaInterceptRequest = null
    private var overrideUrlLoading: CordovaOverrideUrlLoading = null


    /**
     * Intercept request
     * shouldInterceptRequest 拦截，如果自己setWebviewClient，该拦截无效
     * @param block
     */
    fun interceptRequest(block: CordovaInterceptRequest) {
        this.interceptRequest = block
    }

    fun overrideUrlLoading(block: CordovaOverrideUrlLoading) {
        this.overrideUrlLoading = block
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


        var resourceResponse: WebResourceResponse? = null
        if (interceptRequest != null) {
            resourceResponse = interceptRequest!!.invoke(view, request, resourceResponse)
        }

        if (resourceResponse != null) {
            return resourceResponse
        }

        return super.shouldInterceptRequest(view, request)
    }


    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        if (overrideUrlLoading != null) {
            return overrideUrlLoading!!.invoke(view, request)
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

}