package com.yzq.cordova_webcontainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * @description 封装的Fragment
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

abstract class CordovaWebContainerFragment : Fragment() {

    private var webContainer: CordovaWebContainer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        initArgs(arguments)
        return initContentView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webContainer = initWebContainer()
        webContainer?.restoreInstanceState(savedInstanceState)

        /*初始化控件*/
        initWidget()
    }

    /**
     * 初始化参数
     *
     * @param extras  传递的参数对象
     */
    protected open fun initArgs(extras: Bundle?) {
    }

    /**
     * Init content view
     * 初始化内容视图，子类实现
     */
    abstract fun initContentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View


    /*初始化 webContainer 子类实现*/
    abstract fun initWebContainer(): CordovaWebContainer


    /*初始化控件 到这里内容视图以及Webcontainer都已经初始化好了 可以使用了*/
    protected open fun initWidget() {

    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webContainer?.onSaveInstanceState(outState)
    }

}
