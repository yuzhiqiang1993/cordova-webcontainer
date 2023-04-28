package com.yzq.cordova_webcontainer.core

import android.view.KeyEvent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

typealias CordovaInterceptRequest = ((view: WebView, request: WebResourceRequest, response: WebResourceResponse?) -> WebResourceResponse?)?

typealias CordovaOverrideUrlLoading = ((view: WebView, request: WebResourceRequest) -> Boolean)?

typealias CordovaOverrideKeyEvent = ((view: WebView, event: KeyEvent) -> Boolean)?