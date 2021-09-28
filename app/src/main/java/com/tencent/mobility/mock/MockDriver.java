package com.tencent.mobility.mock;

import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.concurrent.atomic.AtomicLong;

public class MockDriver {

    private static final AtomicLong mDriverId = new AtomicLong(200000);
    private final String mId;
    private final MockCar mCar;
    private LatLng mPosition;

    public MockDriver(MockCar car) {
        mCar = car;
        mId = "mock-android-driver-" + mDriverId.getAndIncrement();
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

    public MockCar getCar() {
        return mCar;
    }
}
