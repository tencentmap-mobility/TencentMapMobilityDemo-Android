package com.tencent.mobility.mock;

import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.concurrent.atomic.AtomicLong;

public class MockPassenger {
    private static final AtomicLong mPassengerId = new AtomicLong(100000);
    private final String mId;
    private LatLng mPosition;
    private String mOrderId;

    public MockPassenger() {
        mId = "mock-android-passenger-" + mPassengerId.getAndIncrement();
    }

    public String getId() {
        return mId;
    }

    public void setPosition(LatLng position) {
        mPosition = position;
    }

    public LatLng getPosition() {
        return mPosition;
    }

    public String getOrderId() {
        return mOrderId;
    }

    public void setOrderId(String orderId) {
        mOrderId = orderId;
    }
}
