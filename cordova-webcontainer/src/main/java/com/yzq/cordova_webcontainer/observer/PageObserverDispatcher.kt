package com.yzq.cordova_webcontainer.observer

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.yzq.cordova_webcontainer.data.DocumentReadyState
import org.apache.cordova.customer.data.PlugnExecResult
import org.apache.cordova.customer.data.PlugnExecute

/**
 * @description 页面状态分发器
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

internal class PageObserverDispatcher {
    private val observers = mutableListOf<PageObserver>()

    fun add(observer: PageObserver) {
        observers.add(observer)
    }

    fun clear() {
        observers.clear()
    }

    fun onHostCreate(owner: LifecycleOwner, activity: AppCompatActivity) {
        notifyObservers { it.onHostCreate(owner, activity) }
    }

    fun onHostStart(owner: LifecycleOwner, activity: AppCompatActivity) {
        notifyObservers { it.onHostStart(owner, activity) }
    }

    fun onHostResume(owner: LifecycleOwner, activity: AppCompatActivity) {
        notifyObservers { it.onHostResume(owner, activity) }
    }

    fun onHostPause(owner: LifecycleOwner, activity: AppCompatActivity) {
        notifyObservers { it.onHostPause(owner, activity) }
    }

    fun onHostStop(owner: LifecycleOwner, activity: AppCompatActivity) {
        notifyObservers { it.onHostStop(owner, activity) }
    }

    fun onHostDestroy(owner: LifecycleOwner, activity: AppCompatActivity) {
        notifyObservers { it.onHostDestroy(owner, activity) }
    }

    fun onPageStarted(url: String) {
        notifyObservers { it.onPageStarted(url) }
    }

    fun onPageFinished(url: String) {
        notifyObservers { it.onPageFinished(url) }
    }

    fun onProgressChanged(progress: Int) {
        notifyObservers { it.onProgressChanged(progress) }
    }

    fun onReceivedTitle(title: String) {
        notifyObservers { it.onReceivedTitle(title) }
    }

    fun onNavigationAttempt(url: String) {
        notifyObservers { it.onNavigationAttempt(url) }
    }

    fun onOverrideUrlLoading(url: String) {
        notifyObservers { it.onOverrideUrlLoading(url) }
    }

    fun shouldAllowNavigation(url: String) {
        notifyObservers { it.shouldAllowNavigation(url) }
    }

    fun shouldOpenExternalUrl(url: String) {
        notifyObservers { it.shouldOpenExternalUrl(url) }
    }

    fun onPluginExecute(result: PlugnExecute) {
        notifyObservers { it.pluginExecute(result) }
    }

    fun onPluginExecResult(result: PlugnExecResult) {
        notifyObservers { it.pluginExecResult(result) }
    }

    fun onReadyStateChange(readyState: DocumentReadyState, url: String) {
        notifyObservers { it.readyStateChange(readyState, url) }
    }

    fun onPageError(errorCode: Int, description: String, failingUrl: String) {
        notifyObservers { it.onPageError(errorCode, description, failingUrl) }
    }

    fun onWindowError(url: String, msg: String, lineNo: Int, columnNo: Int) {
        notifyObservers { it.onWindowError(url, msg, lineNo, columnNo) }
    }

    fun goBack() {
        notifyObservers { it.goBack() }
    }

    fun goForward() {
        notifyObservers { it.goForward() }
    }

    fun clearCache(includeDiskFiles: Boolean) {
        notifyObservers { it.clearCache(includeDiskFiles) }
    }

    fun reload() {
        notifyObservers { it.reload() }
    }

    fun clearHistory() {
        notifyObservers { it.clearHistory() }
    }

    private inline fun notifyObservers(action: (PageObserver) -> Unit) {
        observers.forEach(action)
    }
}
