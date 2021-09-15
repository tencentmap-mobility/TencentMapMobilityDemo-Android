package com.tencent.mobility.mock;

import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

public class MockOrder {

    private String mId;
    private final String mPassengerId;
    private final MockPassenger mPassenger;
    private MockDriver mDriver;
    private LatLng mBegin;
    private LatLng mEnd;
    private Status mStatus = Status.Idle;
    private final MockCar mCar;

    public MockOrder(MockPassenger passenger, LatLng begin, LatLng end, MockCar car) {
        mPassenger = passenger;
        mCar = car;
        mPassengerId = passenger.getId();
        if (!begin.equals(end)) {
            mBegin = begin;
            mEnd = end;
            mId = "mc-order-" + System.currentTimeMillis();
        }
    }

    public String getId() {
        return mId;
    }

    public String getPassengerId() {
        return mPassengerId;
    }

    public String getDriverId() {
        if (mDriver != null) {
            return mDriver.getId();
        }
        return "";
    }

    public void setDriver(MockDriver driver) {
        mDriver = driver;
    }

    public MockPassenger getPassenger() {
        return mPassenger;
    }

    public MockDriver getDriver() {
        return mDriver;
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

    public void setStatus(Status status) {
        this.mStatus = status;
    }

    public Status getStatus() {
        return mStatus;
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
