# Cordova WebContainer

[![Maven Central](https://img.shields.io/maven-central/v/com.xeonyu/cordova-webcontainer.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.xeonyu%20AND%20a:cordova-webcontainer)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

`CordovaWebContainer` 是一个可嵌入 `Activity`、`Fragment` 或任意布局中的 Android Cordova 容器。

它的目标很简单：保留 Cordova 插件生态和双端通信能力，同时避免宿主必须继承 `CordovaActivity`。对使用者来说，重点不是内部怎么实现，而是你可以像使用普通 `WebView` 一样把它放进页面里，然后继续使用 Cordova 能力。

---

## 安装

> 最新版本请前往 [Maven Repository](https://mvnrepository.com/artifact/com.xeonyu/cordova-webcontainer) 查看。

```kotlin
dependencies {
    implementation("com.xeonyu:cordova-webcontainer:x.x.x")
}
```

---



## 快速开始

组件提供 3 种常见接入方式。优先选基类方式；只有在你必须保留现有宿主结构时，再用“纯 View 模式”。

### 方式一：继承 `CordovaWebContainerActivity`（推荐）

适用于标准页面场景。生命周期、状态恢复和常规回调链路由基类接管，你只需要初始化容器并加载页面。

**布局**

```xml
<com.yzq.cordova_webcontainer.CordovaWebContainer
    android:id="@+id/web_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

**Activity**

```kotlin
class WebContainerActivity : CordovaWebContainerActivity() {
    private lateinit var binding: ActivityWebContainerBinding

    override fun initContentView() {
        binding = ActivityWebContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initWebContainer(): CordovaWebContainer {
        return binding.webContainer.apply {
            init(this@WebContainerActivity, LOG.VERBOSE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.webContainer.loadUrl("https://apache.org/")
    }
}
```

### 方式二：继承 `CordovaWebContainerFragment`

适用于多 Tab、底部导航、侧滑容器、嵌套页面等场景。

```kotlin
class VantUploaderFragment : CordovaWebContainerFragment() {
    private var _binding: FragmentVantUploaderBinding? = null
    private val binding get() = checkNotNull(_binding)

    override fun initContentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVantUploaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initWebContainer(): CordovaWebContainer {
        return binding.webContainer.apply {
            init(this@VantUploaderFragment, LOG.VERBOSE)
        }
    }

    override fun initWidget() {
        binding.webContainer.loadUrl("https://vant-ui.github.io/vant/v2/mobile.html#/zh-CN/uploader")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**注意**

| 项 | 说明 |
| --- | --- |
| 权限和 ActivityResult | Fragment 模式下，常规 Cordova 插件权限申请与结果回调由容器内部处理，通常不需要宿主 Activity 手动转发 |
| 宿主 Activity | 如果你重写了系统回调，请务必调用 `super`，否则 FragmentManager 可能无法继续分发回调 |

### 方式三：纯 View 模式

适用于你不想继承任何基类，只想把容器当成一个普通 View 嵌入现有页面的场景。

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var webContainer: CordovaWebContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webContainer = findViewById(R.id.web_container)
        webContainer.init(this, savedInstanceState)
        webContainer.loadDefaultUrl()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webContainer.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }
}
```

**什么时候需要手动转发回调？**

只有一种情况：宿主自己拦截了 Cordova 发起的 `requestCode` / ActivityResult / 权限回调，但没有把它继续交还给容器。这时才需要手动调用：

- `webContainer.startActivityForResult(...)`
- `webContainer.onActivityResult(...)`
- `webContainer.onRequestPermissionsResult(...)`

这类兜底只适用于 Cordova 自己登记过的请求，不会帮你推断任意非 Cordova 请求应该分发给哪个插件。

---

## 本地页面加载

如果你的页面资源在 `assets/www` 中，推荐用 `https://localhost/...` 访问，而不是直接使用 `file:///android_asset/...`。

| 场景 | 推荐写法 |
| --- | --- |
| 加载默认启动页 | `webContainer.loadDefaultUrl()` |
| 加载本地 HTML | `webContainer.loadUrl("https://localhost/index.html")` |
| 加载本地子页面 | `webContainer.loadUrl("https://localhost/pages/demo.html")` |
| 加载远程页面 | `webContainer.loadUrl("https://example.com")` |

---

## 核心 API

### 1. 容器控制

| 方法 | 参数 | 用途 |
| --- | --- | --- |
| `init(activity, logLevel)` | `AppCompatActivity`, `Int` | Activity 场景初始化 |
| `init(activity, savedInstanceState, logLevel)` | `AppCompatActivity`, `Bundle?`, `Int` | Activity 场景初始化并恢复状态 |
| `init(fragment, logLevel)` | `Fragment`, `Int` | Fragment 场景初始化 |
| `init(fragment, savedInstanceState, logLevel)` | `Fragment`, `Bundle?`, `Int` | Fragment 场景初始化并恢复状态 |
| `loadUrl(url)` | `String` | 加载指定页面 |
| `loadDefaultUrl()` | 无 | 加载 `config.xml` 中定义的默认启动页 |
| `reload()` | 无 | 重新加载当前页面 |
| `canGoBack()` | 无 | 是否可以后退 |
| `goBack()` | 无 | 页面后退 |
| `canGoForward()` | 无 | 是否可以前进 |
| `goForward()` | 无 | 页面前进 |
| `clearCache(includeDiskFiles)` | `Boolean` | 清理缓存 |
| `clearHistory()` | 无 | 清理历史记录 |
| `onSaveInstanceState(outState)` | `Bundle` | 保存容器状态 |
| `restoreInstanceState(savedInstanceState)` | `Bundle?` | 恢复容器状态 |
| `setOnPageScrollChangedListener(listener)` | `PageScrollChangedListener` | 监听页面滚动 |

### 2. 运行时约束

| 规则 | 说明 |
| --- | --- |
| 初始化前调用业务 API | 会直接抛出明确异常，不会以 `lateinit` 形式崩溃 |
| 已销毁实例 | 不支持在同一实例上再次 `init()`，应重新创建新的容器实例 |
| 系统回调类 API | `onSaveInstanceState(...)`、`restoreInstanceState(...)`、`onActivityResult(...)` 等在容器未 ready 时会安静返回 |

---

## 页面事件监听：`PageObserver`

通过 `addPageObserver(...)` 可以监听页面加载、标题变化、JS 错误、插件调用等事件。

```kotlin
binding.webContainer.addPageObserver(object : PageObserver {
    override fun onPageStarted(url: String) {
        Log.i("Web", "start=$url")
    }

    override fun onReceivedTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun onWindowError(url: String, msg: String, lineNo: Int, columnNo: Int) {
        Log.e("Web", "js error: $msg @ $url:$lineNo:$columnNo")
    }
})
```

### `PageObserver` 回调一览

| 方法 | 说明 |
| --- | --- |
| `onPageStarted(url)` | 页面开始加载 |
| `onPageFinished(url)` | 页面完成加载 |
| `onProgressChanged(newProgress)` | 页面加载进度变化 |
| `onReceivedTitle(title)` | 收到网页标题 |
| `readyStateChange(state, url)` | DOM `readyState` 变化 |
| `onPageError(errorCode, description, failingUrl)` | 页面加载错误 |
| `onWindowError(url, msg, lineNo, columnNo)` | 捕获前端 JS 错误 |
| `onNavigationAttempt(url)` | 导航尝试 |
| `onOverrideUrlLoading(url)` | 命中覆盖跳转逻辑 |
| `shouldAllowNavigation(url)` | 命中导航放行判断 |
| `shouldOpenExternalUrl(url)` | 命中外部链接处理判断 |
| `pluginExecute(plugnExecute)` | JS 调用原生插件前的观察点 |
| `pluginExecResult(plugnExecResult)` | 原生插件结果返回前的观察点 |
| `onHostCreate/Start/Resume/Pause/Stop/Destroy(...)` | 宿主生命周期事件 |

---

## 自定义 JS Bridge：`CordovaJsInterface`

如果你希望在网页里通过 `window.xxx` 调用原生方法，可以继承 `CordovaJsInterface` 并通过 `addJavascriptInterface(...)` 注入。

### API 一览

| 项 | 说明 |
| --- | --- |
| `CordovaJsInterface(jsName)` | 定义注入到 JS 中的对象名 |
| `getJsName()` | 返回当前对象名 |
| `addJavascriptInterface(jsInterface)` | 把自定义 bridge 注入到当前容器 |

### 示例

```kotlin
class NativeBridge : CordovaJsInterface("NativeBridge") {
    @JavascriptInterface
    fun showToast(message: String) {
        Log.i("NativeBridge", "message=$message")
    }
}

binding.webContainer.init(this, savedInstanceState)
binding.webContainer.addJavascriptInterface(NativeBridge())
```

前端调用：

```javascript
window.NativeBridge.showToast("hello from web")
```

### 使用注意

| 项 | 说明 |
| --- | --- |
| 暴露给 JS 的方法 | 必须显式加 `@JavascriptInterface` |
| 调用时机 | 必须在 `init()` 完成之后调用 `addJavascriptInterface(...)` |
| `getJsName()` | 只负责对象名，不会自动暴露任意方法 |

---

## 网络与跳转扩展：`CordovaWebviewClient`

`CordovaWebviewClient` 是对 `SystemWebViewClient` 的封装，提供两个最常用的扩展点：请求拦截和跳转拦截。

### API 一览

| 方法 | 签名 | 说明 |
| --- | --- | --- |
| `interceptRequest(block)` | `(view, request, response) -> WebResourceResponse?` | 对应 `shouldInterceptRequest` |
| `overrideUrlLoading(block)` | `(view, request) -> Boolean` | 对应 `shouldOverrideUrlLoading` |
| `setWebviewClient(client)` | `CordovaWebviewClient` | 替换整个 client |

### 类型签名

```kotlin
typealias CordovaInterceptRequest = (
    view: WebView,
    request: WebResourceRequest,
    response: WebResourceResponse?
) -> WebResourceResponse?

typealias CordovaOverrideUrlLoading = (
    view: WebView,
    request: WebResourceRequest
) -> Boolean
```

### 示例 1：拦截自定义 Scheme

```kotlin
binding.webContainer.webviewClient.overrideUrlLoading { _, request ->
    val url = request.url.toString()
    if (url.startsWith("yourapp://action/")) {
        handleNativeAction(url)
        return@overrideUrlLoading true
    }
    false
}
```

### 示例 2：拦截资源请求

```kotlin
binding.webContainer.webviewClient.interceptRequest { _, request, response ->
    val url = request.url.toString()
    if (url.endsWith("mock.png")) {
        return@interceptRequest myMockResponse
    }
    response
}
```

### 使用注意

| 项 | 说明 |
| --- | --- |
| 返回 `null` | 继续走默认 Cordova / WebView 处理链路 |
| 返回非空 `WebResourceResponse` | 直接覆盖默认加载结果 |
| 替换整个 `WebviewClient` | 原先挂在默认实例上的闭包不会自动迁移 |

---

## Chrome 行为扩展：`CordovaWebviewChromeClient`

`CordovaWebviewChromeClient` 当前最常用的扩展点是网页定位授权弹窗。默认实现已经覆盖 `onGeolocationPermissionsShowPrompt(...)`，会展示一套原生确认对话框。

### API 一览

| 方法 | 说明 |
| --- | --- |
| `onGeolocationPermissionsShowPrompt(origin, callback)` | 当网页请求地理定位权限时触发 |
| `setWebviewChromeClient(client)` | 替换整个 `CordovaWebviewChromeClient` |

### 默认行为

| 行为 | 说明 |
| --- | --- |
| 文案展示 | 自动解析 `origin` 的 host 并展示 |
| 点击允许 | 调用 `callback.invoke(origin, true, true)` |
| 点击拒绝 | 调用 `callback.invoke(origin, false, true)` |

### 示例：自定义定位授权策略

```kotlin
class CustomChromeClient(parentEngine: SystemWebViewEngine) : CordovaWebviewChromeClient(parentEngine) {
    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback,
    ) {
        if (origin.contains("trusted.partner.com")) {
            callback.invoke(origin, true, true)
            return
        }
        super.onGeolocationPermissionsShowPrompt(origin, callback)
    }
}

binding.webContainer.setWebviewChromeClient(
    CustomChromeClient(binding.webContainer.webViewEngine)
)
```

---

## 安全白名单配置

如果你会加载外部页面，建议开启 Cordova API 白名单，避免不受信页面直接调用原生插件能力。

```kotlin
CordovaWebContainerConfig.ENABLE_CORDOVA_API_WHITELIST = true
CordovaWebContainerConfig.cordovaWhitelistConfig = WhitelistConfig(
    enable = true,
    trustedDomains = "localhost, trusted.partner.com",
    trustedApis = listOf("Geolocation/*"),
    rules = listOf(
        WhitelistConfig.Rule(
            domain = "api.limited-partner.com",
            comment = "业务合作方的特定沙盒限制",
            allow = listOf("Camera/*", "Media/*")
        )
    )
)
```

### 配置项说明

| 配置项 | 说明 |
| --- | --- |
| `ENABLE_CORDOVA_API_WHITELIST` | 全局总开关 |
| `WhitelistConfig.enable` | 当前白名单配置是否启用 |
| `trustedDomains` | 完全信任的域名列表，支持域名和子域名匹配 |
| `trustedApis` | 全局允许调用的 API 列表 |
| `rules` | 针对特定域名的细粒度放行规则 |

---

## 自动注入说明

组件内部会自动处理 `cordova.js`、`cordova_plugins.js` 等脚本注入，你通常不需要在每个 HTML 中手动写：

```html
<script src="cordova.js"></script>
```

如果你需要开启相关日志：

```kotlin
CordovaWebContainerConfig.isLogEnable = true
CordovaWebContainerConfig.CORDOVA_ASSET_DIR = "www"
```

---

## ProGuard

从 `1.1.x` 起，组件内已经内置 Consumer ProGuard 规则。大多数场景下，宿主无需额外添加混淆配置即可正常构建。

---

## License

本项目基于 [Apache License 2.0](LICENSE) 开源。
