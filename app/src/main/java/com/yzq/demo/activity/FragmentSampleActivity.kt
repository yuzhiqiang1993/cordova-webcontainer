package com.yzq.demo.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yzq.cordova_webcontainer.CordovaWebContainerFragment
import com.yzq.demo.R
import com.yzq.demo.databinding.ActivityFragmentSampleBinding
import com.yzq.demo.fragment.VantUploaderFragment

/**
 * @description Fragment 宿主示例，负责承载 CordovaWebContainerFragment 并转发回调。
 */
class FragmentSampleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFragmentSampleBinding

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, FragmentSampleActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFragmentSampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Fragment + Vant Uploader"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, VantUploaderFragment.newInstance())
                .commit()
        }
    }

    fun updateToolbarTitle(title: String) {
        supportActionBar?.title = title
    }
}
