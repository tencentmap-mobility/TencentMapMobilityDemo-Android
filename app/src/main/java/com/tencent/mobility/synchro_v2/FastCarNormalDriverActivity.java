package com.tencent.mobility.synchro_v2;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.tencent.map.fusionlocation.model.TencentGeoLocation;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.protocol.SyncProtocol;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.Configs;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.mobility.util.MapUtils;
import com.tencent.navix.api.NavigatorZygote;
import com.tencent.navix.api.config.MapAutoScaleConfig;
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
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class FastCarNormalDriverActivity extends BaseActivity {

    private NavigatorLayerRootDrive mCarNaviView;
    private NavigatorLayerViewDrive mLayerDriverView;
    private PanelView mDriverPanel;
    private TSLDExtendManager mDriverSync;
    private MockDriver mDriver;
    private MockSyncService mDriverSyncService;
    private NavigatorDrive mNaviManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.normal_fast_car_driver_layout);

        NavigatorViewStub navigatorViewStub = findViewById(R.id.driver_navi_car_view);
        navigatorViewStub.inflate();
        mCarNaviView = navigatorViewStub.getNavigatorView();
        mLayerDriverView = new NavigatorLayerViewDrive(this);
        mLayerDriverView.setUIComponentConfig(UIComponentConfig.builder()
                .setComponentVisibility(UIComponentConfig.UIComponent.ENLARGE_INFO_VIEW, true)
                .setComponentVisibility(UIComponentConfig.UIComponent.GUIDE_LANE_VIEW, true)
                .setComponentVisibility(UIComponentConfig.UIComponent.MAP_TRAFFIC_SWITCH_VIEW, true)
                .setComponentVisibility(UIComponentConfig.UIComponent.INFO_VIEW, true)
                .setComponentVisibility(UIComponentConfig.UIComponent.ZOOM_CONTROLLER_VIEW, true)
                .setComponentVisibility(UIComponentConfig.UIComponent.TTS_MUTE_SWITCH_VIEW, true)
                .setComponentVisibility(UIComponentConfig.UIComponent.ROAD_LIMIT_VIEW, true)
                .setComponentVisibility(UIComponentConfig.UIComponent.TRAFFIC_BAR_VIEW, true)
                .setComponentVisibility(UIComponentConfig.UIComponent.BOTTOM_PANEL_VIEW, true)
                .build());
        mCarNaviView.addViewLayer(mLayerDriverView);
        mDriverPanel = findViewById(R.id.group_panel_driver);
        initDriverPanel();
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

    private String fastCarOrderId = "";

    private void initDriverPanel() {
        mNaviManager = NavigatorZygote.with(getApplicationContext()).navigator(NavigatorDrive.class);
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
        mCarNaviView.setAutoScaleConfig(MapAutoScaleConfig.builder()
                .setAutoScaleEnable(true)
                .build());

        MockCar car = MockSyncService.newRandomCar();
        mDriver = MockSyncService.newRandomDriver(mCarNaviView.getMapApi(), car);
        mDriverSync = TSLDExtendManager.newInstance();
        mDriverSyncService = new MockSyncService(mDriverSync);
        mDriverSync.init(FastCarNormalDriverActivity.this,
                TLSConfigPreference.create()
                        .enableTrafficLightCountDown(true)
                        .setDebuggable(true)
                        .setAllTimeLocation(true)
                        .setAccountId(mDriver.getId()));
        mDriverSync.setNaviManager(mNaviManager);
        mDriverPanel.init("司机端","创建订单", "绑定订单", "接驾中", "司机已到达", "乘客已上车", "服务完成");

        // 注意点1：实际由乘客端创建订单
        mDriverPanel.addAction("创建订单", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mDriverSyncService.newOrderLocal(mCarNaviView.getMapApi(), mDriver);
                fastCarOrderId = order.getId();
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 1);
                requestMap.put("userid", "passenger1");
                requestMap.put("userdev", "passenger-dev1");
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(order.getId(), requestMap, new SyncProtocol.OrderResultListener() {
                    @Override
                    public void onResult(int status, String message) {
                        result[0] = true;
                        Log.i(Configs.TAG, "status: " + status + ", message: " + message);
                        sync.countDown();
                    }
                });
                try {
                    sync.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return result[0];
            }
        });

        mDriverPanel.addAction("绑定订单", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                // 注意点2：如果没使用该页面的“创建订单”，则手动设置订单ID
//                fastCarOrderId = "mc-order-1739866491035";
                if (TextUtils.isEmpty(fastCarOrderId)) {
                    MockOrder order = mDriverSyncService.newOrderLocal(mCarNaviView.getMapApi(), mDriver);
                    fastCarOrderId = order.getId();
                }
                mDriver.setPosition(MockSyncService.getRandomVisibleLatLng(mCarNaviView.getMapApi().getProjection()));
                mDriverSync.getOrderManager().editCurrent().setOrderId(fastCarOrderId);
                mDriverSync.getOrderManager().editCurrent().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusInit);
                mDriverPanel.postAction("开启同显");
                return true;
            }
        });

        mDriverPanel.addAction("开启同显", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.setPushTimeInterval(5);
                mDriverSync.start();
                return true;
            }
        });

        mDriverPanel.addAction("接驾中", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 2);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(fastCarOrderId, requestMap, new SyncProtocol.OrderResultListener() {
                    @Override
                    public void onResult(int status, String message) {
                        result[0] = true;
                        mDriverSync.getOrderManager().editCurrent().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mDriverPanel.print("当前订单接驾中");
                        mDriverPanel.postAction("请求路线");
                        sync.countDown();
                    }
                });
                try {
                    sync.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return result[0];
            }
        });

        mDriverPanel.addAction("司机已到达", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                if (mNaviManager.isNavigating()) {
                    mNaviManager.stopNavigation();
                    return true;
                }
                return false;
            }
        });

        mDriverPanel.addAction("乘客已上车", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 3);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(fastCarOrderId, requestMap, new SyncProtocol.OrderResultListener() {
                    @Override
                    public void onResult(int status, String message) {
                        result[0] = true;
                        mDriverSync.getOrderManager().editCurrent().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                        mDriverPanel.print("当前订单送驾中");
                        mDriverPanel.postAction("请求路线");
                        sync.countDown();
                    }
                });
                try {
                    sync.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return result[0];
            }
        });

        mDriverPanel.addAction("服务完成", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                if (mNaviManager.isNavigating()) {
                    mNaviManager.stopNavigation();
                    return true;
                }
                return false;
            }
        });

        mDriverPanel.addAction("请求路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<NavDriveRoute> routeData = new ArrayList<>();
                // 当前接送驾路线
                mDriverSync.searchCarRoutes(fastCarOrderId,
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

    @Override
    protected void onStart() {
        super.onStart();
        mCarNaviView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCarNaviView.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mCarNaviView.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCarNaviView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCarNaviView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNaviManager.stopNavigation();
        mNaviManager.unbindView(mCarNaviView);
        mCarNaviView.removeViewLayer(mLayerDriverView);
        mCarNaviView.onDestroy();
        mDriverSync.destroy();
        NavigatorZygote.with(this.getApplicationContext()).locationApi().removeLocationObserver(mGeoLocationObserver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
