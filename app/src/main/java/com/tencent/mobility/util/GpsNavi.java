package com.tencent.mobility.util;

import android.content.Context;

import com.tencent.map.fusionlocation.TencentLocationAdapter;
import com.tencent.map.fusionlocation.model.TencentGeoLocation;
import com.tencent.map.fusionlocation.model.TencentGnssInfo;
import com.tencent.map.fusionlocation.observer.TencentGeoLocationObserver;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.map.geolocation.routematch.bean.init.LocationPreference;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.map.navi.data.GpsLocation;

/**
 * 导航定位点提供管理类，提供给导航sdk必要的定位点
 *
 * @author selenali
 */
public class GpsNavi {

    /**
     * 腾讯定位sdk定位request
     */
    private final TencentLocationRequest request = TencentLocationRequest.create();

    /**
     * 腾讯定位sdk定位管理类
     */
    private TencentLocationAdapter locationAdapter = null;

    /**
     * 导航sdk实例
     */
    private TencentCarNaviManager tencentCarNaviManager = null;

    private TencentLocation curLocation = null;
    private final TencentGeoLocationObserver locationObserver = new TencentGeoLocationObserver() {
        @Override
        public void onGeoLocationChanged(TencentGeoLocation tencentGeoLocation) {
            curLocation = tencentGeoLocation.getLocation();
//            Log.e(tencentGeoLocation.toString());
            // 通知导航sdk定位点变化
            if (tencentCarNaviManager != null) {
                tencentCarNaviManager.updateLocation(convertToGpsLocation(tencentGeoLocation.getLocation()),
                        tencentGeoLocation.getStatus(), tencentGeoLocation.getReason());
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
     * 定位sdk开始工作
     */
    public void enableGps(Context context) {
        request.setInterval(1000)
                .setAllowGPS(true)
                .setAllowDirection(true)
                .setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA);
        TencentLocationAdapter.setMockGpsEnable(true);
        locationAdapter = TencentLocationAdapter.getInstance(context);
        locationAdapter.startCommonLocation(request, LocationPreference.PLATFORM_PHONE);
        locationAdapter.addLocationObserver(locationObserver);
    }

    /**
     * 定位sdk停止工作
     */
    public void disableGps() {
        if (locationAdapter == null) {
            return;
        }
        locationAdapter.stopCommonLocation();
    }

    /**
     * 设置导航实例对象
     */
    public void setNavigationManager(TencentCarNaviManager naviManager) {
        this.tencentCarNaviManager = naviManager;
    }

    public TencentLocation getCurrentLocation() {
        return curLocation;
    }

    private GpsLocation convertToGpsLocation(TencentLocation tencentLocation) {
        if (tencentLocation == null) {
            return null;
        }
        GpsLocation location = new GpsLocation();
        location.setProvider("gps");
        location.setFusionProvider("gps");
        location.setDirection(tencentLocation.getBearing());
        location.setAccuracy(tencentLocation.getAccuracy());
        if (tencentLocation.getAccuracy() == 0) {
            location.setAccuracy(10);
        }
        location.setLatitude(tencentLocation.getLatitude());
        location.setLongitude(tencentLocation.getLongitude());
        location.setAltitude(tencentLocation.getAltitude());
//        location.setProvider(tencentLocation.getProvider());
        location.setVelocity(tencentLocation.getSpeed());
        location.setTime(tencentLocation.getTime());
//        location.setGpsRssi(tencentLocation.getGPSRssi());
        location.setGpsRssi(4);

        return location;
    }

}
