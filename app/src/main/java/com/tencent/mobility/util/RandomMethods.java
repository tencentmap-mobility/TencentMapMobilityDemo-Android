package com.tencent.mobility.util;

import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.navix.api.map.MapApi;
import com.tencent.navix.api.model.NavSearchPoint;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptor;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.LatLngBounds;
import com.tencent.tencentmap.mapsdk.maps.model.VisibleRegion;

import java.util.Random;

public class RandomMethods {

    private static final RandomMethods single = new RandomMethods();
    private MapApi mMap;

    private RandomMethods() {
    }

    public static RandomMethods getInstance() {
        return single;
    }

    public void initCases(MapApi map) {
        mMap = map;
    }

    public TLSBPosition getRandTLSBPosition(String info) {
        LatLng latLng = getVisibleLatLng();
        if (latLng == null) {
            return null;
        }
        TLSBPosition position = new TLSBPosition();
        position.setLatitude(latLng.getLatitude());
        position.setLongitude(latLng.getLongitude());
        position.setProvider(1);
        position.setVelocity(getFloat(15, 5));
        position.setBearing(getInt(360));
        position.setCityCode("100101");
        position.setExtraInfo(info);
        return position;
    }

    public NavSearchPoint getVisibleNaviPoi() {
        LatLng latLng = getVisibleLatLng();
        if (latLng == null) {
            return null;
        }
        return new NavSearchPoint(latLng.getLatitude(), latLng.getLongitude());
    }

    public LatLng getVisibleLatLng() {
        if (mMap != null) {
            VisibleRegion visibleRegion = mMap.getProjection().getVisibleRegion();
            LatLng nearLeft = visibleRegion.nearLeft;
            LatLng farRight = visibleRegion.farRight;
            double lat = nearLeft.latitude + (farRight.latitude - nearLeft.latitude) * Math.random();
            double lng = nearLeft.longitude + (farRight.longitude - nearLeft.longitude) * Math.random();
            return new LatLng(lat, lng);
        } else {
            return null;
        }
    }

    public LatLng getLatLngFromBounds(LatLngBounds bounds) {
        LatLng nearLeft = bounds.southwest;
        LatLng farRight = bounds.northeast;
        double lat = nearLeft.latitude + (farRight.latitude - nearLeft.latitude) * Math.random();
        double lng = nearLeft.longitude + (farRight.longitude - nearLeft.longitude) * Math.random();
        return new LatLng(lat, lng);
    }

    public int getInt(int num1, int num2) {
        int num = (int) (num1 * Math.random() + num2);
        return num;
    }

    public int getInt(int num) {
        return (int) (num * Math.random());
    }

    public float getFloat(float num1, float num2) {
        float num = (float) (num1 * Math.random() + num2);
        return num;
    }

    public float getFloat(float num) {
        return (float) (num * Math.random());
    }

    public double getDouble(float num1, float num2) {
        double num = num1 * Math.random() + num2;
        return num;
    }

    public double getDouble(float num) {
        return (num * Math.random());
    }

    public boolean getBoolean() {
        double num = Math.random();
        return !(num < 0.5);
    }

    public int getColor() {
        String red;
        String green;
        String blue;
        String alpha;
        Random random = new Random();
        alpha = Integer.toHexString(255).toUpperCase();
        //Integer.toHexString(random.nextInt(256)).toUpperCase();
        red = Integer.toHexString(random.nextInt(256)).toUpperCase();
        green = Integer.toHexString(random.nextInt(256)).toUpperCase();
        blue = Integer.toHexString(random.nextInt(256)).toUpperCase();

        alpha = alpha.length() == 1 ? "0" + alpha : alpha;
        red = red.length() == 1 ? "0" + red : red;
        green = green.length() == 1 ? "0" + green : green;
        blue = blue.length() == 1 ? "0" + blue : blue;
        String color = alpha + red + green + blue;
        long val = Long.parseLong(color, 16);
        if (color.compareTo("7FFFFFFF") > 0) {
            long max = Long.parseLong("100000000", 16);
            return (int) (val - max);
        } else {
            return (int) val;
        }
    }


    public int[] getColors(int size) {
        int[] colors = new int[size];
        for (int i = 0; i < size; i++) {
            colors[i] = getColor();
        }
        return colors;
    }


    public BitmapDescriptor getBitmapDescriptor() {
        int index = getInt(36, 1);
        return BitmapDescriptorFactory.fromAsset(index + ".jpg");
    }

}
