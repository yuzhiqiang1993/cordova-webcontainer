package com.yzq.cordova_webcontainer.data


/**
 * @description 文档加载状态枚举
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */

enum class DocumentReadyState(val event: String) {
    loading("loading"),//加载中
    interactive("interactive"), //可交互
    complete("complete")// 加载完成
}