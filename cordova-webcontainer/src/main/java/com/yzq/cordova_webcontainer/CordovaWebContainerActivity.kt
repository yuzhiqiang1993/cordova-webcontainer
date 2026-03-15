package com.yzq.cordova_webcontainer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


/**
 * @description 封装了cordovaView的Activity
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */
abstract class CordovaWebContainerActivity : AppCompatActivity() {

    private var webContainer: CordovaWebContainer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //初始化intent携带的参数
        initArgs(extras = intent.extras)
        //初始化布局
        initContentView()
        //初始化web容器
        webContainer = initWebContainer()
        webContainer?.restoreInstanceState(savedInstanceState)
        //初始化控件
        initWidget()

    }

    /**
     * 初始化参数
     *
     * @param extras  传递的参数对象
     */
    protected open fun initArgs(extras: Bundle?) {
    }

    /*初始化控件 到这里内容视图以及Webcontainer都已经初始化好了 可以使用了*/
    protected open fun initWidget() {

    }

    /**
     * Init content view
     * 初始化内容视图，子类实现，主要是setContentView
     */
    abstract fun initContentView()

    /*初始化 webContainer 子类实现*/
    abstract fun initWebContainer(): CordovaWebContainer

    override fun onSaveInstanceState(outState: Bundle) {
        webContainer?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

}