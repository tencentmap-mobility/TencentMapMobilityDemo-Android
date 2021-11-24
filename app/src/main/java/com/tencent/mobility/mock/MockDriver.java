package com.tencent.mobility.mock;

import java.util.concurrent.atomic.AtomicLong;

public class MockDriver extends MockUser {

    private static final AtomicLong mDriverId = new AtomicLong(1);

    public MockDriver(MockCar car) {
        super("mock-android-driver-" + (System.currentTimeMillis() / 1000 / 60 / 60 / 24)
                + "-" + mDriverId.getAndIncrement(), car);
    }
}
