package com.yzq.demo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.yzq.cordova_webcontainer.CordovaWebContainer
import com.yzq.cordova_webcontainer.CordovaWebContainerFragment
import com.yzq.demo.R
import com.yzq.demo.activity.ViewPagerWebActivity


/**
 * @description 在Fragment中使用示例
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

class WebContainerFragment(val webUrl: String) : CordovaWebContainerFragment() {
    private lateinit var rootView: View
    private lateinit var webContainer: CordovaWebContainer

    override fun initContentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        rootView = layoutInflater.inflate(R.layout.fragment_web_container, container, false)
        return rootView
    }

    override fun initWebContainer(): CordovaWebContainer {
        webContainer = rootView.findViewById(R.id.web_container)
        webContainer.init(requireActivity() as AppCompatActivity)
        return webContainer
    }

    override fun initWidget() {
        if (webUrl.isNotEmpty()) {
            webContainer.loadUrl(url = webUrl)
        } else {
            webContainer.loadUrl()
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as ViewPagerWebActivity).currentFragment = this
    }


}