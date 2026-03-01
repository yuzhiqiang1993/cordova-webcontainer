package com.yzq.cordova_webcontainer.core.whitelist

/**
 * @description: 插件白名单数据类
 * @author : yuzhiqiang
 */

data class WhitelistConfig(
    // 是否启用白名单（false 时所有 API 均可调用）
    val enable: Boolean = false,
    // 信任域名列表（逗号分隔，自动获得 */* 全部权限）
    val trustedDomains: String = "",
    // 全局放行的 API 列表，任何域名都可调用（如 "Geolocation/*"）
    val trustedApis: List<String> = emptyList(),
    val rules: List<Rule> = emptyList()
) {
    data class Rule(
        val domain: String,
        val comment: String = "",
        val allow: List<String> = emptyList()
    )
}
