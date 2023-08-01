package com.tencent.mobility.synchro_v2.driver;

import static com.tencent.mobility.util.AnimatorUtils.setRouteTraffic;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.lsp.listener.SimplePsgDataListener;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBRoute;
import com.tencent.map.lssupport.bean.TLSBRouteTrafficItem;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSDFetchedData;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.CommonUtils;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.mobility.util.MapUtils;
import com.tencent.mobility.util.SingleHelper;
import com.tencent.navix.api.config.MultiRouteConfig;
import com.tencent.navix.api.config.RouteElementConfig;
import com.tencent.navix.api.layer.NavigatorLayerRootDrive;
import com.tencent.navix.api.layer.NavigatorViewStub;
import com.tencent.navix.api.map.MapApi;
import com.tencent.navix.api.model.NavDriveRoute;
import com.tencent.navix.api.model.NavError;
import com.tencent.navix.api.model.NavMode;
import com.tencent.navix.api.model.NavRoutePlan;
import com.tencent.navix.api.navigator.NavigatorDrive;
import com.tencent.navix.api.plan.DriveRoutePlanOptions;
import com.tencent.navix.ui.NavigatorLayerViewDrive;
import com.tencent.navix.ui.api.config.UIComponentConfig;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.maps.model.Polyline;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 接力单功能
 */
public class DriverRelayOrderActivity extends BaseActivity {

    private NavigatorLayerRootDrive mCarNaviView;
    private NavigatorLayerViewDrive mLayerDriverView;
    private NavigatorLayerRootDrive mMapView1;
    private NavigatorLayerRootDrive mMapView2;

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

