package com.yzq.cordova_webcontainer.core

import org.apache.cordova.engine.SystemWebChromeClient
import org.apache.cordova.engine.SystemWebViewEngine


/**
 * @description 自定义的webviewChormeClient
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

open class CordovaWebviewChormeClient(parentEngine: SystemWebViewEngine) :
    SystemWebChromeClient(parentEngine)