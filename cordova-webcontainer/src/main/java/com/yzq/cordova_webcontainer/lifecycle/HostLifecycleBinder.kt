package com.yzq.cordova_webcontainer.lifecycle

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.yzq.cordova_webcontainer.observer.PageObserverDispatcher

/**
 * @description 宿主生命周期绑定器
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */
internal class HostLifecycleBinder(
    private val owner: LifecycleOwner,
    private val activity: AppCompatActivity,
    private val pageObserverDispatcher: PageObserverDispatcher,
    private val onStart: () -> Unit,
    private val onResume: () -> Unit,
    private val onPause: () -> Unit,
    private val onStop: () -> Unit,
    private val onDestroy: () -> Unit,
) {
    fun bind() {
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                pageObserverDispatcher.onHostCreate(owner, activity)
            }

            override fun onStart(owner: LifecycleOwner) {
                onStart()
                pageObserverDispatcher.onHostStart(owner, activity)
            }

            override fun onResume(owner: LifecycleOwner) {
                onResume()
                pageObserverDispatcher.onHostResume(owner, activity)
            }

            override fun onPause(owner: LifecycleOwner) {
                onPause()
                pageObserverDispatcher.onHostPause(owner, activity)
            }

            override fun onStop(owner: LifecycleOwner) {
                onStop()
                pageObserverDispatcher.onHostStop(owner, activity)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                pageObserverDispatcher.onHostDestroy(owner, activity)
                onDestroy()
            }
        })
    }
}