    private NavigatorDrive mNaviManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relay_layout);

        NavigatorViewStub navigatorViewStub = findViewById(R.id.navi_car_view);
        navigatorViewStub.inflate();
        mCarNaviView = navigatorViewStub.getNavigatorView();
        mLayerDriverView = new NavigatorLayerViewDrive(this);
        mLayerDriverView.setUIComponentConfig(UIComponentConfig.builder()
                .setComponentVisibility(UIComponentConfig.UIComponent.ENLARGE_INFO_VIEW, false)
                .setComponentVisibility(UIComponentConfig.UIComponent.GUIDE_LANE_VIEW, false)
                .setComponentVisibility(UIComponentConfig.UIComponent.MAP_TRAFFIC_SWITCH_VIEW, false)
                .setComponentVisibility(UIComponentConfig.UIComponent.INFO_VIEW, false)
                .setComponentVisibility(UIComponentConfig.UIComponent.ZOOM_CONTROLLER_VIEW, false)
                .setComponentVisibility(UIComponentConfig.UIComponent.TTS_MUTE_SWITCH_VIEW, false)
                .setComponentVisibility(UIComponentConfig.UIComponent.ROAD_LIMIT_VIEW, false)
                .setComponentVisibility(UIComponentConfig.UIComponent.TRAFFIC_BAR_VIEW, false)
                .setComponentVisibility(UIComponentConfig.UIComponent.BOTTOM_PANEL_VIEW, false)
                .build());
        mCarNaviView.addViewLayer(mLayerDriverView);

        NavigatorViewStub navigatorViewStub1 = findViewById(R.id.map_view1);
        navigatorViewStub1.inflate();
        mMapView1 = navigatorViewStub1.getNavigatorView();

        NavigatorViewStub navigatorViewStub2 = findViewById(R.id.map_view2);
        navigatorViewStub2.inflate();
        mMapView2 = navigatorViewStub2.getNavigatorView();

        mDriverPanel = findViewById(R.id.group_panel_driver);
        mPassengerAPanel = findViewById(R.id.group_panel_passenger_a);
        mPassengerBPanel = findViewById(R.id.group_panel_passenger_b);

        initCurrentPassengerPanel();
        initDriverPanel();
        initRelayPassengerPanel();
    }

    private void initCurrentPassengerPanel() {
        mPassengerA = MockSyncService.newRandomPassenger(mMapView1.getMapApi());
        mPassengerSyncA = TSLPassengerManager.newInstance();
        mPassengerSyncA.init(DriverRelayOrderActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mPassengerA.getId()));
        mPassengerSyncA.setPullTimeInterval(3);
        mPassengerSyncService = new MockSyncService(mPassengerSyncA);
        mPassengerAPanel.init("当前乘客", "创建订单");
        mPassengerAPanel.addAction("创建订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                mPassengerAPanel.postAction("开启同显");
                mPassengerA.setPosition(MockSyncService.getRandomVisibleLatLng(mMapView1.getMapApi().getProjection()));
                MockOrder order = mPassengerSyncService.newOrder(mMapView1.getMapApi(), mPassengerA);
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
                            boolean ret = drawRoutesA(mMapView1.getMapApi(), mPassengerSyncA);
                            mPassengerAPanel.print("绘制路线:" + ret);
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
        mPassengerB = MockSyncService.newRandomPassenger(mMapView2.getMapApi());
        mPassengerSyncB = TSLPassengerManager.newInstance();
        mPassengerSyncB.init(DriverRelayOrderActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mPassengerB.getId()));
        mPassengerSyncB.setPullTimeInterval(3);
        mPassengerBPanel.init("接力单乘客", "创建订单", "修改上车点");
        mPassengerBPanel.addAction("创建订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                mPassengerB.setPosition(MockSyncService.getRandomVisibleLatLng(mMapView2.getMapApi().getProjection()));
                MockOrder order = mPassengerSyncService.newOrder(mMapView2.getMapApi(), mPassengerB);
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    mPassengerBPanel.postPrint("等待接驾");
                    mPassengerSyncB.getOrderManager().editCurrent().setOrderId(order.getId());
                    mDriverPanel.postAction("绑定接力订单");
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
                            boolean ret = drawRoutesB(mMapView2.getMapApi(), mPassengerSyncB);
                            mPassengerBPanel.print("绘制路线:" + ret);
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

        mPassengerBPanel.addAction("修改上车点", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder orderB = mPassengerSyncService.getOrder(mPassengerB);
                if (orderB == null) {
                    return false;
                }
                orderB.setBegin(MockSyncService.getRandomVisibleLatLng(mMapView2.getMapApi().getProjection()));
                mPassengerBPanel.print("修改上车点");
                mDriverPanel.postAction("修改起点重新规划路线");
                return true;
            }
        });
    }

    private void initDriverPanel() {
        mNaviManager = SingleHelper.getNaviManager(this);
        mNaviManager.setMultiRouteConfig(MultiRouteConfig.builder()
                .setMultiRouteEnable(true)
                .setShowMultiRouteOnNavStart(false)
                .build());
        mNaviManager.bindView(mCarNaviView);
        mCarNaviView.setNavMode(NavMode.MODE_OVERVIEW);
        mCarNaviView.setRouteElementConfig(RouteElementConfig.builder()
                .setTurnArrowEnable(false)
                .setTrafficBubbleEnable(false)
                .setTrafficLightEnable(false)
                .setCameraDistanceEnable(false)
                .setCameraMarkerEnable(false)
                .build());

        MockCar car = MockSyncService.newRandomCar();
        mDriver = MockSyncService.newRandomDriver(mCarNaviView.getMapApi(), car);
        mDriverSync = TSLDExtendManager.newInstance();
        mDriverSyncService = new MockSyncService(mDriverSync);
        mDriverSync.init(DriverRelayOrderActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mDriver.getId()));
        mDriverSync.setNaviManager(mNaviManager);
        mDriverPanel.init("司机", "乘客送达");

        mDriverPanel.addAction("绑定订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                mDriver.setPosition(MockSyncService.getRandomVisibleLatLng(mCarNaviView.getMapApi().getProjection()));
                MockOrder order = mDriverSyncService.getOrder(mPassengerA);
                order.setStatus(MockOrder.Status.Waiting);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                if (order.isWaiting()) {
                    order = mDriverSyncService.acceptPassenger(mDriver, mPassengerA);
                    if (order.isAccepted()) {
                        mDriverPanel.print("当前订单接驾中");
                        order = mDriverSyncService.onTheWayPassenger(mDriver, mPassengerA);
                        if (order.isOnTheWay()) {
                            mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                            mPassengerSyncA.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                            mDriverPanel.print("当前订单送驾中");
                            mDriverPanel.postAction("开启同显");
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
                mDriverPanel.postAction("请求路线");
                return true;
            }
        });

        mDriverPanel.addAction("请求路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder orderA = mDriverSyncService.getOrder(mPassengerA);
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<NavDriveRoute> routeData = new ArrayList<>();

                // 当前送驾路线
                mDriverSync.searchCarRoutes(orderA.getId(),
                        ConvertUtils.toNaviPoi(orderA.getBegin()),
                        ConvertUtils.toNaviPoi(orderA.getEnd()),
                        new ArrayList<>(),
                        DriveRoutePlanOptions.Companion.newBuilder().build(),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onInternalError(int errCode, String errMsg) {
                                syncWaiting.countDown();
                            }

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
                        });

                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mDriverSync.getRouteManager().useRouteIndex(0);
                mDriverPanel.print("当前线路：" + routeData.get(0).getRouteId());
                mDriverPanel.postAction("上报路线");

                try {
                    mNaviManager.simulator().setEnable(true);
                    mNaviManager.startNavigation(mDriverSync.getRouteManager().getRouteId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                MapUtils.fitsWithRoute(mCarNaviView.getMapApi(), ConvertUtils.transformLatLngs(mDriverSync.getRouteManager().getPoints()),
                        25, 25, 25, 25);
                return routeData.size() >= 1;
            }
        });

        mDriverPanel.addAction("请求接力单路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder orderA = mDriverSyncService.getOrder(mPassengerA);
                final List<NavDriveRoute> routeData = new ArrayList<>();
                final MockOrder orderB = mDriverSyncService.getOrder(mPassengerB);
                final CountDownLatch syncWaiting2 = new CountDownLatch(1);
                // 接力单路线
                mDriverSync.searchCarRoutes(orderB.getId(),
                        ConvertUtils.toNaviPoi(orderA.getEnd()),
                        ConvertUtils.toNaviPoi(orderB.getBegin()),
                        new ArrayList<>(),
                        DriveRoutePlanOptions.Companion.newBuilder().build(),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onResultCallback(NavRoutePlan navRoutePlan, NavError navError) {
                                if (navRoutePlan != null) {
                                    List<NavDriveRoute> arrayList = navRoutePlan.getRoutes();
                                    if (arrayList != null && !arrayList.isEmpty()) {
                                        NavDriveRoute routeData1 = arrayList.get(0);
                                        // 司机端核心步骤：
                                        mDriverSync.getRouteManager()
                                                .editRelayRoute(routeData1.getRouteId())
                                                .setRelayOrderRoute(true);
                                        routeData.add(arrayList.get(0));

                                    }
                                }
                                syncWaiting2.countDown();
                            }

                            @Override
                            public void onInternalError(int errCode, String errMsg) {
                                syncWaiting2.countDown();
                            }
                        }
                );
                try {
                    syncWaiting2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mDriverPanel.print("接力单线路:" + routeData.get(0).getRouteId());
                mDriverPanel.postAction("上报路线");
                return routeData.size() == 1;
            }
        });

        mDriverPanel.addAction("送达乘客重新规划路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder orderA = mDriverSyncService.getOrder(mPassengerA);
                final MockOrder orderB = mDriverSyncService.getOrder(mPassengerB);
                final List<NavDriveRoute> routeData = new ArrayList<>();
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                // 接力单路线
                mDriverSync.searchCarRoutes(orderB.getId(),
                        ConvertUtils.toNaviPoi(orderA.getEnd()),
                        ConvertUtils.toNaviPoi(orderB.getBegin()),
                        new ArrayList<>(),
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

                mDriverPanel.print("路线总数：" + routeData.size());
                mDriverSync.getRouteManager().useRouteIndex(0);
                mDriverPanel.postAction("上报路线");
                try {
                    mNaviManager.simulator().setEnable(true);
                    mNaviManager.startNavigation(mDriverSync.getRouteManager().getRouteId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                MapUtils.fitsWithRoute(mCarNaviView.getMapApi(), ConvertUtils.transformLatLngs(mDriverSync.getRouteManager().getPoints()),
                        25, 25, 25, 25);
                return routeData.size() >= 1;
            }
        });

        mDriverPanel.addAction("修改起点重新规划路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder orderA = mDriverSyncService.getOrder(mPassengerA);
                final MockOrder orderB = mDriverSyncService.getOrder(mPassengerB);
                final List<NavDriveRoute> routeData = new ArrayList<>();
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                // 接力单路线
                mDriverSync.searchCarRoutes(orderB.getId(),
                        ConvertUtils.toNaviPoi(orderA.getEnd()),
                        ConvertUtils.toNaviPoi(orderB.getBegin()),
                        new ArrayList<>(),
                        DriveRoutePlanOptions.Companion.newBuilder().build(),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onResultCallback(NavRoutePlan navRoutePlan, NavError navError) {
                                if (navRoutePlan != null) {
                                    List<NavDriveRoute> arrayList = navRoutePlan.getRoutes();
                                    if (arrayList != null && !arrayList.isEmpty()) {
                                        NavDriveRoute routeData1 = arrayList.get(0);
                                        mDriverSync.getRouteManager()
                                                .editRelayRoute(routeData1.getRouteId())
                                                .setRelayOrderRoute(true);
                                        routeData.add(arrayList.get(0));
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

                mDriverPanel.print("路线总数：" + routeData.size());
                mDriverPanel.postAction("上报路线");
                MapUtils.fitsWithRoute(mCarNaviView.getMapApi(), ConvertUtils.transformLatLngs(mDriverSync.getRouteManager().getPoints()),
                        25, 25, 25, 25);
                return routeData.size() >= 1;
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

                if (mDriverSync.getOrderManager().getRelayOrder() != null) {
                    mPassengerBPanel.postAction("开启同显");
                }
                return result[0] == 1;
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
                    mDriverPanel.postAction("请求接力单路线");
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
                mPassengerSyncA.destroy();
                mNaviManager.stopNavigation();
                mDriverPanel.postAction("送达乘客重新规划路线");
                return true;
            }
        });
    }

    Polyline polylineA = null;
    Marker aMarkerStart = null;
    Marker aMarkerEnd = null;
    String curRouteIdA = null;

    private boolean drawRoutesA(MapApi map, BaseSyncProtocol manager) {
        List<TLSLatlng> latlngs = manager.getRouteManager().getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }

        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
        List<LatLng> all = new ArrayList<>(mapLatLngs);
        List<TLSBRouteTrafficItem> trafficItems = manager.getRouteManager().getUsingRoute()
                .getTrafficItemsWithInternalRoute();
        int[] colors = new int[trafficItems.size()];
        int[] indexes = new int[trafficItems.size()];
        setRouteTraffic(trafficItems, indexes, colors);
        if (polylineA == null) {
            polylineA = map.addPolyline(new PolylineOptions()
                    .width(20)
                    .colors(colors, indexes)
                    .arrow(true)
                    .addAll(mapLatLngs));
            MapUtils.fitsWithRoute(map, all,
                    25, 25, 25, 25);
        }

        if (!manager.getRouteManager().getRouteId().equals(curRouteIdA) && curRouteIdA != null) {
            polylineA.setPoints(mapLatLngs);
        }
        curRouteIdA = manager.getRouteManager().getRouteId();
        polylineA.setColors(colors, indexes);
        if (aMarkerStart != null) {
            aMarkerStart.remove();
        }
        aMarkerStart = map.addMarker(new MarkerOptions(mapLatLngs.get(0))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_start_point)));

        if (aMarkerEnd != null) {
            aMarkerEnd.remove();
        }
        aMarkerEnd = map.addMarker(new MarkerOptions(mapLatLngs.get(mapLatLngs.size() - 1))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_end_point)));
        return true;
    }

    Polyline polylineB1 = null;
    Polyline polylineB2 = null;
    Marker markerB2 = null;
    String curRouteIdB1 = null;
    String curRouteIdB2 = null;

    private boolean drawRoutesB(MapApi map, BaseSyncProtocol manager) {
        List<TLSLatlng> latlngs = manager.getRouteManager().getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }

        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
        List<LatLng> all = new ArrayList<>(mapLatLngs);
        List<TLSBRouteTrafficItem> trafficItems = manager.getRouteManager().getUsingRoute()
                .getTrafficItemsWithInternalRoute();
        int[] colors = new int[trafficItems.size()];
        int[] indexes = new int[trafficItems.size()];
        setRouteTraffic(trafficItems, indexes, colors);
        if (polylineB1 == null) {
            polylineB1 = map.addPolyline(new PolylineOptions()
                    .width(20)
                    .colors(colors, indexes)
                    .arrow(true)
                    .addAll(mapLatLngs));
            MapUtils.fitsWithRoute(map, all,
                    25, 25, 25, 25);
        }
        if (!manager.getRouteManager().getRouteId().equals(curRouteIdB1) && curRouteIdB1 != null) {
            polylineB1.setPoints(mapLatLngs);
        }
        curRouteIdB1 = manager.getRouteManager().getRouteId();
        polylineB1.setColors(colors, indexes);

        if (!manager.getRouteManager().getRelayRoutes().isEmpty()) {
            TLSBRoute route = manager.getRouteManager().getRelayRoutes().get(0);
            List<LatLng> others = ConvertUtil.toLatLngList(route.getPoints());
            if (others.isEmpty()) {
                return false;
            }
            all.addAll(others);
            List<TLSBRouteTrafficItem> trafficItems1 = route.getTrafficItemsWithInternalRoute();
            int[] colors1 = new int[trafficItems1.size()];
            int[] indexes1 = new int[trafficItems1.size()];
            setRouteTraffic(trafficItems1, indexes1, colors1);
            if (polylineB2 == null) {
                polylineB2 = map.addPolyline(new PolylineOptions()
                        .width(10)
                        .colors(colors1, indexes1)
                        .addAll(others));
            }
            if (!route.getRouteId().equals(curRouteIdB2) && curRouteIdB2 != null) {
                polylineB2.setPoints(others);
            }
            curRouteIdB2 = route.getRouteId();
            polylineB2.setColors(colors, indexes);
            if (markerB2 != null) {
                markerB2.remove();
            }
            markerB2 = map.addMarker(new MarkerOptions(others.get(others.size() - 1))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.passenger))
                    .anchor(0.5f, 1));
        } else {
            if (polylineB2 != null) {
                polylineB2.remove();
            }
        }
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
        mNaviManager.stopNavigation();
        mNaviManager.unbindView(mCarNaviView);
        mCarNaviView.removeViewLayer(mLayerDriverView);
        mCarNaviView.onDestroy();
        mMapView1.onDestroy();
        mMapView2.onDestroy();

        mDriverSync.destroy();
        mPassengerSyncA.destroy();
        mPassengerSyncB.destroy();
    }
}
