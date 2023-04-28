package com.yzq.cordova_webcontainer.observer

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.yzq.cordova_webcontainer.data.DocumentReadyState
import org.apache.cordova.customer.data.PlugnExecResult
import org.apache.cordova.customer.data.PlugnExecute


/**
 * @description 页面状态的回调
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

interface PageObserver {

    /**
     * On page started
     * 页面开始加载
     * @param url
     */
    fun onPageStarted(url: String) {}

    /**
     * On page finished
     * 页面加载结束
     * @param url
     */
    fun onPageFinished(url: String) {}

    /**
     * On page error
     * 页面加载错误回调，如网络异常，找不到页面等
     * @param errorCode
     * @param description
     * @param failingUrl
     */
    fun onPageError(errorCode: Int, description: String, failingUrl: String) {}


    /**
     * Ready state change
     *
     * @param readyState 文档加载的状态
     * @param url  对应加载的url
     */
    fun readyStateChange(readyState: DocumentReadyState, url: String) {}


    /*宿主的生命周期*/
    fun onHostCreate(owner: LifecycleOwner, activity: AppCompatActivity) {}
    fun onHostStart(owner: LifecycleOwner, activity: AppCompatActivity) {}
    fun onHostResume(owner: LifecycleOwner, activity: AppCompatActivity) {}
    fun onHostPause(owner: LifecycleOwner, activity: AppCompatActivity) {}
    fun onHostStop(owner: LifecycleOwner, activity: AppCompatActivity) {}
    fun onHostDestory(owner: LifecycleOwner, activity: AppCompatActivity) {}


    /**
     * On progress changed
     * 页面加载进度发生变化
     * @param newProgress
     */
    fun onProgressChanged(newProgress: Int) {

    }

    /**
     * On received title
     * 页面标题
     * @param title
     */
    fun onReceivedTitle(title: String) {

    }

    /**
     * Go back
     * 返回了
     */
    fun goBack() {

    }

    /**
     * Go forward
     * 前进了
     */
    fun goForward() {
    }

    /**
     * Clear cache
     * 清除缓存了
     * @param includeDiskFiles
     */
    fun clearCache(includeDiskFiles: Boolean) {
    }

    /**
     * Reload
     * 重新加载了
     */
    fun reload() {


    }

    fun clearHistory() {

    }

    /**
     * On navigation attempt
     * 拦截所有经过shouldOverrideUrlLoading的url
     * @param url
     */
    fun onNavigationAttempt(url: String) {

    }

    /**
     * On override url loading
     * 允许加载的链接
     * @param s
     */
    fun onOverrideUrlLoading(url: String) {


    }

    /**
     * Should allow navigation
     * 允许导航的url
     * @param url
     */
    fun shouldAllowNavigation(url: String) {


    }

    /**
     * Should open external url
     * url无法导航，需要被当做意图处理时回调
     * @param url
     * @param String
     */
    fun shouldOpenExternalUrl(url: String) {


    }

    /**
     * Plugin execute
     * 插件执行
     * @param plugnExecute
     */
    fun pluginExecute(plugnExecute: PlugnExecute) {
    }

    /**
     * Plugin exec result
     * 插件结果返回
     * @param plugnExecResult
     */
    fun pluginExecResult(plugnExecResult: PlugnExecResult) {}

    /**
     * On window error
     * js error
     * 示例：{"msg":"Uncaught Error: test error","url":"https://localhost/js/index.js","lineNo":45,"columnNo":5}
     * @param url
     * @param msg 错误信息
     * @param lineNo 行号
     * @param columnNo 列号
     */
    fun onWindowError(url: String, msg: String, lineNo: Int, columnNo: Int) {


    }


}