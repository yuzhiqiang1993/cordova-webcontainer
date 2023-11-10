package com.yzq.demo.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.yzq.demo.data.UrlItem
import com.yzq.demo.databinding.ActivityViewPagerWebBinding
import com.yzq.demo.fragment.WebContainerFragment


/**
 * @description 在fragment中使用 webviewContainer示例
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

class ViewPagerWebActivity : AppCompatActivity() {

    private val TAG = "ViewPagerWebActivity"
    private val tabTitles = arrayListOf(
        UrlItem("本地网页"),
        UrlItem("百度", "https://www.baidu.com/"),
        UrlItem("京东", "https://m.jd.com/"),
        UrlItem("本地网页"),
        UrlItem("京东", "https://m.jd.com/"),
        UrlItem("百度", "https://www.baidu.com/"),
    )


    var currentFragment: WebContainerFragment? = null

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ViewPagerWebActivity::class.java))
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityViewPagerWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return tabTitles.size
            }

            override fun createFragment(position: Int): Fragment {
                Log.i(TAG, "createFragment: $position")
                return WebContainerFragment(tabTitles[position].webUrl)
            }
        }


        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position].tabTitle
        }.attach()


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult: $currentFragment")
        currentFragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionsResult: $currentFragment")
        currentFragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}