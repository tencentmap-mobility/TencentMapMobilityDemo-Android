package com.tencent.mobility.mock;

import androidx.annotation.IntDef;

public class MockCar {

    private int mCarType = CarType.All; //车类型：0：所有分类 1：出租车 2：新能源 3：舒适型 4：豪华型 5：商务型
    private int mBizType = BizType.RealTime; //业务类型：1：实时订单 2：顺风车---司机/服务器 3：顺风车---乘客 4：拼车----司机/服务器 5：拼车----乘客

    public MockCar() {
    }

    public MockCar(@CarType int carType, @BizType int bizType) {
        mCarType = carType;
        mBizType = bizType;
    }

    public int getCarType() {
        return mCarType;
    }

    public void setCarType(@CarType int carType) {
        mCarType = carType;
    }

    public int getBizType() {
        return mBizType;
    }

    public void setBizType(@BizType int bizType) {
        mBizType = bizType;
    }

    @IntDef({
            BizType.RealTime,
            BizType.HitchHikeDriver,
            BizType.HitchHikePassenger,
            BizType.CarpoolDriver,
            BizType.CarpoolPassenger
    })
    public @interface BizType {

        int RealTime = 1;
        int HitchHikeDriver = 2;
        int HitchHikePassenger = 3;
        int CarpoolDriver = 4;
        int CarpoolPassenger = 5;
    }

    @IntDef({
            CarType.All,
            CarType.Taxi,
            CarType.Ev,
            CarType.Comfort,
            CarType.Deluxe,
            CarType.Business
    })
    public @interface CarType {

        int All = 0;
        int Taxi = 1;
        int Ev = 2;
        int Comfort = 3;
        int Deluxe = 4;
        int Business = 5;
    }
}
