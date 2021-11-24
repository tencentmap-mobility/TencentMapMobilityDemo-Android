package com.tencent.mobility.mock;

import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

public class MockUser {

    private final String mId;
    private final MockCar mCar;
    private String mOrderId;
    private LatLng mPosition;
    private LatLng mStart;
    private LatLng mEnd;

    public MockUser(String id) {
        mId = id;
        mCar = new MockCar();
    }

    public MockUser(String id, MockCar car) {
        mId = id;
        mCar = car;
    }

    public MockCar getCar() {
        return mCar;
    }

    public void setBizType(@MockCar.BizType int bizType) {
        mCar.setBizType(bizType);
    }

    public void setCarType(@MockCar.CarType int carType) {
        mCar.setCarType(carType);
    }

    public String getId() {
        return mId;
    }

    public String getOrderId() {
        return mOrderId;
    }

    public void setOrderId(String orderId) {
        mOrderId = orderId;
    }

    public LatLng getPosition() {
        return mPosition;
    }

    public void setPosition(LatLng position) {
        mPosition = position;
    }

    public LatLng getStart() {
        return mStart;
    }

    public void setStart(final LatLng start) {
        mStart = start;
    }

    public LatLng getEnd() {
        return mEnd;
    }

    public void setEnd(final LatLng end) {
        mEnd = end;
    }
}
