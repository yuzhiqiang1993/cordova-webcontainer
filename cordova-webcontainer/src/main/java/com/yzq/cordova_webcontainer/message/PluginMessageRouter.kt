package com.yzq.cordova_webcontainer.message

import com.yzq.cordova_webcontainer.observer.PageObserverDispatcher
import org.apache.cordova.customer.constant.PluginMessageId
import org.apache.cordova.customer.data.PlugnExecResult
import org.apache.cordova.customer.data.PlugnExecute

/**
 * @description 插件消息路由器
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */
internal interface PluginMessageHost {
    val launchUrl: String
    var pageTitle: String
    val pageObserverDispatcher: PageObserverDispatcher

    fun onPageStarted()
    fun onPageFinished()
    fun onReadyStateChange(data: Any?)
    fun onReceivedError(data: Any?)
    fun onWindowError(data: Any?)
}

internal class PluginMessageRouter(
    private val host: PluginMessageHost,
) {
    private val handlers = mapOf<String, (Any?) -> Unit>(
        PluginMessageId.onPageStarted to { data ->
            host.onPageStarted()
            (data as? String)?.let(host.pageObserverDispatcher::onPageStarted)
        },
        PluginMessageId.onPageFinished to { data ->
            host.onPageFinished()
            (data as? String)?.let(host.pageObserverDispatcher::onPageFinished)
        },
        PluginMessageId.onProgressChanged to { data ->
            (data as? Int)?.let(host.pageObserverDispatcher::onProgressChanged)
        },
        PluginMessageId.onReceivedTitle to { data ->
            (data as? String)?.let { title ->
                host.pageTitle = title
                if (title != host.launchUrl) {
                    host.pageObserverDispatcher.onReceivedTitle(title)
                }
            }
        },
        PluginMessageId.onNavigationAttempt to { data ->
            (data as? String)?.let(host.pageObserverDispatcher::onNavigationAttempt)
        },
        PluginMessageId.onOverrideUrlLoading to { data ->
            (data as? String)?.let(host.pageObserverDispatcher::onOverrideUrlLoading)
        },
        PluginMessageId.shouldAllowNavigation to { data ->
            (data as? String)?.let(host.pageObserverDispatcher::shouldAllowNavigation)
        },
        PluginMessageId.shouldOpenExternalUrl to { data ->
            (data as? String)?.let(host.pageObserverDispatcher::shouldOpenExternalUrl)
        },
        PluginMessageId.pluginExecute to { data ->
            val pluginExecute = (data as? PlugnExecute)?.apply {
                if (url.isBlank()) {
                    url = host.launchUrl
                }
            } ?: PlugnExecute()
            host.pageObserverDispatcher.onPluginExecute(pluginExecute)
        },
        PluginMessageId.pluginResult to { data ->
            val pluginExecResult = (data as? PlugnExecResult)?.apply {
                if (url.isBlank()) {
                    url = host.launchUrl
                }
            } ?: PlugnExecResult()
            host.pageObserverDispatcher.onPluginExecResult(pluginExecResult)
        },
        PluginMessageId.readyStateChange to { data ->
            host.onReadyStateChange(data)
        },
        PluginMessageId.onReceivedError to { data ->
            host.onReceivedError(data)
        },
        PluginMessageId.windowOnError to { data ->
            host.onWindowError(data)
        },
    )

    fun dispatch(id: String, data: Any?): Any {
        handlers[id]?.invoke(data)
        return "handlePluginMessage"
    }
}
