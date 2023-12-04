package com.tencent.mobility.synchro_v2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.map.fusionlocation.model.TencentGeoLocation;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.lsp.listener.SimplePsgDataListener;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSDFetchedData;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.bean.TLSTrafficLightInfo;
import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.synchro_v2.view.BubbleView;
import com.tencent.mobility.synchro_v2.view.RouteInfoView;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.AnimatorUtils;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.mobility.util.MapUtils;
import com.tencent.mobility.util.SingleHelper;
import com.tencent.navix.api.NavigatorZygote;
import com.tencent.navix.api.config.MultiRouteConfig;
import com.tencent.navix.api.config.RouteElementConfig;
import com.tencent.navix.api.layer.NavigatorLayerRootDrive;
import com.tencent.navix.api.layer.NavigatorViewStub;
import com.tencent.navix.api.location.GeoLocationObserver;
import com.tencent.navix.api.map.MapApi;
import com.tencent.navix.api.model.NavDriveRoute;
import com.tencent.navix.api.model.NavError;
import com.tencent.navix.api.model.NavMode;
import com.tencent.navix.api.model.NavRoutePlan;
import com.tencent.navix.api.navigator.NavigatorDrive;
import com.tencent.navix.api.plan.DriveRoutePlanOptions;
import com.tencent.navix.ui.NavigatorLayerViewDrive;
import com.tencent.navix.ui.api.config.UIComponentConfig;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FastCarNormalActivity extends BaseActivity {

    private NavigatorLayerRootDrive mCarNaviView;
    private NavigatorLayerViewDrive mLayerDriverView;
    private NavigatorLayerRootDrive mMapView;

    private PanelView mDriverPanel;
    private PanelView mPassengerPanel;

    private TSLDExtendManager mDriverSync;
    private TSLPassengerManager mPassengerSync;

    private MockDriver mDriver;
    private MockPassenger mPassenger;

    private MockSyncService mDriverSyncService;
    private MockSyncService mPassengerSyncService;

    private NavigatorDrive mNaviManager;

    private RouteInfoView mRouteOneView;
    private RouteInfoView mRouteTwoView;
    private RouteInfoView mRouteThreeView;
    private BubbleView bubbleView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.normal_fast_car_layout);

        NavigatorViewStub navigatorViewStub = findViewById(R.id.driver_navi_car_view);
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

        NavigatorViewStub navigatorViewStub1 = findViewById(R.id.psg_map_view);
        navigatorViewStub1.inflate();
        mMapView = navigatorViewStub1.getNavigatorView();
        bubbleView = new BubbleView(this);
        mMapView.getMapApi().setInfoWindowAdapter(new BubbleWindow());

        mRouteOneView = findViewById(R.id.route_info_one_view);
        mRouteTwoView = findViewById(R.id.route_info_two_view);
        mRouteThreeView = findViewById(R.id.route_info_three_view);

        mDriverPanel = findViewById(R.id.group_panel_driver);
        mPassengerPanel = findViewById(R.id.group_panel_passenger);

        initPassengerPanel();
        initDriverPanel();

        AnimatorUtils.init(FastCarNormalActivity.this);
        NavigatorZygote.with(this.getApplicationContext()).locationApi().addLocationObserver(mGeoLocationObserver, 1000);
    }

    private final GeoLocationObserver mGeoLocationObserver = new GeoLocationObserver() {
        @Override
        public void onGeoLocationChanged(TencentGeoLocation tencentGeoLocation) {
            super.onGeoLocationChanged(tencentGeoLocation);
            if (tencentGeoLocation == null) {
                return;
            }
            TencentLocation tencentLocation = tencentGeoLocation.getLocation();
            if (tencentLocation == null) {
                return;
            }
            tlsbPosition.setAccuracy(tencentLocation.getAccuracy());
            tlsbPosition.setAltitude(tencentLocation.getAltitude());
            tlsbPosition.setBearing(tencentLocation.getBearing());
            tlsbPosition.setCityCode(tencentLocation.getCityCode());
            tlsbPosition.setLatitude(tencentLocation.getLatitude());
            tlsbPosition.setLongitude(tencentLocation.getLongitude());
            tlsbPosition.setProvider(ConvertUtil.providerTypeByProvider(tencentLocation.getProvider()));
            tlsbPosition.setTime(tencentLocation.getTime());
            tlsbPosition.setVelocity(tencentLocation.getSpeed());
            tlsbPosition.setMockGPS(tencentLocation.isMockGps());
        }
    };

    private TLSBPosition tlsbPosition = new TLSBPosition();

    private void initPassengerPanel() {
        mPassenger = MockSyncService.newRandomPassenger(mMapView.getMapApi());
        mPassengerSync = TSLPassengerManager.newInstance();
        mPassengerSync.init(FastCarNormalActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mPassenger.getId()));
        mPassengerSync.setPullTimeInterval(3);
        mPassengerSyncService = new MockSyncService(mPassengerSync);
        mPassengerPanel.init("当前乘客", "创建订单");
        mPassengerPanel.addAction("创建订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                mPassengerPanel.postAction("开启同显");
                mPassenger.setPosition(MockSyncService.getRandomVisibleLatLng(mMapView.getMapApi().getProjection()));
                MockOrder order = mPassengerSyncService.newOrder(mMapView.getMapApi(), mPassenger);
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    mPassengerPanel.postPrint("等待接驾");
                    mPassengerSync.getOrderManager().editCurrent().setOrderId(order.getId());
                    mDriverPanel.postAction("绑定订单");
                    return order.getId();
                }
                return "";
            }
        });

        mPassengerPanel.addAction("开启同显", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerSync.addTLSPassengerListener(new SimplePsgDataListener() {
                    @Override
                    public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
                        drawMarker(mMapView.getMapApi(), mPassengerSync);
                        AnimatorUtils.updateDriverInfo(mMapView.getMapApi(), bubbleView, fetchedData.getRoute(), fetchedData.getOrder(), fetchedData.getPositions());

                        mRouteOneView.reset();
                        mRouteTwoView.reset();
                        mRouteThreeView.reset();
                        int mainIndex = mPassengerSync.getRouteManager().getRouteIndexByRouteId(fetchedData.getRoute().getRouteId());
                        for (int i = 0; i < fetchedData.getRoutes().size(); i++) {
                            if (i == 0) {
                                mRouteOneView.setData(fetchedData.getRoutes().get(i).getRemainingTime(),
                                        fetchedData.getRoutes().get(i).getRemainingDistance(),
                                        fetchedData.getRoutes().get(i).getRemainingTrafficCount());
                                if (mainIndex == 0) {
                                    mRouteOneView.isMain(true);
                                }

                            } else if (i == 1) {
                                mRouteTwoView.setData(fetchedData.getRoutes().get(i).getRemainingTime(),
                                        fetchedData.getRoutes().get(i).getRemainingDistance(),
                                        fetchedData.getRoutes().get(i).getRemainingTrafficCount());
                                if (mainIndex == 0) {
                                    mRouteOneView.isMain(true);
                                    mRouteTwoView.isMain(false);
                                } else if (mainIndex == 1) {
                                    mRouteOneView.isMain(false);
                                    mRouteTwoView.isMain(true);
                                }
                            } else if (i == 2) {
                                mRouteThreeView.setData(fetchedData.getRoutes().get(i).getRemainingTime(),
                                        fetchedData.getRoutes().get(i).getRemainingDistance(),
                                        fetchedData.getRoutes().get(i).getRemainingTrafficCount());
                                if (mainIndex == 0) {
                                    mRouteOneView.isMain(true);
                                    mRouteTwoView.isMain(false);
                                    mRouteThreeView.isMain(false);
                                } else if (mainIndex == 1) {
                                    mRouteOneView.isMain(false);
                                    mRouteTwoView.isMain(true);
                                    mRouteThreeView.isMain(false);
                                } else if (mainIndex == 2) {
                                    mRouteOneView.isMain(false);
                                    mRouteTwoView.isMain(false);
                                    mRouteThreeView.isMain(true);
                                }
                            }
                        }
                    }

                    @Override
                    public void onPullLsInfoFail(int errCode, String errMsg) {
                        mPassengerPanel.postPrint("拉取失败：" + errCode);
                    }

                    @Override
                    public void onReceiveTrafficLightCountdown(TLSTrafficLightInfo trafficLightInfo) {
                        mPassengerPanel.postPrint("红绿灯信息：" + trafficLightInfo);
                        if (trafficLightInfo.getStatus() == TLSTrafficLightInfo.TrafficLightType.LIGHT_RED.getType()) {
                            int endTimestamp = trafficLightInfo.getEndTimestamp();
                            int currentTimestamp = (int) (System.currentTimeMillis() / 1000);
                            if (endTimestamp <= currentTimestamp) {
                                return;
                            }
                            AnimatorUtils.updateWaitingInfo(endTimestamp - currentTimestamp, bubbleView);
                        } else if (trafficLightInfo.getStatus() == TLSTrafficLightInfo.TrafficLightType.LIGHT_NONE.getType()) {
                            AnimatorUtils.updateWaitingInfo(0, bubbleView);
                        }
                    }
                });

                mPassengerSync.start();
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
//                .setTrafficBubbleEnable(false)
//                .setTrafficLightEnable(false)
                .setCameraDistanceEnable(false)
                .setCameraMarkerEnable(false)
                .build());

        MockCar car = MockSyncService.newRandomCar();
        mDriver = MockSyncService.newRandomDriver(mCarNaviView.getMapApi(), car);
        mDriverSync = TSLDExtendManager.newInstance();
        mDriverSyncService = new MockSyncService(mDriverSync);
        mDriverSync.init(FastCarNormalActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mDriver.getId()));
        mDriverSync.setNaviManager(mNaviManager);
        mDriverPanel.init("司机");

        mDriverPanel.addAction("绑定订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                mDriver.setPosition(MockSyncService.getRandomVisibleLatLng(mCarNaviView.getMapApi().getProjection()));
                MockOrder order = mDriverSyncService.getOrder(mPassenger);
                if (order == null) {
                    return "";
                }
                order.setStatus(MockOrder.Status.Waiting);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                if (order.isWaiting()) {
                    mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    order = mDriverSyncService.acceptPassenger(mDriver, mPassenger);
                    if (order.isAccepted()) {
                        mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mDriverPanel.print("当前订单接驾中");
                        mDriverPanel.postAction("开启同显");
                        return order.getId();
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
                final MockOrder orderA = mDriverSyncService.getOrder(mPassenger);
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<NavDriveRoute> routeData = new ArrayList<>();

                // 当前送驾路线
                mDriverSync.searchCarRoutes(orderA.getId(),
//                        ConvertUtils.toNaviPoi(orderA.getBegin()),
//                        ConvertUtils.toNaviPoi(orderA.getEnd()),
                        ConvertUtils.toNaviPoi(new LatLng(40.007398, 116.390305)),
                        ConvertUtils.toNaviPoi(new LatLng(39.896938, 116.316483)),
                        new ArrayList<>(),
                        DriveRoutePlanOptions.Companion.newBuilder().build(),
                        new DriDataListener.ISearchCallBack() {
                            @Override
                            public void onResultCallback(NavRoutePlan navRoutePlan, NavError navError) {
                                if (navRoutePlan != null) {
                                    List<NavDriveRoute> arrayList = navRoutePlan.getRoutes();
                                    if (arrayList != null && !arrayList.isEmpty()) {
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
                return routeData.size() == 1;
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
                mDriverSync.uploadUsingRoute(tlsbPosition);
                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return result[0] == 1;
            }
        });
    }

    Marker aMarkerStart = null;
    Marker aMarkerEnd = null;
    private void drawMarker(MapApi map, BaseSyncProtocol manager) {
        List<TLSLatlng> latlngs = manager.getRouteManager().getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return;
        }
        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
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
    }

    private class BubbleWindow implements TencentMap.InfoWindowAdapter {

        @Override
        public View getInfoWindow(Marker marker) {
            return bubbleView;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCarNaviView.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCarNaviView.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mCarNaviView.onRestart();
        mMapView.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCarNaviView.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCarNaviView.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNaviManager.stopNavigation();
        mNaviManager.unbindView(mCarNaviView);
        mCarNaviView.removeViewLayer(mLayerDriverView);
        mCarNaviView.onDestroy();
        mMapView.onDestroy();
        AnimatorUtils.clearUi();
        mDriverSync.destroy();
        mPassengerSync.destroy();
        NavigatorZygote.with(this.getApplicationContext()).locationApi().removeLocationObserver(mGeoLocationObserver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
