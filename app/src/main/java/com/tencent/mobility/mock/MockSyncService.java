package com.tencent.mobility.mock;

import android.net.Uri;
import android.util.Log;

import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.VisibleRegion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MockSyncService {

    private static final LatLng southWestOfChina = new LatLng(28.767659, 91.582031);
    private static final LatLng northEastOfChina = new LatLng(42.098222, 118.037109);
    private final List<MockOrder> mOrders = new ArrayList<>();

    private final String mSyncKey;
    private final boolean mIsTestEnv;

    public MockSyncService(String syncKey, boolean testEnv) {
        mSyncKey = syncKey;
        mIsTestEnv = testEnv;
    }

    /**
     * 创建随机位置的乘客
     *
     * @return 乘客
     */
    public static MockPassenger newRandomPassenger(TencentMap map) {
        MockPassenger passenger = new MockPassenger();
        passenger.setPosition(getRandomVisibleLatLng(map));
        return passenger;
    }

    /**
     * 创建随机类型车辆
     *
     * @return 车辆
     */
    public static MockCar newRandomCar() {
        return new MockCar();
    }

    /**
     * 创建随机位置的司机
     *
     * @param car 车
     * @return 司机
     */
    public static MockDriver newRandomDriver(TencentMap map, MockCar car) {
        MockDriver driver = new MockDriver(car);
        driver.setPosition(getRandomVisibleLatLng(map));
        return driver;
    }

    /**
     * 地图可见区域中创建订单
     *
     * @param map       地图对象
     * @param passenger 乘客
     * @return 订单
     */
    public MockOrder newOrder(TencentMap map, MockPassenger passenger) {
        LatLng start = getRandomVisibleLatLng(map);
        LatLng end = getRandomVisibleLatLng(map);
        MockOrder oldOrder = getOrder(passenger);
        if (oldOrder != null) {
            mOrders.remove(oldOrder);
        }
        MockOrder mockOrder = new MockOrder(passenger, start, end, newRandomCar());
        if(created(mockOrder, passenger)){
            mOrders.add(mockOrder);
        }

        return mockOrder;
    }

    /**
     * 司机接驾乘客
     *
     * @param driver    司机
     * @param passenger 乘客
     * @return 签约订单
     */
    public MockOrder acceptPassenger(MockDriver driver, MockPassenger passenger) {
        MockOrder order = getOrder(passenger);
        if (order != null) {
            accepted(order, driver);
        }
        return order;
    }

    /**
     * 司机送驾乘客
     *
     * @param driver    司机
     * @param passenger 乘客
     * @return 签约订单
     */
    public MockOrder onTheWayPassenger(MockDriver driver, MockPassenger passenger) {
        MockOrder order = getOrder(passenger);
        if (order != null) {
            onTheWay(order, driver);
        }
        return order;
    }

    /**
     * 根据乘客找订单
     *
     * @param passenger 乘客
     * @return 订单
     */
    public MockOrder getOrder(MockPassenger passenger) {
        for (MockOrder order : mOrders) {
            String passengerId = order.getPassengerId();
            if (passengerId.equals(passenger.getId())) {
                return order;
            }
        }
        return null;
    }

    /**
     * 随机位置
     *
     * @return 中国范围内的坐标位置
     */
    public static LatLng getRandomLatLng() {
        double lat = southWestOfChina.latitude + (northEastOfChina.latitude - southWestOfChina.latitude) * Math.random();
        double lng = southWestOfChina.longitude + (northEastOfChina.longitude - southWestOfChina.longitude) * Math.random();
        return new LatLng(lat, lng);
    }

    /**
     * 在地图可视区域内随机位置
     *
     * @param map 地图对象
     * @return 坐标位置
     */
    public static LatLng getRandomVisibleLatLng(TencentMap map) {
        if (map != null) {
            VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();
            LatLng nearLeft = visibleRegion.nearLeft;
            LatLng farRight = visibleRegion.farRight;
            double lat = nearLeft.latitude + (farRight.latitude - nearLeft.latitude) * Math.random();
            double lng = nearLeft.longitude + (farRight.longitude - nearLeft.longitude) * Math.random();
            return new LatLng(lat, lng);
        } else {
            return null;
        }
    }

    private boolean created(MockOrder order, MockPassenger passenger) {
        if(orderSync(order, MockOrder.Status.Waiting)) {
            order.setStatus(MockOrder.Status.Waiting);
            passenger.setOrderId(order.getId());
            return true;
        }

        return false;
    }

    private void accepted(MockOrder order, MockDriver driver) {
        order.setDriver(driver);
        if(orderSync(order, MockOrder.Status.Accepted)) {
            order.setStatus(MockOrder.Status.Accepted);
        }
    }

    private void onTheWay(MockOrder order, MockDriver driver) {
        order.setDriver(driver);
        if(orderSync(order, MockOrder.Status.OnTheWay)) {
            order.setStatus(MockOrder.Status.OnTheWay);
        }
    }

    private void finish(MockOrder order, MockDriver driver) {
        order.setDriver(driver);
        if(orderSync(order, MockOrder.Status.Finished)) {
            order.setStatus(MockOrder.Status.Finished);
        }
    }

    private boolean orderSync(MockOrder order, MockOrder.Status targetStatus) {
        MockDriver driver = order.getDriver();
        MockCar car = order.getCar();
        MockPassenger passenger = order.getPassenger();
        OkHttpClient client = new OkHttpClient();
        JSONObject postBody = new JSONObject();
        try {
            postBody.put("reqid", UUID.randomUUID().toString());
            postBody.put("reqtime", System.currentTimeMillis() / 1000);
            postBody.put("uptime", System.currentTimeMillis() / 1000);
            postBody.put("orderid", order.getId());
            postBody.put("driverid", order.getDriverId());
            postBody.put("driverdev", "dec1");
            if (driver != null) {
                postBody.put("driver_lnglat", toLngLatString(driver.getPosition()));
            } else {
                postBody.put("driver_lnglat", toLngLatString(passenger.getPosition()));
            }
            postBody.put("userid", order.getPassengerId());
            postBody.put("userdev", "dec2");
            postBody.put("user_lnglat", toLngLatString(passenger.getPosition()));
            postBody.put("getin_lnglat", toLngLatString(order.getBegin()));
            postBody.put("getin_poiname", "起点");
            postBody.put("getoff_poiid", "123");
            postBody.put("getoff_poiname", "终点");
            postBody.put("getoff_lnglat", toLngLatString(order.getEnd()));
            postBody.put("business_type", 1);
            postBody.put("city", "110000");
            postBody.put("type", 1);
            postBody.put("status", targetStatus.getStatus());
            postBody.put("cartype", car.getType());

            if (targetStatus == MockOrder.Status.OnTheWay) {
                postBody.put("real_getin_lnglat", toLngLatString(order.getBegin()));
                postBody.put("real_getin_time", System.currentTimeMillis() / 1000);

            }
            if (mIsTestEnv) {
                postBody.put("apikey", mSyncKey);
            } else {
                postBody.put("key", mSyncKey);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int status = -1;
        String json = postBody.toString();
        try {
            Uri url = Uri.EMPTY.buildUpon()
                    .scheme("http")
                    .encodedAuthority(mIsTestEnv ? "triptrace.wsd.com/api/v1" : "apis.map.qq.com/ws/tls/v1")
                    .appendEncodedPath("order/sync").build();
            Log.d("ls_Mock", url.toString() + json);
            RequestBody body = RequestBody.Companion.create(json.getBytes());
            Response response = client.newCall(new Request.Builder()
                    .url(url.toString())
                    .post(body)
                    .header("Content-Type", "application/json")
                    .build()).execute();

            Log.d("ls_Mock", response.message());
            if (response.isSuccessful()) {
                String resp = Objects.requireNonNull(response.body()).string();
                Log.d("ls_Mock", resp);
                JSONObject respBody = new JSONObject(resp);
                status = respBody.optInt("status", -1);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return status == 0;
    }

    private String toLngLatString(LatLng latLng) {
        DecimalFormat df = new DecimalFormat("#.000000");
        return df.format(latLng.longitude) + "," + df.format(latLng.latitude);
    }
}
