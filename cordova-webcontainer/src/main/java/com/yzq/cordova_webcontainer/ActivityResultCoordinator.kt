package com.yzq.cordova_webcontainer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.LOG
import org.apache.cordova.PluginManager

/**
 * @description Activity Result 协调器
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

internal data class PendingPermissionRequest(
    val mappedRequestCode: Int,
    val permissions: Array<String>,
)

internal interface ActivityResultDelegate {
    fun onSaveInstanceState(outState: Bundle?)
    fun restoreInstanceState(savedInstanceState: Bundle)
    fun onCordovaInit(pluginManager: PluginManager)
    fun registerPermissionRequest(
        plugin: CordovaPlugin,
        requestCode: Int,
        permissions: Array<out String>,
    ): PendingPermissionRequest

    fun dispatchPermissionResult(
        mappedRequestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    )

    fun prepareActivityResult(command: CordovaPlugin, requestCode: Int)
    fun rememberActivityResultRequestCode(requestCode: Int)
    fun consumeActivityResultRequestCode(): Int
    fun savedActivityResultRequestCode(): Int?
    fun clearPendingActivityResult()
    fun dispatchActivityResult(requestCode: Int, resultCode: Int, intent: Intent?)
}

internal class ActivityResultCoordinator(
    private val hostActivity: AppCompatActivity,
    private val hostLifecycleOwner: LifecycleOwner,
    private val launcherKeyFactory: (String) -> String,
    private val delegate: ActivityResultDelegate,
    private val logTag: String,
) {
    companion object {
        private const val STATE_CONTAINER = "cordova_web_container_state"
        private const val STATE_ACTIVITY_RESULT_REQUEST_CODE = "activity_result_request_code"
    }

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var pendingPermissionRequest: PendingPermissionRequest? = null
    private val activityResultLauncherKey by lazy { launcherKeyFactory("activity_result") }
    private val permissionLauncherKey by lazy { launcherKeyFactory("permission_request") }

    fun prepare(savedInstanceState: Bundle?) {
        applySavedState(savedInstanceState)
        registerLaunchers()
    }

    fun onWebViewInitialized(pluginManager: PluginManager) {
        delegate.onCordovaInit(pluginManager)
    }

    fun onSaveInstanceState(outState: Bundle?) {
        outState?.let(::saveContainerState)
        delegate.onSaveInstanceState(outState)
    }

    fun restoreInstanceState(savedInstanceState: Bundle?, pluginManager: PluginManager?) {
        applySavedState(savedInstanceState)
        if (savedInstanceState != null && pluginManager != null) {
            delegate.onCordovaInit(pluginManager)
        }
    }

    fun launchPermissionRequest(
        plugin: CordovaPlugin,
        requestCode: Int,
        permissions: Array<out String>,
    ) {
        val pendingRequest = delegate.registerPermissionRequest(plugin, requestCode, permissions)
        pendingPermissionRequest = pendingRequest
        permissionLauncher.launch(pendingRequest.permissions)
    }

    fun launchActivityForResult(
        command: CordovaPlugin,
        intent: Intent,
        requestCode: Int,
    ) {
        try {
            delegate.prepareActivityResult(command, requestCode)
            activityResultLauncher.launch(intent)
        } catch (e: RuntimeException) {
            delegate.clearPendingActivityResult()
            throw e
        }
    }

    fun clear() {
        pendingPermissionRequest = null
        delegate.clearPendingActivityResult()
    }

    private fun registerLaunchers() {
        val activityResultRegistry = hostActivity.activityResultRegistry

        activityResultLauncher = activityResultRegistry.register(
            activityResultLauncherKey,
            hostLifecycleOwner,
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            delegate.dispatchActivityResult(
                delegate.consumeActivityResultRequestCode(), result.resultCode, result.data
            )
        }

        permissionLauncher = activityResultRegistry.register(
            permissionLauncherKey,
            hostLifecycleOwner,
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            dispatchPermissionResult(result)
        }
    }

    private fun applySavedState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            return
        }
        restoreContainerState(savedInstanceState)
        delegate.restoreInstanceState(savedInstanceState)
    }

    private fun restoreContainerState(savedInstanceState: Bundle) {
        val state = savedInstanceState.getBundle(STATE_CONTAINER) ?: return
        if (state.containsKey(STATE_ACTIVITY_RESULT_REQUEST_CODE)) {
            delegate.rememberActivityResultRequestCode(
                state.getInt(STATE_ACTIVITY_RESULT_REQUEST_CODE)
            )
        }
    }

    private fun saveContainerState(outState: Bundle) {
        val state = Bundle()
        delegate.savedActivityResultRequestCode()?.let {
            state.putInt(STATE_ACTIVITY_RESULT_REQUEST_CODE, it)
        }
        if (!state.isEmpty) {
            outState.putBundle(STATE_CONTAINER, state)
        }
    }

    private fun dispatchPermissionResult(result: Map<String, Boolean>) {
        val request = pendingPermissionRequest ?: return
        val grantResults = request.permissions.map { permission ->
            if (result[permission] == true) {
                PackageManager.PERMISSION_GRANTED
            } else {
                PackageManager.PERMISSION_DENIED
            }
        }.toIntArray()
        pendingPermissionRequest = null
        kotlin.runCatching {
            delegate.dispatchPermissionResult(
                request.mappedRequestCode,
                request.permissions,
                grantResults,
            )
        }.onFailure {
            LOG.i(logTag, "JSONException: Parameters fed into the method are not valid")
            it.printStackTrace()
        }
    }
}
