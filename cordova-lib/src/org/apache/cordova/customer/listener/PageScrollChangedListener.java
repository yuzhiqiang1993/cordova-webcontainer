package org.apache.cordova.customer.listener;


/**
 * @author yuzhiqiang (zhiqiang.yu.xeon@gmail.com)
 * @description 页面滚动的监听
 * @date 2023/4/28
 * @time 13:45
 */

public interface PageScrollChangedListener {
    /**
     * @param xOffset    x轴的偏移量
     * @param yOffset    y轴的偏移量
     * @param oldxOffset 滚动前x抽的偏移量
     * @param oldyOffset 滚动前y轴的偏移量
     */
    void onScrollChanged(int xOffset, int yOffset, int oldxOffset, int oldyOffset);
}
