package com.tencent.mobility.synchro_v2.driver;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lsdriver.protocol.OrderRouteSearchOptions;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.lsp.listener.SimplePsgDataListener;
import com.tencent.map.lssupport.bean.TLSBOrder;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBRoute;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSDFetchedData;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.map.navi.car.CarNaviView;
import com.tencent.map.navi.data.CalcRouteResult;
import com.tencent.map.navi.data.RouteData;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.ConvertHelper;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.mobility.util.MapUtils;
import com.tencent.mobility.util.SHelper;
import com.tencent.mobility.util.SingleHelper;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 接力单功能
 */
public class DriverRelayOrderActivity extends BaseActivity {

    private CarNaviView mCarNaviView;
    private MapView mMapView1;
    private MapView mMapView2;

    private PanelView mDriverPanel;
    private PanelView mPassengerAPanel;
    private PanelView mPassengerBPanel;

    private TSLDExtendManager mDriverSync;
    private TSLPassengerManager mPassengerSyncA;
    private TSLPassengerManager mPassengerSyncB;

    private MockDriver mDriver;
    private MockPassenger mPassengerA;
    private MockPassenger mPassengerB;

    private MockSyncService mDriverSyncService;
    private MockSyncService mPassengerSyncService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relay_layout);

        mCarNaviView = findViewById(R.id.navi_car_view);
        mMapView1 = findViewById(R.id.map_view1);
        mMapView2 = findViewById(R.id.map_view2);

        mDriverPanel = findViewById(R.id.group_panel_driver);
        mPassengerAPanel = findViewById(R.id.group_panel_passenger_a);
        mPassengerBPanel = findViewById(R.id.group_panel_passenger_b);

        initCurrentPassengerPanel();
        initDriverPanel();
        initRelayPassengerPanel();
    }

    private void initCurrentPassengerPanel() {
        mPassengerA = MockSyncService.newRandomPassenger(mMapView1.getMap());
        mPassengerSyncA = TSLPassengerManager.newInstance();
        mPassengerSyncA.init(DriverRelayOrderActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mPassengerA.getId()));
        mPassengerSyncService = new MockSyncService(mPassengerSyncA);
        mPassengerAPanel.init("当前乘客", "创建订单");
        mPassengerAPanel.postAction("创建订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                MockOrder order = mPassengerSyncService.newOrder(mMapView1.getMap(), mPassengerA);
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    mPassengerAPanel.postPrint("等待接驾");
                    mPassengerSyncA.getTLSPOrder().setOrderId(order.getId());
                    mDriverPanel.postAction("绑定订单");
                    return order.getId();
                }
                return "";
            }
        });

        mPassengerAPanel.addAction("开启同显", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerSyncA.addTLSPassengerListener(new SimplePsgDataListener() {
                    @Override
                    public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
                        if (fetchedData != null) {
                            boolean ret = drawRoutes(mMapView1.getMap(), mPassengerSyncA);
                            mPassengerAPanel.print("绘制路线:" + ret);
                            if (ret) {
                                mPassengerSyncA.removeTLSPassengerListener(this);
                            }
                        }
                    }

                    @Override
                    public void onPullLsInfoFail(int errCode, String errMsg) {
                        mPassengerAPanel.postPrint("拉取失败：" + errCode);
                    }
                });

                mPassengerSyncA.start();
                return true;
            }
        });
    }

    private void initRelayPassengerPanel() {
        mPassengerB = MockSyncService.newRandomPassenger(mMapView2.getMap());
        mPassengerSyncB = TSLPassengerManager.newInstance();
        mPassengerSyncB.init(DriverRelayOrderActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mPassengerB.getId()));
        mPassengerBPanel.init("接力单乘客", "创建订单");
        mPassengerBPanel.postAction("创建订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                MockOrder order = mPassengerSyncService.newOrder(mMapView2.getMap(), mPassengerB);
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    mPassengerBPanel.postPrint("等待接驾");
                    mPassengerSyncB.getOrderManager().editCurrent().setRelay(true).setOrderId(order.getId());
                    return order.getId();
                }

                return "";
            }
        });

        mPassengerBPanel.addAction("开启同显", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerSyncB.addTLSPassengerListener(new SimplePsgDataListener() {
                    @Override
                    public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
                        if (fetchedData != null && mPassengerSyncB.getOrderManager().getOrderStatus() == 2) {
                            boolean ret = drawRoutes(mMapView2.getMap(), mPassengerSyncB);
                            mPassengerBPanel.print("绘制路线:" + ret);
                            if (ret) {
                                mPassengerSyncB.removeTLSPassengerListener(this);
                            }
                        }
                    }

                    @Override
                    public void onPullLsInfoFail(int errCode, String errMsg) {
                        mPassengerBPanel.postPrint("拉取失败：" + errCode);
                    }
                });

                mPassengerSyncB.start();
                return true;
            }
        });
    }

    private void initDriverPanel() {
        MockCar car = MockSyncService.newRandomCar();
        mDriver = MockSyncService.newRandomDriver(mCarNaviView.getMap(), car);
        mDriverSync = TSLDExtendManager.newInstance();
        mDriverSyncService = new MockSyncService(mDriverSync);
        mDriverSync.init(DriverRelayOrderActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mDriver.getId()));
        mDriverSync.setNaviManager(SingleHelper.getNaviManager(this));
        mDriverSync.setCarNaviView(mCarNaviView);
        mCarNaviView.setNavigationPanelVisible(false);
        mDriverPanel.init("司机", "绑定订单", "绑定接力订单", "乘客送达");

        mDriverPanel.addAction("绑定订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                MockOrder order = mDriverSyncService.getOrder(mPassengerA);
                order.setStatus(MockOrder.Status.Waiting);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                if (order.isWaiting()) {
                    mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    mPassengerSyncA.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    order = mDriverSyncService.acceptPassenger(mDriver, mPassengerA);
                    if (order.isAccepted()) {
                        mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mPassengerSyncA.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mDriverPanel.print("当前订单接驾中");
                        order = mDriverSyncService.onTheWayPassenger(mDriver, mPassengerA);
                        if (order.isOnTheWay()) {
                            mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                            mPassengerSyncA.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                            mDriverPanel.print("当前订单送驾中");
                            mDriverPanel.postAction("开启同显");
                            mDriverPanel.postAction("绑定接力订单");
                            return order.getId();
                        } else {
                            mDriverPanel.print("当前订单送驾失败");
                        }
                    } else {
                        mDriverPanel.print("当前订单接驾失败");
                    }
                }
                return order.getId();
            }
        });

        mDriverPanel.addAction("开启同显", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.start();
                return true;
            }
        });

        mDriverPanel.addAction("请求路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder orderA = mDriverSyncService.getOrder(mPassengerA);
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<RouteData> routeData = new ArrayList<>();

                // 当前送驾路线
                mDriverSync.searchCarRoutes(
                        ConvertUtils.toNaviPoi(orderA.getBegin()),
                        ConvertUtils.toNaviPoi(orderA.getEnd()),
                        new ArrayList<>(),
                        OrderRouteSearchOptions.create(orderA.getId()),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onParamsInvalid(int errCode, String errMsg) {
                                syncWaiting.countDown();
                            }

                            @Override
                            public void onCalcRouteSuccess(CalcRouteResult calcRouteResult) {
                                List<RouteData> arrayList = calcRouteResult.getRoutes();
                                if (arrayList != null && !arrayList.isEmpty()) {
                                    routeData.add(arrayList.get(0));
                                }
                                syncWaiting.countDown();
                            }

                            @Override
                            public void onCalcRouteFailure(CalcRouteResult calcRouteResult) {
                                syncWaiting.countDown();
                            }
                        }
                );

                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mDriverSync.getRouteManager().useRouteIndex(0);
                mDriverPanel.print("当前线路：" + routeData.get(0).getRouteId());

                final MockOrder orderB = mDriverSyncService.getOrder(mPassengerB);
                final CountDownLatch syncWaiting2 = new CountDownLatch(1);
                // 接力单路线
                mDriverSync.searchCarRoutes(
                        ConvertUtils.toNaviPoi(orderA.getEnd()),
                        ConvertUtils.toNaviPoi(orderB.getBegin()),
                        new ArrayList<>(),
                        OrderRouteSearchOptions.create(orderB.getId()),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onParamsInvalid(int errCode, String errMsg) {
                                syncWaiting2.countDown();
                            }

                            @Override
                            public void onCalcRouteSuccess(CalcRouteResult calcRouteResult) {
                                List<RouteData> arrayList = calcRouteResult.getRoutes();
                                if (arrayList != null && !arrayList.isEmpty()) {
                                    RouteData routeData1 = arrayList.get(0);
                                    // 司机端核心步骤：
                                    mDriverSync.getRouteManager()
                                            .editRelayRoute(routeData1.getRouteId())
                                            .setRelayOrderRoute(true);
                                    routeData.add(arrayList.get(0));

                                }
                                syncWaiting2.countDown();
                            }

                            @Override
                            public void onCalcRouteFailure(CalcRouteResult calcRouteResult) {
                                syncWaiting2.countDown();
                            }
                        }
                );
                try {
                    syncWaiting2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mDriverPanel.print("接力单线路:" + routeData.get(1).getRouteId());
                mDriverPanel.postAction("上报路线");

                return routeData.size() == 2;
            }
        });

        mDriverPanel.addAction("上报路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final byte[] result = new byte[1];
                mDriverSync.addTLSDriverListener(new SimpleDriDataListener() {

                    @Override
                    public void onPushRouteSuc() {
                        result[0] = 1;
                        syncWaiting.countDown();
                        mDriverSync.removeTLSDriverListener(this);
                    }

                    @Override
                    public void onPushRouteFail(int errCode, String errStr) {
                        result[0] = 0;
                        syncWaiting.countDown();
                    }
                });
                // 司机端核心步骤：
                mDriverSync.uploadRoutes();
                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mPassengerAPanel.postAction("开启同显");
                mPassengerBPanel.postAction("开启同显");
                return result[0] == 1;
            }
        });

        mDriverPanel.addAction("绘制路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                return drawRoutes(mCarNaviView.getMap(), mDriverSync);
            }
        });

        mDriverPanel.addAction("绑定接力订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                MockOrder order = mDriverSyncService.acceptPassenger(mDriver, mPassengerB);
                if (order != null && order.isAccepted()) {
                    mPassengerSyncB.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                    // 司机端核心步骤：
                    mDriverSync.getOrderManager()
                            .addRelayOrder()
                            .setOrderId(order.getId())
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                    mDriverPanel.print("接力单接驾中");

                    mDriverPanel.postAction("请求路线");
                    mDriverPanel.postAction("绘制路线");
                    return order.getId();
                } else {
                    mDriverPanel.print("接力单接驾失败");
                }

                return "";
            }
        });

        mDriverPanel.addAction("乘客送达", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                // 司机端核心步骤：
                mDriverSync.removeRelayOrder();
                mDriverPanel.print("需重新规划接乘客路线");
                return true;
            }
        });
    }

    private boolean drawRoutes(TencentMap map, BaseSyncProtocol manager) {
        List<TLSLatlng> latlngs = manager.getRouteManager().getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }

        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
        List<LatLng> all = new ArrayList<>(mapLatLngs);
        map.addPolyline(new PolylineOptions()
                .width(20)
                .arrow(true)
                .addAll(mapLatLngs));

        TLSLatlng startPosition = manager.getRouteManager().getStartPosition();
        TLSLatlng destPosition = manager.getRouteManager().getDestPosition();

        if (startPosition != null) {
            map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(startPosition))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_start_point)));
        }

        if (destPosition != null) {
            map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(destPosition))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_end_point)));
        }

        //other订单
        List<TLSBOrder> orders = manager.getOrderManager().getNextOrders();
        for (TLSBOrder order : orders) {
            TLSBRoute route = manager.getRouteManager().getRelayRouteByOrderId(order.getOrderId());
            if (route == null) {
                continue;
            }
            List<LatLng> others = ConvertUtil.toLatLngList(route.getPoints());
            all.addAll(others);
            map.addPolyline(new PolylineOptions()
                    .width(10)
                    .color(0xFF00FF00)
                    .addAll(others));
            if (route.getDestPosition() != null) {
                map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(route.getDestPosition()))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.passenger))
                        .anchor(0.5f, 1));
            }
        }

        // 乘客端核心步骤：
        if (manager.getOrderManager().isRelay()) {
            TLSBRoute route = manager.getRouteManager().getRouteByOrderId(manager.getOrderManager().getOrderId());
            List<LatLng> others = ConvertUtil.toLatLngList(route.getPoints());
            all.addAll(others);
            map.addPolyline(new PolylineOptions()
                    .width(10)
                    .color(0xFF00FF00)
                    .addAll(others));
            if (route.getDestPosition() != null) {
                map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(route.getDestPosition()))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.passenger))
                        .anchor(0.5f, 1));
            }
            if (others.isEmpty()) {
                return false;
            }
        }

        MapUtils.fitsWithRoute(map, all,
                25, 25, 25, 25);

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCarNaviView.onStart();
        mMapView1.onStart();
        mMapView2.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCarNaviView.onResume();
        mMapView1.onResume();
        mMapView2.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mCarNaviView.onRestart();
        mMapView1.onRestart();
        mMapView2.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCarNaviView.onPause();
        mMapView1.onPause();
        mMapView2.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCarNaviView.onStop();
        mMapView1.onStop();
        mMapView2.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCarNaviView.onDestroy();
        mMapView1.onDestroy();
        mMapView2.onDestroy();

        mDriverSync.destroy();
        mPassengerSyncA.destroy();
        mPassengerSyncB.destroy();
    }
}
