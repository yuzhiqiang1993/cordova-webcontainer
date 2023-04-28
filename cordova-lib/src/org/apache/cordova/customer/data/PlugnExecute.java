package org.apache.cordova.customer.data;


/**
 * @author yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 * @description js调原生插件数据类
 * @date 2023/3/27
 * @time 14:10
 */

public class PlugnExecute {
    /*url*/
    public String url = "";
    /*插件的名称*/
    public String pluginName = "";
    /*js对象*/
    public String pluginClassName = "";
    /*callback id*/
    public String callbackId = "";
    /*action*/
    public String action = "";
    /*参数*/
    public String rawArgs = "";

    public PlugnExecute() {
    }

    public PlugnExecute(String pluginName, String pluginClassName, String callbackId, String action, String rawArgs) {
        this.pluginName = pluginName;
        this.pluginClassName = pluginClassName;
        this.callbackId = callbackId;
        this.action = action;
        this.rawArgs = rawArgs;
    }

    @Override
    public String toString() {
        return "PlugnExecute{" +
                "url='" + url + '\'' +
                ", pluginName='" + pluginName + '\'' +
                ", pluginClassName='" + pluginClassName + '\'' +
                ", callbackId='" + callbackId + '\'' +
                ", action='" + action + '\'' +
                ", rawArgs='" + rawArgs + '\'' +
                '}';
    }
}
