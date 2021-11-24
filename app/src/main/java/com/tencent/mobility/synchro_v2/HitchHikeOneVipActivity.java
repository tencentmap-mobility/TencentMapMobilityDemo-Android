package com.tencent.mobility.synchro_v2;

import android.text.TextUtils;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.protocol.OrderRouteSearchOptions;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lssupport.bean.TLSBWayPointType;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.map.navi.car.CarNaviView;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.map.navi.data.RouteData;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.ui.OneDriverOnePassengerActivity;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.tencentmap.mapsdk.maps.MapView;
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
                ACTION_SYNC_OPEN
        };
    }

    @Override
    protected String[] getPassengerActionIndexes() {
        return new String[]{
                "2",
                "3"
        };
    }

    @Override
    protected String[] getDriverActions() {
        return new String[]{
                ACTION_ORDER_CREATE,
                ACTION_HITCHHIKE_ORDER_MERGE,
                ACTION_HITCHHIKE_ROUTE_PLAN,
                ACTION_ROUTES_DRAW,
                ACTION_ROUTES_UPLOAD,
                ACTION_SYNC_OPEN,
                ACTION_NAVI_SIMULATOR_OPEN,
                ACTION_NAVI_SIMULATOR_CLOSE
        };
    }

    @Override
    protected String[] getDriverActionIndexes() {
        return new String[]{
                "1",
                "4",
                "5",
                "6",
                "7",
                "8",
                "9",
                "退出"
        };
    }

    @Override
    protected void onCreatePassengerAction(MockPassenger passenger, TSLPassengerManager passengerSync,
            PanelView passengerPanel, MapView mapView) {

        //设置乘客固定起终点
        passenger.setStart(new LatLng(40.042879, 116.270723));
        passenger.setEnd(new LatLng(40.098958, 116.27824));

    }

    @Override
    protected void onCreateDriverAction(MockDriver driver, TSLDExtendManager driverSync,
            PanelView driverPanel, CarNaviView carNaviView,
            TencentCarNaviManager manager) {
        //设置司机的类型
        driver.setCarType(MockCar.CarType.All);
        driver.setBizType(MockCar.BizType.HitchHikeDriver);

        //设置司机固定起终点
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
                final List<RouteData> routeData = new ArrayList<>();
                driverSync.searchCarRoutes(
                        ConvertUtils.toNaviPoi(order.getBegin()),
                        ConvertUtils.toNaviPoi(order.getEnd()),
                        ws,
                        OrderRouteSearchOptions
                                .create(order.getId())
                                .rejectWayPoint(ws.toArray(new TLSDWayPointInfo[0])),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onParamsInvalid(int errCode, String errMsg) {
                                syncWaiting.countDown();
                            }

                            @Override
                            public void onRouteSearchFailure(int i, String s) {
                                syncWaiting.countDown();
                            }

                            @Override
                            public void onRouteSearchSuccess(ArrayList<RouteData> arrayList) {
                                if (arrayList != null && !arrayList.isEmpty()) {
                                    routeData.addAll(arrayList);
                                }
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
