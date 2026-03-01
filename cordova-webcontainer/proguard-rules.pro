#-------------------------------------------------
# Cordova WebContainer 组件内部混淆豁免规则
#-------------------------------------------------

# 保留 Apache Cordova 底层基础关联类不被擦除改名
-keep class org.apache.cordova.** { *; }
-keep interface org.apache.cordova.** { *; }
-keep enum org.apache.cordova.** { *; }

# 保持含有 @JavascriptInterface 注解的方法与类，防 JavaScript 桥接调用失效
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 保持插件反射调用时的入口类免遭混淆
-keep public class * extends org.apache.cordova.CordovaPlugin {
    public <init>(...);
    public <methods>;
}

# 保持自定义的 JsInterface 对象不被混淆
-keep class com.yzq.cordova_webcontainer.core.CordovaJsInterface { *; }
-keep class * extends com.yzq.cordova_webcontainer.core.CordovaJsInterface { *; }
