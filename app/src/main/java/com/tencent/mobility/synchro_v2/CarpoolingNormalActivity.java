package com.tencent.mobility.synchro_v2;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBOrderType;
import com.tencent.map.lssupport.bean.TLSBWayPointType;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.ui.OneDriverNPassengerActivity;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.Configs;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.navix.api.layer.NavigatorLayerRootDrive;
import com.tencent.navix.api.model.NavDriveRoute;
import com.tencent.navix.api.model.NavError;
import com.tencent.navix.api.model.NavRoutePlan;
import com.tencent.navix.api.navigator.NavigatorDrive;
import com.tencent.navix.api.plan.DriveRoutePlanOptions;
import com.tencent.tencentmap.mapsdk.maps.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class CarpoolingNormalActivity extends OneDriverNPassengerActivity {

    public static final String ACTION_CARPOOLING_ORDER_CREATE = "拼车-订单创建";
    public static final String ACTION_CARPOOLING_ORDER_MERGE = "拼车-订单撮合";
    public static final String ACTION_CARPOOLING_ROUTE_PLAN = "拼车-路线规划";

    @Override
    protected int getPassengerSize() {
        return 3;
    }

    @Override
    protected String[] getPassengerActions(int passengerNo) {
        switch (passengerNo) {
            case 0:
            case 1:
            case 2:
                return new String[]{
                        ACTION_CARPOOLING_ORDER_CREATE,
                        ACTION_SYNC_OPEN,
                        ACTION_PULL,
                        ACTION_ROUTES_DRAW,
                        ACTION_SYNC_CLOSE,
                        ACTION_ARRIVED_GETON,
                        ACTION_ARRIVED_GETOFF,
                        ACTION_ORDER_STATUS_SYNC
                };
        }
        return new String[0];
    }

    @Override
    protected String[] getPassengerActionIndexes(int passengerNo) {
        return new String[0];
    }

    @Override
    protected String[] getDriverActions() {
        return new String[]{
                ACTION_CARPOOLING_ORDER_CREATE,
                ACTION_SYNC_OPEN,
                ACTION_CARPOOLING_ORDER_MERGE,
                ACTION_ORDER_TO_TRIP,
                ACTION_ORDER_TO_PICKUP,
                ACTION_CARPOOLING_ROUTE_PLAN,
                ACTION_ROUTES_DRAW,
                ACTION_ROUTES_UPLOAD,
                ACTION_PULL,
                ACTION_SYNC_CLOSE,
                ACTION_NAVI_SIMULATOR_OPEN,
                ACTION_NAVI_SIMULATOR_CLOSE,
                ACTION_UPLOAD_POSITION,
                ACTION_ORDER_STATUS_SYNC
        };
    }

    @Override
    protected void onCreatePassengerAction(int number, MockPassenger passenger, TSLPassengerManager passengerSync,
                                           PanelView passengerPanel, NavigatorLayerRootDrive mapView) {
        passenger.setCarType(MockCar.CarType.All);
        passenger.setBizType(MockCar.BizType.CarpoolPassenger);
        passengerPanel.addAction(ACTION_CARPOOLING_ORDER_CREATE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                passenger.setPosition(MockSyncService.getRandomVisibleLatLng(mapView.getMapApi().getProjection()));
                MockOrder order = mPassengerInfo.mMockSyncService.newOrder(mapView.getMapApi(), passenger);
//                order.setBegin(new LatLng(39.98860718554204, 116.3171375559992));
//                order.setEnd(new LatLng(39.96960996954201, 116.32867682082374));
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    passengerPanel.print("创建订单：" + order.getId());
                    passengerSync.getTLSPOrder().setOrderId(order.getId())
                            .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    protected void onCreateDriverAction(MockDriver driver, TSLDExtendManager driverSync,
                                        PanelView driverPanel, NavigatorLayerRootDrive carNaviView,
                                        NavigatorDrive manager) {
        driver.setCarType(MockCar.CarType.All);
        driver.setBizType(MockCar.BizType.CarpoolDriver);

        driverPanel.addAction(ACTION_CARPOOLING_ORDER_CREATE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                driver.setPosition(MockSyncService.getRandomVisibleLatLng(carNaviView.getMapApi().getProjection()));
                MockOrder order = mDriverInfo.mMockSyncService
                        .newCarpoolOrder(carNaviView.getMapApi(), driver,
                                getPassenger());
//                order.setBegin(new LatLng(39.98385673303598, 116.30641977425192));
//                order.setEnd(new LatLng(39.96960996954201, 116.32867682082374));
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    driverPanel.print("创建订单：" + order.getId());
                    driverSync.getTLSBOrder().setOrderId(order.getId())
                            .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                    return true;
                }
                return super.run();
            }
        });

        driverPanel.addAction(ACTION_CARPOOLING_ORDER_MERGE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder carpoolOrder = mDriverInfo.mMockSyncService.acceptPassengers(driver, getPassengers());
                if (carpoolOrder != null && carpoolOrder.isAccepted()) {
                    List<MockPassenger> passengers = getPassengers();
                    for (MockPassenger passenger : passengers) {
                        MockOrder pOrder = mDriverInfo.mMockSyncService.getOrder(passenger);
                        pOrder.setCarpoolOrderId(carpoolOrder.getId());
                        PassengerInfo passengerInfo = getPassengerInfo(passenger);
                        passengerInfo.mPassengerSync.getOrderManager().editCurrent()
                                .setOrderId(carpoolOrder.getId())
                                .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                                .setSubOrderId(pOrder.getOriginalId());
                    }
                    return true;
                }
                return false;
            }
        });

        driverPanel.addAction(ACTION_CARPOOLING_ROUTE_PLAN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder order = mDriverInfo.mMockSyncService.getOrder(driver);
                if (order == null) {
                    return false;
                }
                List<TLSDWayPointInfo> ws = new ArrayList<>();
                Set<MockOrder> subOrders = order.getSubOrders();
                for (MockOrder subOrder : subOrders) {
                    ws.add(TLSDWayPointInfo.newBuilder()
                            .order(subOrder.getId())
                            .latLng(subOrder.getBegin())
                            .type(TLSBWayPointType.TLSDWayPointTypeGetIn)
                            .build());
                    ws.add(TLSDWayPointInfo.newBuilder()
                            .order(subOrder.getId())
                            .latLng(subOrder.getEnd())
                            .type(TLSBWayPointType.TLSDWayPointTypeGetOff)
                            .build());
                }
                final CountDownLatch sortedWaiting = new CountDownLatch(1);
                driverSync.requestBestSortedWayPoints(
                        ConvertUtils.toNaviPoi(order.getBegin()),
                        ws,
                        new DriDataListener.ISortedWayPointsCallBack() {
                            @Override
                            public void onSortedWaysSuc(List<TLSDWayPointInfo> sortedWays) {
                                ws.clear();
                                ws.addAll(sortedWays);
                                sortedWaiting.countDown();
                            }

                            @Override
                            public void onSortedWayFail(int errCode, String errMsg) {
                                sortedWaiting.countDown();
                            }
                        }
                );

                try {
                    sortedWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<NavDriveRoute> routeData = new ArrayList<>();
                driverSync.searchCarRoutes(order.getId(),
                        ConvertUtils.toNaviPoi(order.getBegin()),
                        ws,
                        DriveRoutePlanOptions.Companion.newBuilder().build(),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onResultCallback(NavRoutePlan navRoutePlan, NavError navError) {
                                if (navRoutePlan != null) {
                                    List<NavDriveRoute> arrayList = navRoutePlan.getRoutes();
                                    if (arrayList != null && !arrayList.isEmpty()) {
                                        routeData.addAll(arrayList);
                                    }
                                }
                                syncWaiting.countDown();
                            }

                            @Override
                            public void onInternalError(int errCode, String errMsg) {
                                syncWaiting.countDown();
                            }
                        }
                );

                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                driverPanel.print("路线总数：" + routeData.size());

                if (!routeData.isEmpty()) {
                    //送驾路线，默认选择0路线
                    driverSync.getRouteManager().useRouteIndex(0);
                }

                if (TextUtils.isEmpty(driverSync.getRouteManager().getRouteId())) {
                    driverPanel.postPrint("没有命中乘客选择的路线");
                } else {
                    driverPanel.print("当前线路：" + driverSync.getRouteManager().getRouteId());
                }

                driverSync.addRemoveWayPointCallBack(wayPoints -> {
                    // 剔除途经点的回调
                    Log.e(Configs.TAG, ">>>onRemoveWayPoint !!");
                    // app->停止导航，重新算路，开始导航
                    manager.stopNavigation();
                    // from:当前司机起点,这里测试就写死了
                    // 开始算路
                    driverSync.searchCarRoutes(order.getId(), ConvertUtils.toNaviPoi(order.getBegin()),
                            wayPoints, DriveRoutePlanOptions.Companion.newBuilder().build(),
                            new DriDataListener.ISearchCallBack() {
                                @Override
                                public void onResultCallback(NavRoutePlan navRoutePlan, NavError navError) {
                                    if (navRoutePlan != null) {
                                        Log.d(Configs.TAG, "onRouteSearchSuccess:" +
                                                ((NavDriveRoute) (navRoutePlan.getRoutes().get(0))).getWaypoints().size());
                                        try {
                                            manager.simulator().setEnable(true);
                                            manager.startNavigation(driverSync.getRouteManager().getRouteId());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void onInternalError(int errCode, String errMsg) {
                                    Log.d(Configs.TAG, "onParamsInvalid:" + errCode + " " + errMsg);
                                }
                            }
                    );
                });

                return routeData.size() >= 1;
            }
        });
    }
}
