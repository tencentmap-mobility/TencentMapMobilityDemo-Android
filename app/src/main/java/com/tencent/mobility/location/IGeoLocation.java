package com.tencent.mobility.location;

import android.content.Context;

public interface IGeoLocation {

    /**
     * 开启定位sdk。
     */
    void startGeoLocationAdapter(Context context);

    /**
     * 结束定位SDK。
     */
    void stopGeoLocationAdapter();

    /**
     * 添加定位数据回调监听。
     *
     * @param listener 回调监听
     */
    void addGeoLocationListener(GeoLocationAdapter.IGeoLocationListeners listener);

    /**
     * 移除定位数据回调监听。
     *
     * @param listener 回调监听
     */
    void removeGeoLocationListener(GeoLocationAdapter.IGeoLocationListeners listener);

}
