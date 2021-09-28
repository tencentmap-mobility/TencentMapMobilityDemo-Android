package com.tencent.mobility.util;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.lssupport.bean.TLSBDriverPosition;
import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.navi.data.NaviPoi;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

public class ConvertUtils {

    public static TLSBDriverPosition tenPoToTLSDPo(TencentLocation tenLo) {
        if (tenLo == null) {
            return null;
        }

        TLSBDriverPosition driverPosition = new TLSBDriverPosition();
        driverPosition.setTime(tenLo.getTime());
        driverPosition.setCityCode(tenLo.getCityCode());
        driverPosition.setLatitude(tenLo.getLatitude());
        driverPosition.setLongitude(tenLo.getLongitude());
        driverPosition.setAccuracy(tenLo.getAccuracy());
        driverPosition.setBearing((float) tenLo.getBearing());
        driverPosition.setVelocity(tenLo.getSpeed());
        driverPosition.setAltitude(tenLo.getAltitude());
        driverPosition.setProvider(tenLo.getProvider());
        driverPosition.setCityCode(tenLo.getCityCode());
        return driverPosition;
    }

    public static TLSBPosition tenPoTOTLSPo(TencentLocation location) {
        TLSBPosition position = new TLSBPosition();
        position.setLatitude(location.getLatitude());
        position.setLongitude(location.getLongitude());
        position.setProvider(location.getProvider());
        position.setVelocity(location.getSpeed());
        position.setBearing(location.getBearing());
        position.setCityCode(location.getCityCode());
        return position;
    }

    public static NaviPoi toNaviPoi(LatLng latLng) {
        return new NaviPoi(latLng.latitude, latLng.longitude);
    }

}
