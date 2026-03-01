package com.yzq.cordova_webcontainer.core

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import android.util.DisplayMetrics
import android.view.WindowManager
import android.webkit.GeolocationPermissions
import androidx.core.net.toUri
import com.yzq.cordova_webcontainer.R
import org.apache.cordova.engine.SystemWebChromeClient
import org.apache.cordova.engine.SystemWebViewEngine


/**
 * @description 自定义的webviewChormeClient
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

open class CordovaWebviewChormeClient(parentEngine: SystemWebViewEngine) :
    SystemWebChromeClient(parentEngine) {


    override fun onGeolocationPermissionsShowPrompt(
        origin: String, callback: GeolocationPermissions.Callback
    ) {

        // 弹窗询问用户，是否同意将原生已有的定位权限，授权给当前这个网页。
        showWebPermissionDialog(origin, callback)
    }


    private fun showWebPermissionDialog(
        origin: String, callback: GeolocationPermissions.Callback
    ) {
        val host = try {
            origin.toUri().host ?: origin
        } catch (e: Exception) {
            origin
        }

        val context = parentEngine.view.context ?: return

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_geolocation, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
        val btnAllow = dialogView.findViewById<TextView>(R.id.btn_dialog_allow)
        val btnDeny = dialogView.findViewById<TextView>(R.id.btn_dialog_deny)

        tvMessage.text = "当前网页【$host】需要获取您的位置信息以提供相关服务。您是否允许？"

        val dialog = AlertDialog.Builder(context).setView(dialogView).setCancelable(false).create()

        btnAllow.setOnClickListener {
            callback.invoke(origin, true, true)
            dialog.dismiss()
        }

        btnDeny.setOnClickListener {
            callback.invoke(origin, false, true)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        dialog.window?.let { window ->
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            val layoutParams = window.attributes
            layoutParams.width = (displayMetrics.widthPixels * 0.8).toInt()
            window.attributes = layoutParams
        }
    }
}