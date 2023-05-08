package com.tencent.mobility.mock;

import android.text.TextUtils;

import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.HashSet;
import java.util.Set;

public class MockOrder {

    private final MockCar mCar;
    Set<MockOrder> subOrders = new HashSet<>();
    private String mId;
    private String mPassengerId;
    private MockPassenger mPassenger;
    private MockDriver mDriver;
    private LatLng mBegin;
    private LatLng mEnd;
    private Status mStatus = Status.Idle;
    private String mDriverId;
    private boolean isDriverOrder;
    private String mCarpoolOrderId;

    public MockOrder(MockUser user, LatLng begin, LatLng end, MockCar car) {
        if (user instanceof MockPassenger) {
            mPassenger = (MockPassenger) user;
            mPassengerId = user.getId();
            isDriverOrder = false;
        } else if (user instanceof MockDriver) {
            mDriver = (MockDriver) user;
            mDriverId = user.getId();
            isDriverOrder = true;
        }
        mCar = car;
        if (begin != null && !begin.equals(end)) {
            mBegin = begin;
            mEnd = end;
            mId = "mc-order-" + System.currentTimeMillis();
        }
    }

    public String getId() {
        if (isDriverOrder && !TextUtils.isEmpty(mCarpoolOrderId)) {
            return mCarpoolOrderId;
        }
        return mId;
    }

    /**
     * 原始创建ID
     */
    public String getOriginalId() {
        return mId;
    }

    public void setId(String orderId) {
        this.mId = orderId;
    }

    /**
     * 拼车ID
     */
    public String getCarpoolOrderId() {
        return mCarpoolOrderId;
    }

    public void setCarpoolOrderId(String carpoolOrderId) {
        mCarpoolOrderId = carpoolOrderId;
    }

    public boolean isDriverOrder() {
        return isDriverOrder;
    }

    public String getUserId() {
        if (isDriverOrder) {
            return getDriverId();
        } else {
            return getPassengerId();
        }
    }

    public String getPassengerId() {
        if (mPassenger != null) {
            return mPassenger.getId();
        }
        return mPassengerId;
    }

    public String getDriverId() {
        if (mDriver != null) {
            return mDriver.getId();
        }
        return mDriverId;
    }

    public MockPassenger getPassenger() {
        return mPassenger;
    }

    public void setPassenger(MockPassenger passenger) {
        mPassenger = passenger;
    }

    public MockDriver getDriver() {
        return mDriver;
    }

    public void setDriver(MockDriver driver) {
        mDriver = driver;
    }

    public MockCar getCar() {
        return mCar;
    }

    public LatLng getBegin() {
        return mBegin;
    }

    public LatLng getEnd() {
        return mEnd;
    }

    public void setBegin(LatLng begin) {
        this.mBegin = begin;
    }

    public void setEnd(LatLng end) {
        this.mEnd = end;
    }

    public Status getStatus() {
        return mStatus;
    }

    public void setStatus(Status status) {
        this.mStatus = status;
    }

    /**
     * 等待接单
     */
    public boolean isWaiting() {
        return mStatus == Status.Waiting;
    }

    /**
     * 已派单
     */
    public boolean isAccepted() {
        return mStatus == Status.Accepted;
    }

    /**
     * 送驾中
     */
    public boolean isOnTheWay() {
        return mStatus == Status.OnTheWay;
    }

    /**
     * 订单完成
     */
    public boolean isFinished() {
        return mStatus == Status.Finished;
    }

    /**
     * 订单撤销
     */
    public boolean isCanceled() {
        return mStatus == Status.Canceled;
    }

    public void addSubOrder(MockOrder order) {
        subOrders.add(order);
    }

    public Set<MockOrder> getSubOrders() {
        return subOrders;
    }

    public enum Status {
        Idle(0),
        Waiting(1),
        Accepted(2),
        OnTheWay(3),
        Canceled(4),
        Finished(6),
        ;

        private final int status;

        Status(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
