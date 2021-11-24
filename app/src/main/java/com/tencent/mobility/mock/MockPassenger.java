package com.tencent.mobility.mock;

import java.util.concurrent.atomic.AtomicLong;

public class MockPassenger extends MockUser {

    private static final AtomicLong mPassengerId = new AtomicLong(1);

    public MockPassenger() {
        super("mock-android-passenger-" + (System.currentTimeMillis() / 1000 / 60 / 60 / 24)
                + "-" + mPassengerId.getAndIncrement());
    }
}
