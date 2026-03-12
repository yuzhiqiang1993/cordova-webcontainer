# Cordova WebContainer

[![Maven Central](https://img.shields.io/maven-central/v/com.xeonyu/cordova-webcontainer.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.xeonyu%20AND%20a:cordova-webcontainer)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

一个轻量、现代且易于嵌入的 Android Cordova Web 容器组件库。

本项目基于**定制版** `cordova-android` 进行二次封装。其**核心目的**在于解决传统 Cordova 开发中宿主**必须强行继承 `CordovaActivity` 所带来的 UI 僵化与极高耦合度问题**。在这个库中，底层引擎的初始化与交互被解耦收拢到一个普通的 `RelativeLayout` 自定义 View 中。这让你能在保留 Cordova 原有插件生态与强大双端通信交互能力的前提下，像使用原生 `WebView` 一样，将其灵活地嵌套在 `Activity`、`Fragment` 甚至复杂的折叠屏/侧滑布局中。同时，我们在原生机制上拓展了更容易上手的声明式回调，轻松胜任各种混合开发验证与独立 Web 业务场景。

## 核心优势

- **极致灵活**: 细粒度组件化设计，打破 Activity 继承约束，提供 View 级的最小侵入集成方案。
- **开箱即用**: 提供经过验证的最佳实践基类 (`CordovaWebContainerActivity`, `CordovaWebContainerFragment`) 供快速继承使用。
- **现代化架构**: 原生 AndroidX 兼容及 Kotlin API 侧封装，处理繁重的手动生命周期与事件分发。
- **多维侦听器**: 扩展了页面进度、标题获取、HTML Document API 级别的 `readyStateChange` 以及底层 JS 异常追踪机制。
- **透明式请求拦截**: 提供极简的闭包 API 以拦截资源请求与自定义协议劫持 (`overrideUrlLoading` / `interceptRequest`)。

---

## 运行示例

![Demo](assets/sample.gif)

---

## 安装 (Installation)

> **获取最新版本**: 请前往 [Maven Repository](https://mvnrepository.com/artifact/com.xeonyu/cordova-webcontainer) 查看并获取最新的正式版本号。

在你的工程模块级 `build.gradle.kts` 或 `build.gradle` 文件中引入依赖：

```kotlin
dependencies {
    // 请将 x.x.x 替换为上述链接中查询到的最新版本号
    implementation("com.xeonyu:cordova-webcontainer:x.x.x") 
}
```

---

## 快速开始 (Quick Start)

为满足不同耦合度的业务场景，组件提供了由浅到深两种集成方式。

### 方式一：继承特化基类 (推荐)

该方式适用于绝大部分标准混合开发场景。我们封装了 `CordovaWebContainerActivity` 和 `CordovaWebContainerFragment`，帮助开发者省去向容器转发系统生命周期、ActivityResult 及权限申请回调等各种固式样板代码，又兼备了极高的 UI 自定义拓展性。

**1. 布局中引用 `CordovaWebContainer` 组件**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- 你的任意原生标题栏或其他组合控件 -->
    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <!-- 承载网页的核心 CordovaWebContainer 容器 -->
    <com.yzq.cordova_webcontainer.CordovaWebContainer
        android:id="@+id/web_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
```

**2. 核心代码初始化及挂载**

继承基类并实现相关的抽象组件初始化方法：

```kotlin
class WebContainerActivity : CordovaWebContainerActivity() {
    private lateinit var binding: ActivityWebContainerBinding

    /* 初始化你的原生布局 */
    override fun initContentView() {
        binding = ActivityWebContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /* 绑定并初始化 WebContainer 控件 */
    override fun initWebContainer(): CordovaWebContainer {
        with(binding.webContainer) {
            // 初始化宿主环境，LOG 为 Cordova 内部日志级别
            init(this@WebContainerActivity, LOG.VERBOSE)
        }
        return binding.webContainer
    }

    /* 至此，生命周期、页面逻辑已被内部接管，可专注于拓展交互逻辑 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 加载特定的 Web 业务资源
        binding.webContainer.loadUrl("https://apache.org/")
        
        // 绑定原生滚动或其他的扩展事件
        binding.webContainer.setOnPageScrollChangedListener { xOffset, yOffset, oldX, oldY ->
            Log.i("WebContainer", "当前滚动 Y 轴距离: $yOffset")
        }
    }
}
```

**3. 在 Fragment 中使用（完整示例）**

适合底部导航、多 tab、侧滑容器等场景。推荐直接继承 `CordovaWebContainerFragment`，并在 Fragment 内调用 `webContainer.init(this)`。

**Host Activity（仅负责承载 Fragment）**

```kotlin
class FragmentSampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_sample)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, VantUploaderFragment.newInstance())
                .commit()
        }
    }
}
```

**Fragment（继承基类并初始化容器）**

```kotlin
class VantUploaderFragment : CordovaWebContainerFragment() {
    private var _binding: FragmentVantUploaderBinding? = null
    private val binding get() = checkNotNull(_binding)

    companion object {
        fun newInstance() = VantUploaderFragment()
    }

    override fun initContentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVantUploaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initWebContainer(): CordovaWebContainer {
        return binding.webContainer.apply {
            init(this@VantUploaderFragment, LOG.VERBOSE)
            addPageObserver(object : PageObserver {
                override fun onReceivedTitle(title: String) {
                    // 例如：同步标题到宿主 Activity Toolbar
                }
            })
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

**回调分发说明（重要）**

- 当前版本中，`CordovaWebContainer` 在 Fragment 模式下会通过 Fragment 发起权限请求和 `startActivityForResult`，因此常规场景**不需要**在宿主 Activity 手动转发 `onActivityResult/onRequestPermissionsResult`。
- 如果宿主 Activity 重写了这些系统回调，请确保调用 `super`，否则 FragmentManager 无法把回调继续分发给子 Fragment。

---

### 方式二：完全作为原生自定义 View 自由组合

如果你有着极为特殊的嵌套需求，或者坚决不想改变任何已有的基类设计架构（如使用第三方开源基础库的 Activity），可以直接在 XML 里将 `CordovaWebContainer` 当成一个普通的黑盒 `View` 嵌入并独立工作。

**代价是，作为宿主容器（Activity / Fragment），你必须亲自分发系统事件（如权限组、唤起回传、屏幕翻转缓存恢复）给容器，否则诸多混合开发插件（如拍照）将因为失去生命周期焦点而出现崩溃或无响应。**

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var webContainer: CordovaWebContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webContainer = findViewById(R.id.web_container)

        // 1. 初始化容器
        webContainer.init(this)
        
        // 2. 加载 URL
        webContainer.loadUrl("file:///android_asset/www/index.html")
    }

    // --- 必须手动分发的生命周期与系统事件 (标准固定样板) ---
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webContainer.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        webContainer.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        webContainer.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        webContainer.startActivityForResult(requestCode)
        super.startActivityForResult(intent, requestCode, options)
    }
}
```

---

## 核心 API 手册

在成功调用 `webContainer.init()` 完成运行环境注册后，组件暴露出贴合标准 Web 操作与深度劫持开发的核心方法。全套接口均基于清晰可见的设计与空安全声明。

### 1. 容器控制与操作集合

这是 `CordovaWebContainer` 提供控制 WebView 展示、历史堆栈与清理机制的一等方法集合：

| 方法名称与签名 | 参数 / 类型说明 | 描述详情 / 适用场景 |
| ------ | :------: | ------ |
| `init(activity, logLevel)` | `activity` (AppCompatActivity) <br/> `logLevel` (Int, 默认为ERROR) | 在 Activity 场景下注册引擎、初始化插件列表和 JS 映射层。**这是任何操作的绝对前置前提**。 |
| `init(fragment, logLevel)` | `fragment` (Fragment) <br/> `logLevel` (Int, 默认为ERROR) | 在 Fragment 场景下初始化容器，内部会绑定 `fragment.requireActivity()` 并走 Fragment 的权限/回调分发链路。 |
| `loadUrl(url)` | `url` (String) | 指引底层容器加载指定的 `https://` 混合页面或 `file:///android_asset/...` 本地直装代码。 |
| `reload()` | 无 | 强制触发当前网页的热更新/重新加载。 |
| `canGoBack()` | 无 | 判断当前容器是否支持返回上一页的 H5 网页栈逻辑，返回可用布尔值。 |
| `goBack()` | 无 | 触发系统 WebView 返回上一页（等同于浏览器返回键）。 |
| `canGoForward()` | 无 | 判断当前容器能否前进到下一页，返回布尔值。 |
| `goForward()` | 无 | 触发系统 WebView 前进动作。 |
| `clearCache(includeDiskFiles)` | `includeDiskFiles` (Boolean) | 彻底清理 Web 容器产生的缓存。<br/>`includeDiskFiles` = true 时，将一并把磁盘中的旧静态资源持久化文件一并销毁。 |
| `clearHistory()` | 无 | 一键清空当前 WebView 从被唤起至今累计的所有访问链与历史状态堆栈。 |
| `setWebviewClient(client)` | `CordovaWebviewClient` 实例 | 更替或重写 WebClient 代理池，接管并干预包括离线缓存提取、自定义协议分发和 SSL 认证在内的高级网络生命周期。 |
| `setWebviewChromeClient(client)` | `CordovaWebviewChromeClient` 实例 | 更替或重写 ChromeClient 代理，一般用于接手 H5 发起的系统级 Alert 处理、文件选取或精细控制 ProgressBar 的展示进度控制。 |


### 2. 生命周期回调全景监听 (`PageObserver`)

与生硬的基础 `WebViewClient` 不同，该模块重构了并入 Cordova 解析事件流与基础 JSBridge 信息的综合回传渠道。使用极为快捷的注入实现（`webContainer.addPageObserver()`）。

| 接口侦听方法 | 传参解构说明 | 回调发生时机与推荐用途 |
| ------ | ------ | ------ |
| `onPageStarted(url)` | `url`：当前访问目的路径。 | 视图开始构建但尚未渲染内容。**绝佳的显示顶部 LoadingBar 的契机**。 |
| `onPageFinished(url)` | `url`：完成并落地的目的路径。 | 页面已加载完成。资源树已渲染稳定就绪。 |
| `onProgressChanged(newProgress)` | `newProgress`：0-100。 | 资源加载百分比。用于为进度条不断填装递进而非只是假加载动画。 |
| `onReceivedTitle(title)` | `title`：获取到的网页 Title 变量。 | 每当 DOM 结构动态变迁并读取到最新的 `<title>` 字段时，该值会自动送达，用于更新 Native Actionbar 面包屑。 |
| `readyStateChange(state, url)` | `state`：loading, interactive, complete 几个维度的精准生命状态枚举值。<br/> `url`：发生地点。 | 基于 JS 层 `Document API` 极深维度的加载阶段探测。能在 Native 层面准确知道网页此时处在资源串流还是交互渲染完毕状态。 |
| `onWindowError(url, msg, lineNo, columnNo)` | `msg`：未捕获异常具体错误内容。<br/> `lineNo` & `columnNo`：具体发生异常的行列代码指代。 | **非常关键的高级排错与容错接口**。它不仅截取标准 Web 错误，连底层的 JavaScript 匿名或内抛异常也会被此渠道精确勾取到，用于前端研发人员无缝定位或自动日志采集系统的崩溃推流。 |
| `pluginExecute(plugnExecute)` | 包含对象属性 `service`(发起目标插件类别) 与 `action`(具体触发动作意图)。 | JSBridge 发起通信且恰好打在原生侧即将调度具体对应类方法前的**阻截观测事件**，适合编写业务调试追踪逻辑。 |
| `pluginExecResult(plugnExecResult)` | 包含了原生处理后的 `status`(状态码) 和 `message`(处理成果)。 | 接上篇，当诸如`相机处理完成`并准备向 JS 回流抛投执行成果的一瞬间，你能在此拦截并获得具体的成果内容镜像数据。 |

### 3. 请求劫持与协议定制扩展

借助对基础 Cordova 代理重放的高级 Kotlin Lambda DSL 包裹实现。你可以在无需侵入原有包核心代码的基础上干预一切基于加载阶段的事件网络与协议。

**一、自定义特权协议分发劫持 (等同 `shouldOverrideUrlLoading`)**

应用内可能会碰到使用 `<a href="yourapp://pay">` 来唤起支付宝、分享等特殊外链业务协议的情况。

```kotlin
webContainer.webviewClient.overrideUrlLoading { _, request ->
    val uriStr = request.url.toString()
    if (uriStr.startsWith("yourapp://action/")) {
        // 在此处解析业务逻辑、切断 WebView 并且抛至外层去调起相应的原生 Activity 能力
        // ...
        
        // 关键：消耗并拦截此非法基础网络的加载，防止发生 ERR_UNKNOWN_URL_SCHEME 崩溃
        return@overrideUrlLoading true 
    }
    
    // 返回 false 意味着自己对该类型的链接无兴趣，放给普通系统浏览器或内部逻辑处理
    return@overrideUrlLoading false 
}
```

**二、本地静态资源或跨域接口伪造代理 (等同 `shouldInterceptRequest`)**

可以为项目拦截所有 `png` 扩展的图片请求并指向工程自带的一套内置 `drawable` 高速解密模块等高复杂需求。

```kotlin
webContainer.webviewClient.interceptRequest { _, request, response ->
    val rawUrl = request.url.toString()
    
    if (rawUrl.contains("remote-censor.png")) {
        // 在此处完全构造另一个 WebResourceResponse 覆盖，阻止真实的网络通讯
        // return@interceptRequest myCustomWebResourceResponse
    }
    
    // 如果返回普通的 response，Cordova 内核仍然有最后的干预权限（例如接管 https://localhost/ 白名单请求等）
    return@interceptRequest response
}
```

---

## 高级特性与安全配置

在这套组件中，我们不仅关注灵活解耦，还为了应对各种复杂的网络与恶意代码执行场景，提供了深度的可控策略。

### 1. 核心资源高性能自动注入 (`CordovaInject`)

为了摆脱传统打包中需要在每个 HTML 里手动引入 `<script src="cordova.js"></script>` 的沉重历史包袱，组件内部封装了强大的**无感知自动注入器**：
- **协程级异步解析**：在容器初始化的瞬间即在后台开启协程，将位于 `assets/www` 中的 `cordova.js` 及各种插件 `cordova_plugins.js` 加载并通过 Base64 编码常驻内存。
- **页面生命周期智能拦截**：依靠对 `onPageStarted` 与 `onPageFinished` 的多重埋伏，当发现加载的 DOM 就绪后，会瞬间将存放在内存池内的底层逻辑环境直写至 WebView。
- **配置与开关**：注入默认开启。如果你需要开启日志监控注入性能，可以通过以下前置配置打开：
  ```kotlin
  // (需在组件实例化 init() 之前设定)
  CordovaWebContainerConfig.isLogEnable = true
  CordovaWebContainerConfig.CORDOVA_ASSET_DIR = "www" //默认资源目录
  ```

### 2. Cordova API 安全白名单防火墙 (`CordovaWhitelistInterceptor`)

在部分需要动态加载外部（甚至不受信的公开 WebH5 内容）以混合展示的业务诉求下。放任外部 JS 直接调用底层任意原生的定位、拍照甚至本地持久化组件是非常危险的。
为此，特别引入了安全白名单拦截能力，能有效阻止不受信的跨域域名滥用底层服务。

```kotlin
// 1. 在容器初始化（ webContainer.init(...) ）之前手动挂载这套安全引擎
CordovaWebContainerConfig.ENABLE_CORDOVA_API_WHITELIST = true

// 2. 注入你预定义的全局防御与域名白名单策略
CordovaWebContainerConfig.cordovaWhitelistConfig = WhitelistConfig(
    // 信任的域名列表（逗号分隔），匹配该列表中域名的页面将拥有所有 API 权限
    trustedDomains = "localhost, trusted.partner.com",
    
    // 全局放行的 API 列表（如 "Geolocation/*" 放行该插件所有动作，"Camera/takePicture" 精确放行动作）
    trustedApis = listOf("Geolocation/*"),
    
    // 细粒度的域名规则：针对特定域名，细化其被允许调用的 API 列表
    rules = listOf(
        WhitelistConfig.Rule(
            domain = "api.limited-partner.com",
            comment = "业务合作方的特定沙盒限制",
            allow = listOf("Camera/*", "Media/*")
        )
    )
)
```

> 说明：当前版本真正生效的是 `CordovaWebContainerConfig.ENABLE_CORDOVA_API_WHITELIST`。`WhitelistConfig.enable` 字段目前是保留字段，不参与拦截判断。

---

## ProGuard 代码混淆与构建

**零配置防混淆介入**。

从 `1.1.x` 起，核心组件内已**内置下发了完整的 Consumer ProGuard 豁免规则**。这不仅自动保留了 `org.apache.cordova.**` 底层类不被 R8 擦除，还一并完美豁免了所有含有 `@JavascriptInterface` 注解的反向反射隧道。

在绝大多数工程集成下，宿主无需在任何地方修改额外的 `proguard-rules.pro` 亦可顺利完成生产包环境构建！

---

## 开源许可证 (License)

本项目基于 [Apache License 2.0](LICENSE) 协议开源分发及演进。
