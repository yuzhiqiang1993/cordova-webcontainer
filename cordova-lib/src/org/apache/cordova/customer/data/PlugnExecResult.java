package org.apache.cordova.customer.data;


/**
 * @author yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 * @description js调原生插件执行结束的数据类
 * @date 2023/3/27
 * @time 14:10
 */

public class PlugnExecResult {
    /*url*/
    public String url = "";
    /*callback id*/
    public String callbackId = "";
    public String status = "";
    /*返回值*/
    public String result = "";

    public PlugnExecResult() {
    }

    public PlugnExecResult(String callbackId, String status, String result) {
        this.callbackId = callbackId;
        this.status = status;
        this.result = result;
    }

    @Override
    public String toString() {
        return "PlugnExecResult{" +
                "url='" + url + '\'' +
                ", callbackId='" + callbackId + '\'' +
                ", status='" + status + '\'' +
                ", result='" + result + '\'' +
                '}';
    }
}
