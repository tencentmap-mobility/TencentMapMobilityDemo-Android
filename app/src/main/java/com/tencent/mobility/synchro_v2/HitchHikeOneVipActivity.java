package com.tencent.mobility.synchro_v2;

import android.text.TextUtils;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lssupport.bean.TLSBWayPointType;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.ui.OneDriverOnePassengerActivity;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.navix.api.layer.NavigatorLayerRootDrive;
import com.tencent.navix.api.model.NavDriveRoute;
import com.tencent.navix.api.model.NavError;
import com.tencent.navix.api.model.NavRoutePlan;
import com.tencent.navix.api.navigator.NavigatorDrive;
import com.tencent.navix.api.plan.DriveRoutePlanOptions;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class HitchHikeOneVipActivity extends OneDriverOnePassengerActivity {

    public static final String ACTION_HITCHHIKE_ORDER_MERGE = "顺风车-专享订单撮合";
    public static final String ACTION_HITCHHIKE_ROUTE_PLAN = "顺风车-路线规划";

    @Override
    protected String[] getPassengerActions() {
        return new String[]{
                ACTION_ORDER_CREATE,
                ACTION_SYNC_OPEN,
                ACTION_PULL,
                ACTION_ROUTES_DRAW,
                ACTION_SYNC_CLOSE,
        };
    }

    @Override
    protected String[] getDriverActions() {
        return new String[]{
                ACTION_ORDER_CREATE,
                ACTION_SYNC_OPEN,
                ACTION_HITCHHIKE_ORDER_MERGE,
                ACTION_ORDER_TO_TRIP,
                ACTION_ORDER_TO_PICKUP,
                ACTION_HITCHHIKE_ROUTE_PLAN,
                ACTION_ROUTES_DRAW,
                ACTION_ROUTES_UPLOAD,
                ACTION_PULL,
                ACTION_SYNC_CLOSE,
                ACTION_NAVI_OPEN,
                ACTION_NAVI_SIMULATOR_OPEN,
                ACTION_NAVI_SIMULATOR_CLOSE,
                ACTION_NAVI_CLOSE,

        };
    }

    @Override
    protected void onCreatePassengerAction(MockPassenger passenger, TSLPassengerManager passengerSync,
                                           PanelView passengerPanel, NavigatorLayerRootDrive mapView) {

        passenger.setStart(new LatLng(40.042879, 116.270723));
        passenger.setEnd(new LatLng(40.098958, 116.27824));

    }

    @Override
    protected void onCreateDriverAction(MockDriver driver, TSLDExtendManager driverSync,
                                        PanelView driverPanel, NavigatorLayerRootDrive carNaviView,
                                        NavigatorDrive manager) {
        driver.setCarType(MockCar.CarType.All);
        driver.setBizType(MockCar.BizType.HitchHikeDriver);

        driver.setStart(new LatLng(40.002229, 116.323806));
        driver.setEnd(new LatLng(40.103269, 116.269314));

        driverPanel.addAction(ACTION_HITCHHIKE_ORDER_MERGE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mDriverInfo.mMockSyncService.acceptPassenger(driver, getPassenger());
                return order != null && order.isAccepted();
            }
        });

        driverPanel.addAction(ACTION_HITCHHIKE_ROUTE_PLAN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder order = mDriverInfo.mMockSyncService.getOrder(driver);
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

                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<NavDriveRoute> routeData = new ArrayList<>();
                driverSync.searchCarRoutes(order.getId(),
                        ConvertUtils.toNaviPoi(order.getBegin()),
                        ConvertUtils.toNaviPoi(order.getEnd()),
                        ws,
                        DriveRoutePlanOptions.Companion.newBuilder().build(),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onResultCallback(NavRoutePlan navRoutePlan, NavError navError) {
                                if (navRoutePlan != null ) {
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

                if (driverSync.getOrderManager().getUsingOrder().isTripStatus() && !routeData.isEmpty()) {
                    //送驾路线，默认选择0路线
                    driverSync.getRouteManager().useRouteIndex(0);
                }

                if (TextUtils.isEmpty(driverSync.getRouteManager().getRouteId())) {
                    driverPanel.postPrint("没有命中乘客选择的路线");
                } else {
                    driverPanel.print("当前线路：" + driverSync.getRouteManager().getRouteId());
                }

                return routeData.size() >= 1;
            }
        });
    }
}
