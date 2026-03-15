package com.yzq.cordova_webcontainer.state

/**
 * @description 容器状态控制器
 * @author  yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 */
internal class ContainerStateController(
    private val ownerName: String,
) {
    enum class ContainerState {
        NEW,
        INITIALIZING,
        READY,
        DESTROYED,
    }

    var currentState: ContainerState = ContainerState.NEW
        private set

    fun beginInit(): Boolean {
        return when (currentState) {
            ContainerState.NEW -> {
                currentState = ContainerState.INITIALIZING
                true
            }

            ContainerState.READY,
            ContainerState.INITIALIZING -> false

            ContainerState.DESTROYED -> error("$ownerName is destroyed. Create a new instance.")
        }
    }

    fun markReady() {
        currentState = ContainerState.READY
    }

    fun markDestroyed() {
        currentState = ContainerState.DESTROYED
    }

    fun isDestroyed(): Boolean {
        return currentState == ContainerState.DESTROYED
    }

    fun requireReady() {
        check(currentState == ContainerState.READY) {
            "$ownerName is not ready. state=$currentState. Call init() first."
        }
    }

    fun isCordovaReady(cordovaInitialized: Boolean): Boolean {
        return currentState == ContainerState.READY && cordovaInitialized
    }

    fun isWebViewReady(webViewInitialized: Boolean): Boolean {
        return currentState == ContainerState.READY && webViewInitialized
    }
}
