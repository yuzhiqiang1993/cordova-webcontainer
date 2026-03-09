package com.yzq.demo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.yzq.cordova_webcontainer.CordovaWebContainer
import com.yzq.cordova_webcontainer.CordovaWebContainerFragment
import com.yzq.cordova_webcontainer.observer.PageObserver
import com.yzq.demo.activity.FragmentSampleActivity
import com.yzq.demo.databinding.FragmentVantUploaderBinding
import org.apache.cordova.LOG

/**
 * @description Fragment 场景下加载 Vant Uploader 示例页面。
 */
class VantUploaderFragment : CordovaWebContainerFragment() {
    private var _binding: FragmentVantUploaderBinding? = null
    private val binding: FragmentVantUploaderBinding
        get() = checkNotNull(_binding)

    companion object {
        private const val PAGE_URL = "https://vant-ui.github.io/vant/v2/mobile.html#/zh-CN/uploader"

        fun newInstance() = VantUploaderFragment()
    }

    override fun initContentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVantUploaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initWebContainer(): CordovaWebContainer {
        return binding.webContainer.apply {
            init(this@VantUploaderFragment, LOG.VERBOSE)
            addPageObserver(object : PageObserver {
                override fun onPageStarted(url: String) {
                    binding.progressbar.visibility = View.VISIBLE
                }

                override fun onProgressChanged(newProgress: Int) {
                    binding.progressbar.progress = newProgress
                    if (newProgress >= 100) {
                        binding.progressbar.visibility = View.GONE
                    }
                }

                override fun onReceivedTitle(title: String) {
                    (activity as? FragmentSampleActivity)?.updateToolbarTitle(title)
                }

                override fun onPageFinished(url: String) {
                    binding.progressbar.visibility = View.GONE
                }
            })
        }
    }

    override fun initWidget() {
        binding.webContainer.loadUrl(PAGE_URL)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
