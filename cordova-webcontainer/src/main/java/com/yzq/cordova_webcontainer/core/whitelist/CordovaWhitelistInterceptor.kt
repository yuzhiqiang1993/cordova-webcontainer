package com.yzq.cordova_webcontainer.core.whitelist

import android.util.Log
import androidx.core.net.toUri
import com.yzq.cordova_webcontainer.config.CordovaWebContainerConfig
import org.apache.cordova.customer.CordovaApiInterceptor

/**
 * @description: Cordova API 白名单拦截器
 * @author : yuzhiqiang
 */
internal class CordovaWhitelistInterceptor(config: WhitelistConfig) : CordovaApiInterceptor {

    companion object {
        private const val TAG = "CordovaWhitelist"
    }

    // 信任域名列表
    private val trustedDomains: List<String>

    // 全局放行 API 列表
    private val trustedApis: List<String> = config.trustedApis

    // 细粒度的域名规则（域名 → 规则映射）
    private val domainRules: Map<String, WhitelistConfig.Rule> =
        config.rules.associateBy { it.domain }

    init {
        val domains = mutableListOf<String>()
        if (config.trustedDomains.isNotEmpty()) {
            config.trustedDomains.split(",").forEach { raw ->
                val domain = raw.trim()
                if (domain.isNotEmpty()) {
                    domains.add(domain)
                }
            }
        }
        trustedDomains = domains
        if (CordovaWebContainerConfig.isLogEnable) {
            Log.i(
                TAG,
                "白名单初始化: 信任域名 ${domains.size} 个, 全局API ${trustedApis.size} 个, 规则 ${domainRules.size} 条"
            )
        }
    }

    override fun shouldAllowApi(url: String?, service: String?, action: String?): Boolean {
        if (url.isNullOrEmpty() || service.isNullOrEmpty() || action.isNullOrEmpty()) {
            if (CordovaWebContainerConfig.isLogEnable) {
                Log.w(TAG, "API 拦截: 参数为空")
            }
            return false
        }

        val host = try {
            url.toUri().host ?: return false
        } catch (e: Exception) {
            if (CordovaWebContainerConfig.isLogEnable) {
                Log.w(TAG, "API 拦截: URL 解析失败 $url")
            }
            return false
        }

        // 是信任的域名
        if (isTrustedDomain(host)) {
            if (CordovaWebContainerConfig.isLogEnable) {
                Log.d(TAG, "API 允许: $service.$action (信任域名 $host)")
            }
            return true
        }

        // 属于全局放行 API
        if (matchApiList(trustedApis, service, action)) {
            if (CordovaWebContainerConfig.isLogEnable) {
                Log.d(TAG, "API 允许: $service.$action (全局放行API)")
            }
            return true
        }

        // 细粒度规则: 按域名查找对应的 allow 列表
        val rule = domainRules[host]
        if (rule != null && matchApiList(rule.allow, service, action)) {
            if (CordovaWebContainerConfig.isLogEnable) {
                Log.d(TAG, "API 允许: $service.$action (规则匹配 $host)")
            }
            return true
        }

        // 默认拦截
        if (CordovaWebContainerConfig.isLogEnable) {
            Log.w(TAG, "API 拦截（不满足白名单规则）: $service.$action, host=$host")
        }
        return false
    }

    /**
     * 检查 host 是否在信任域名列表中
     */
    private fun isTrustedDomain(host: String): Boolean {
        for (i in trustedDomains.indices) {
            if (host.contains(trustedDomains[i])) {
                return true
            }
        }
        return false
    }

    /**
     * 检查 service/action 是否匹配 API 列表
     * - "Service/Action" 精确匹配
     * - "Service/\*" 插件级通配
     */
    private fun matchApiList(
        apiList: List<String>,
        service: String,
        action: String
    ): Boolean {
        for (i in apiList.indices) {
            val entry = apiList[i]

            val slashIndex = entry.indexOf('/')
            if (slashIndex <= 0) {
                // 只配置插件名，表示允许该插件所有方法
                if (entry == service) {
                    return true
                }
                continue
            }

            val entryService = entry.substring(0, slashIndex)
            val entryAction = entry.substring(slashIndex + 1)

            // 插件级通配: "Service/*"
            if (entryAction == "*") {
                if (entryService == service) {
                    return true
                }
                continue
            }

            // 精确匹配: "Service/Action"
            if (entryService == service && entryAction == action) {
                return true
            }
        }
        return false
    }
}
