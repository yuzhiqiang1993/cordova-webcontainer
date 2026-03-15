package com.yzq.cordova_webcontainer.config

import org.apache.cordova.Config
import org.apache.cordova.ConfigXmlParser
import org.apache.cordova.LOG

/**
 * @author yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 * @description 封装对 Cordova Config 的反射操作，解决 Config.parser 已弃用且为全局静态状态的问题。
 * 兼容范围：Cordova Android 10.x - 12.x (当前主要版本)
 */
object ConfigBridge {
    private const val TAG = "ConfigBridge"

    /**
     * 将解析后的 ConfigXmlParser 注入到 Config.parser 中。
     * 这样做是因为 Cordova 内部许多组件仍然依赖于静态的 Config.parser。
     */
    fun setupConfig(parser: ConfigXmlParser) {
        try {
            val parserField = Config::class.java.getDeclaredField("parser")
            parserField.isAccessible = true
            parserField[null] = parser
            LOG.i(TAG, "Successfully injected Config.parser via reflection.")
        } catch (e: Throwable) {
            LOG.e(TAG, "Failed to inject Config.parser.", e)
            throw IllegalStateException("Failed to inject Config.parser", e)
        }
    }
}
