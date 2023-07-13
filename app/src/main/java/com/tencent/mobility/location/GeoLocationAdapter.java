package com.tencent.mobility.location;

import android.content.Context;
import android.util.Pair;

import com.tencent.map.fusionlocation.TencentLocationAdapter;
import com.tencent.map.fusionlocation.model.TencentGeoLocation;
import com.tencent.map.fusionlocation.model.TencentGnssInfo;
import com.tencent.map.fusionlocation.observer.TencentGeoLocationObserver;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.map.geolocation.internal.TencentExtraKeys;
import com.tencent.map.geolocation.routematch.bean.init.LocationPreference;
import com.tencent.mobility.util.Singleton;
import com.tencent.navix.api.NavigatorZygote;

import java.util.ArrayList;

/**
 * 定位SDK管理方法。
 */
public class GeoLocationAdapter implements IGeoLocation {

    // 单例方法
    public static final Singleton<GeoLocationAdapter> singleton =
            new Singleton<GeoLocationAdapter>() {
                @Override
                protected GeoLocationAdapter create() {
                    return new GeoLocationAdapter();
                }
            };
    private final ArrayList<IGeoLocationListeners> geoLists = new ArrayList<>();
    private TencentLocationAdapter mGeoAdapter;
    // 普适定位回调
    private final TencentGeoLocationObserver mTencentGeoLocationObserver = new TencentGeoLocationObserver() {
        @Override
        public void onGeoLocationChanged(TencentGeoLocation tencentGeoLocation) {
            for (IGeoLocationListeners listener : geoLists) {
                listener.onGeoLocationChanged(tencentGeoLocation);
            }
        }

        @Override
        public void onNmeaMsgChanged(String s) {
        }

        @Override
        public void onGNSSInfoChanged(TencentGnssInfo tencentGnssInfo) {
        }
    };

    /**
     * 开启定位sdk。
     */
    @Override
    public void startGeoLocationAdapter(Context context) {
        // 避免多次开启
        if (null != mGeoAdapter) {
            return;
        }

        // 1.配置全局Context
        TencentExtraKeys.setContext(context);

        // 2.配置设备ID
        final Pair<String, String> deviceID = new Pair<>(TencentLocationAdapter.TYPE_QIMEI
                , NavigatorZygote.with(context).context().getConfig().getDeviceId());
        TencentLocationAdapter.setDeviceId(context, deviceID);

        final TencentLocationRequest request = TencentLocationRequest.create()
                .setInterval(1000) // 频率1s
                .setAllowGPS(true) // 关闭省电省流量
                .setAllowDirection(true)
                .setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA);

        // 3.获取大定位实例
        mGeoAdapter = TencentLocationAdapter.getInstance(context);
        mGeoAdapter.startCommonLocation(request
                , LocationPreference.PLATFORM_PHONE);

        // 4.普适定位添加listener
        mGeoAdapter.addLocationObserver(mTencentGeoLocationObserver);
    }

    /**
     * 结束定位sdk。
     */
    @Override
    public void stopGeoLocationAdapter() {
        mGeoAdapter.stopCommonLocation();
        mGeoAdapter.destroyAdapter();
        mGeoAdapter = null;
        if (null != geoLists) {
            geoLists.clear();
        }
    }

    /**
     * 添加定位回调监听。
     *
     * @param listener 定位监听
     */
    @Override
    public void addGeoLocationListener(IGeoLocationListeners listener) {
        if (null != listener && !geoLists.contains(listener)) {
            geoLists.add(listener);
        }
    }

    /**
     * 移除定位 sdk 数据监听。
     *
     * @param listener 定位监听
     */
    @Override
    public void removeGeoLocationListener(IGeoLocationListeners listener) {
        if (null != listener && -1 != geoLists.indexOf(listener)) {
            geoLists.remove(listener);
        }
    }

    public interface IGeoLocationListeners {

        void onGeoLocationChanged(TencentGeoLocation tencentGeoLocation);
    }
}
