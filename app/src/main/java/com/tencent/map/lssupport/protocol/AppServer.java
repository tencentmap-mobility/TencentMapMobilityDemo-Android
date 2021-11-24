package com.tencent.map.lssupport.protocol;

import android.text.TextUtils;

import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AppServer {

    private final BaseSyncProtocol mSyncServerDelegate;

    public AppServer(BaseSyncProtocol syncServerDelegate) {mSyncServerDelegate = syncServerDelegate;}

    public boolean orderSync(MockOrder order, MockOrder.Status targetStatus) {
        MockDriver driver = order.getDriver();
        MockPassenger passenger = order.getPassenger();
        MockCar car = order.getCar();
        final CountDownLatch latch = new CountDownLatch(1);
        final Object[] result = new Object[2];
        TLSLatlng passengerPos = null;
        String passengerId = "";
        TLSLatlng driverPos = null;
        String driverId = "";
        if (passenger != null) {
            passengerId = passenger.getId();
            passengerPos = ConvertUtil.toTLSLatLng(passenger.getPosition());
        }
        if (driver != null) {
            driverId = driver.getId();
            driverPos = ConvertUtil.toTLSLatLng(driver.getPosition());
        }

        if (passengerPos == null && driverPos != null) {
            passengerPos = driverPos;
        } else if (passengerPos != null && driverPos == null) {
            driverPos = passengerPos;
        }


        mSyncServerDelegate.orderStatusSync(
                order.getId(),
                passengerId,
                passengerPos,
                driverId,
                driverPos,
                ConvertUtil.toTLSLatLng(order.getBegin()),
                ConvertUtil.toTLSLatLng(order.getEnd()),
                targetStatus.getStatus(),
                car.getCarType(),
                car.getBizType(),
                new SyncProtocol.OrderResultListener() {
                    /**
                     * 订单结果
                     *
                     * @param status  状态
                     * @param message 消息
                     * @since 2.2
                     */
                    @Override
                    public void onResult(int status, String message) {
                        result[0] = status;
                        result[1] = message;
                        latch.countDown();
                    }
                });

        try {
            latch.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (result[0] != null) {
            return (int) result[0] == 0;
        }

        return false;
    }

    public boolean carpoolOrderSync(final MockOrder driverOrder, MockOrder passengerOrder,
                                     MockOrder.Status targetStatus) {
        MockDriver driver = driverOrder.getDriver();
        MockPassenger passenger = passengerOrder.getPassenger();
        MockCar car = driverOrder.getCar();
        final CountDownLatch latch = new CountDownLatch(1);
        final Object[] result = new Object[2];
        TLSLatlng passengerPos = null;
        String passengerId = "";
        TLSLatlng driverPos = null;
        String driverId = "";
        if (passenger != null) {
            passengerId = passenger.getId();
            passengerPos = ConvertUtil.toTLSLatLng(passenger.getPosition());
        }
        if (driver != null) {
            driverId = driver.getId();
            driverPos = ConvertUtil.toTLSLatLng(driver.getPosition());
        }

        if (passengerPos == null && driverPos != null) {
            passengerPos = driverPos;
        } else if (passengerPos != null && driverPos == null) {
            driverPos = passengerPos;
        }

        mSyncServerDelegate.carpoolOrderStatusSync(
                driverOrder.getId(),
                passengerOrder.getId(),
                passengerId,
                passengerPos,
                driverId,
                driverPos,
                ConvertUtil.toTLSLatLng(driverOrder.getBegin()),
                ConvertUtil.toTLSLatLng(driverOrder.getEnd()),
                targetStatus.getStatus(),
                car.getCarType(),
                car.getBizType(),
                new SyncProtocol.OrderResultListener() {
                    @Override
                    public void onResult(int status, String message) {
                        result[0] = status;
                        result[1] = message;
                        latch.countDown();

                        if (status == 0 && !TextUtils.isEmpty(message)) {

                            try {
                                JSONObject data = new JSONObject(message);
                                String driverOrderId = data.optString("driver_orderid");
                                if (!TextUtils.isEmpty(driverOrderId)) {
                                    driverOrder.setCarpoolOrderId(driverOrderId);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        try {
            latch.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return (int) result[0] == 0;
    }
}
