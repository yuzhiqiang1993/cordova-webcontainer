package com.yzq.demo.activity

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.yzq.demo.databinding.ActivityMainBinding


/**
 * @description 直接使用Webcontainer控件的示例，适用于更加灵活的场景,例如你不想继承指定的Activity
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.run {
            toolbar.title = "基于Cordova的webview使用"
            btnCordovaActivitySample.setOnClickListener {
                CordovaDemoActivity.startActivity(this@MainActivity)
            }
            btnContainerActivitySample.setOnClickListener {
                WebContainerActivity.startActivity(this@MainActivity)
            }
            btnFragmentSample.setOnClickListener {
                ViewPagerWebActivity.startActivity(this@MainActivity)
            }
            /*初始化*/
            webContainer.init(this@MainActivity, this@MainActivity)
            /*加载url*/
//            val url = "https://www.baidu.com/"
            webContainer.loadUrl()

        }

    }


    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        binding.webContainer.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.webContainer.onActivityResult(requestCode, resultCode, data)

    }

    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        binding.webContainer.startActivityForResult(requestCode)
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        binding.webContainer.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}