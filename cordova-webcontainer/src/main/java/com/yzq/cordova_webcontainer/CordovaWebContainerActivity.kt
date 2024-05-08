package com.yzq.cordova_webcontainer

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
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

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        webContainer?.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        webContainer?.onActivityResult(requestCode, resultCode, data)

    }


    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        webContainer?.startActivityForResult(requestCode)
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        webContainer?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


}