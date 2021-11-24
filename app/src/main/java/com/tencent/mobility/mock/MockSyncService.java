package com.tencent.mobility.mock;

import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.protocol.AppServer;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.VisibleRegion;

import java.util.ArrayList;
import java.util.List;

public class MockSyncService {

    private static final LatLng southWestOfChina = new LatLng(28.767659, 91.582031);
    private static final LatLng northEastOfChina = new LatLng(42.098222, 118.037109);
    private static final List<MockOrder> mOrders = new ArrayList<>();

    private final AppServer mAppServer;

    public MockSyncService(BaseSyncProtocol delegate) {
        mAppServer = new AppServer(delegate);
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
     * 创建快车车辆
     *
     * @return
     */
    public static MockCar newFastCar() {
        return new MockCar(MockCar.CarType.All, MockCar.BizType.RealTime);
    }

    /**
     * 创建顺风车司机车辆
     *
     * @return 车辆
     */
    public static MockCar newHitchHikeCar() {
        return new MockCar(MockCar.CarType.All, MockCar.BizType.HitchHikeDriver);
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
     * 随机位置
     *
     * @return 中国范围内的坐标位置
     */
    public static LatLng getRandomLatLng() {
        double lat = southWestOfChina.latitude
                + (northEastOfChina.latitude - southWestOfChina.latitude) * Math.random();
        double lng = southWestOfChina.longitude
                + (northEastOfChina.longitude - southWestOfChina.longitude) * Math.random();
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
            double lat = nearLeft.latitude + (farRight.latitude - nearLeft.latitude) * Math
                    .random();
            double lng = nearLeft.longitude + (farRight.longitude - nearLeft.longitude) * Math
                    .random();
            return new LatLng(lat, lng);
        } else {
            return null;
        }
    }

    /**
     * 地图可见区域中创建订单
     *
     * @param map 地图对象
     * @param user 乘客
     * @return 订单
     */
    public MockOrder newOrder(TencentMap map, MockUser user) {
        LatLng start = user.getStart() == null ? getRandomVisibleLatLng(map) : user.getStart();
        LatLng end = user.getEnd() == null ? getRandomVisibleLatLng(map) : user.getEnd();
        MockOrder oldOrder = getOrder(user);
        if (oldOrder != null) {
            mOrders.remove(oldOrder);
        }
        MockOrder mockOrder = new MockOrder(user, start, end, user.getCar());
        if (created(mockOrder, user)) {
            mOrders.add(mockOrder);
        }

        return mockOrder;
    }

    /**
     * 创建司机拼车订单
     *
     * @param map
     * @param driver
     * @return
     */
    public MockOrder newCarpoolOrder(TencentMap map, MockDriver driver, MockPassenger passenger) {
        LatLng start = driver.getStart() == null ? getRandomVisibleLatLng(map) : driver.getStart();
        LatLng end = driver.getEnd() == null ? getRandomVisibleLatLng(map) : driver.getEnd();
        MockOrder oldOrder = getOrder(driver);
        if (oldOrder != null) {
            mOrders.remove(oldOrder);
        }
        MockOrder mockOrder = new MockOrder(driver, start, end, driver.getCar());
        mockOrder.setPassenger(passenger);
        MockOrder passengerOrder = getOrder(passenger);
        if (acceptedCarpool(mockOrder, passengerOrder)) {
            mOrders.add(mockOrder);
            driver.setOrderId(mockOrder.getId());
        }

        return mockOrder;
    }

    /**
     * 司机接多个乘客
     *
     * @param driver 司机
     * @param passengers 多个乘客
     * @return 司机订单
     */
    public MockOrder acceptPassengers(MockDriver driver, List<MockPassenger> passengers) {
        MockOrder driverOrder = getOrder(driver);

        accepted(driverOrder);
        for (MockPassenger passenger : passengers) {
            MockOrder passengerOrder = getOrder(passenger);
            driverOrder.addSubOrder(passengerOrder);
            passengerOrder.setDriver(driver);
            accepted(passengerOrder);
        }

        return driverOrder;
    }

    /**
     * 司机接驾乘客
     *
     * @param driver 司机
     * @param passenger 乘客
     * @return 签约订单
     */
    public MockOrder acceptPassenger(MockDriver driver, MockPassenger passenger) {
        MockOrder driverOrder = getOrder(driver);
        MockOrder passengerOrder = getOrder(passenger);
        if (driverOrder != null) {
            driverOrder.setPassenger(passenger);
            accepted(driverOrder);

            if (passengerOrder != null) {
                driverOrder.addSubOrder(passengerOrder);
                passengerOrder.setDriver(driver);
                accepted(passengerOrder);
            }

            return driverOrder;
        } else {
            if (passengerOrder != null) {
                passengerOrder.setDriver(driver);
                accepted(passengerOrder);
            }
            return passengerOrder;
        }
    }

    /**
     * 司机送多个乘客
     *
     * @param driver 司机
     * @param passengers 多个乘客
     * @return 司机订单
     */
    public MockOrder onTheWayPassengers(MockDriver driver, List<MockPassenger> passengers) {
        MockOrder driverOrder = getOrder(driver);

        onTheWay(driverOrder);
        for (MockPassenger passenger : passengers) {
            MockOrder passengerOrder = getOrder(passenger);
            driverOrder.addSubOrder(passengerOrder);
            passengerOrder.setDriver(driver);
            onTheWay(passengerOrder);
        }

        return driverOrder;
    }

    /**
     * 司机送驾乘客
     *
     * @param driver 司机
     * @param passenger 乘客
     * @return 签约订单
     */
    public MockOrder onTheWayPassenger(MockDriver driver, MockPassenger passenger) {
        MockOrder driverOrder = getOrder(driver);
        MockOrder passengerOrder = getOrder(passenger);
        if (driverOrder != null) {
            driverOrder.setPassenger(passenger);
            onTheWay(driverOrder);

            if (passengerOrder != null) {
                driverOrder.addSubOrder(passengerOrder);
                passengerOrder.setDriver(driver);
                onTheWay(passengerOrder);
            }

            return driverOrder;
        } else {
            if (passengerOrder != null) {
                passengerOrder.setDriver(driver);
                onTheWay(passengerOrder);
            }
            return passengerOrder;
        }
    }

    /**
     * 根据乘客找订单
     *
     * @param user 乘客
     * @return 订单
     */
    public MockOrder getOrder(MockUser user) {
        for (MockOrder order : mOrders) {
            String userId = order.getUserId();
            if (userId.equals(user.getId())) {
                return order;
            }
        }
        return null;
    }

    private boolean created(MockOrder order, MockUser user) {
        if (mAppServer.orderSync(order, MockOrder.Status.Idle)) {
            order.setStatus(MockOrder.Status.Idle);
            user.setOrderId(order.getId());
            return true;
        }

        return false;
    }

    private void accepted(MockOrder order) {
        if (mAppServer.orderSync(order, MockOrder.Status.Accepted)) {
            order.setStatus(MockOrder.Status.Accepted);
        }
    }

    private boolean acceptedCarpool(MockOrder driverOrder, MockOrder passengerOrder) {
        if (mAppServer.carpoolOrderSync(driverOrder, passengerOrder, MockOrder.Status.Accepted)) {
            driverOrder.setStatus(MockOrder.Status.Accepted);
            return true;
        }
        return false;
    }

    private void onTheWay(MockOrder order) {
        if (mAppServer.orderSync(order, MockOrder.Status.OnTheWay)) {
            order.setStatus(MockOrder.Status.OnTheWay);
        }
    }

    private void finish(MockOrder order) {
        if (mAppServer.orderSync(order, MockOrder.Status.Finished)) {
            order.setStatus(MockOrder.Status.Finished);
        }
    }

}
