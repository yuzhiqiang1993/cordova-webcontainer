package com.yzq.cordova_webcontainer.config

import com.yzq.cordova_webcontainer.core.whitelist.WhitelistConfig

/**
 * @description: 配置类
 * @author : yuzhiqiang
 */

object CordovaWebContainerConfig {

    // 日志开关
    var isLogEnable = false

    var CORDOVA_ASSET_DIR = "www"

    // 是否开启 Cordova API 白名单
    var ENABLE_CORDOVA_API_WHITELIST = false

    // 白名单配置
    var cordovaWhitelistConfig: WhitelistConfig? = null
}
