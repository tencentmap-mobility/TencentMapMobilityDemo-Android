package com.tencent.mobility.synchro_v2.psg;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.gaya.framework.tools.Streams;
import com.tencent.lbssearch.object.param.DrivingParam;
import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.lsp.listener.SimplePsgDataListener;
import com.tencent.map.lspassenger.protocol.SearchProtocol;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBRoute;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSDFetchedData;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.protocol.SyncProtocol;
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
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.maps.model.Polyline;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 乘客选路
 */
public class PassengerSelectRoutesActivity extends BaseActivity {

    private NavigatorLayerRootDrive mCarNaviView;
    private NavigatorLayerViewDrive mLayerDriverView;

    private NavigatorLayerRootDrive mMapView;

    private PanelView mDriverPanel;
    private PanelView mPassengerPanel;

    private TSLDExtendManager mDriverSync;
    private TSLPassengerManager mPassengerSync;

    private MockDriver mDriver;
    private MockPassenger mPassenger;

    private MockSyncService mMockSyncService;
    private boolean mPassengerDrawRoute;
    private NavigatorDrive mNaviManager;
    private List<TLSBRoute> mTLSRoutes;
    private int mChosenRouteIndex = 0;
    private boolean onTrip = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.one_driver_one_passenger_layout);

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
        mMapView = navigatorViewStub1.getNavigatorView();

        mDriverPanel = findViewById(R.id.group_panel_driver);
        mPassengerPanel = findViewById(R.id.group_panel_passenger);

        initPassengerPanel();
        initDriverPanel();

        mMockSyncService = new MockSyncService(mDriverSync);
    }

    private void initPassengerPanel() {
        mPassenger = MockSyncService.newRandomPassenger(mMapView.getMapApi());
        mPassengerSync = TSLPassengerManager.newInstance();
        mPassengerSync.init(this, TLSConfigPreference.create()
                .setAccountId(mPassenger.getId()));
        mPassengerPanel.init("乘客", "行前选路", "行中选路");
        mPassengerPanel.addAction("行前选路", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                onTrip = false;
                mPassenger.setPosition(MockSyncService.getRandomVisibleLatLng(mMapView.getMapApi().getProjection()));
                MockOrder order = mMockSyncService.newOrder(mMapView.getMapApi(), mPassenger);
                if (order != null && !TextUtils.isEmpty(order.getId())) {

                    mPassengerPanel.postAction("开启同显");
                    mPassengerSync.addTLSPassengerListener(new SimplePsgDataListener() {

                        @Override
                        public void onPushRouteSuc() {
                            super.onPushRouteSuc();
                            mPassengerPanel.print("路线上报成功！");
                            mDriverPanel.postAction("绑定订单至接驾");
                        }

                        @Override
                        public void onPushRouteFail(int errCode, String errStr) {
                            super.onPushRouteFail(errCode, errStr);
                            mPassengerPanel.print("路线上报失败：" + errStr);
                        }

                        @Override
                        public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
                            super.onPullLsInfoSuc(fetchedData);
                            if (fetchedData.getRoute() != null
                                    && !mPassengerDrawRoute
                                    && fetchedData.getRoute().getPoints() != null
                                    && !fetchedData.getRoute().getPoints().isEmpty()) {
//                                mMapView.getMapApi().clearAllOverlays();
                                int routeSize = 1;
                                if (fetchedData.getRoutes() != null) {
                                    routeSize = fetchedData.getRoutes().size();
                                }
                                mPassengerPanel.print("拉取的路线总数：" + routeSize);
                                drawRoute(mMapView.getMapApi(), mPassengerSync);
                                mPassengerDrawRoute = true;
                            }
                        }
                    });

                    mPassengerPanel.print("创建订单：" + order.getId());
                    mPassengerSync.getTLSPOrder().setOrderId(order.getId())
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    mPassengerPanel.postAction("检索路线");
                    return true;
                }
                return false;
            }
        });

        mPassengerPanel.addAction("行中选路", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                onTrip = true;
                mPassenger.setPosition(MockSyncService.getRandomVisibleLatLng(mMapView.getMapApi().getProjection()));
                MockOrder order = mMockSyncService.newOrder(mMapView.getMapApi(), mPassenger);
                if (order != null && !TextUtils.isEmpty(order.getId())) {

                    mPassengerSync.addTLSPassengerListener(new SimplePsgDataListener() {
                        @Override
                        public void onRouteSelectSuccess() {
                            super.onRouteSelectSuccess();
                            mPassengerPanel.print("路线选择成功！");
                        }

                        @Override
                        public void onRouteSelectFail(int status, String message) {
                            super.onRouteSelectFail(status, message);
                            mPassengerPanel.print("路线选择失败！" + message);
                        }

                        @Override
                        public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
                            super.onPullLsInfoSuc(fetchedData);
                            if (fetchedData.getRoute() != null
                                    && fetchedData.getRoute().getPoints() != null
                                    && !fetchedData.getRoute().getPoints().isEmpty()) {
//                                mMapView.getMapApi().clearAllOverlays();
                                int routeSize = 1;
                                if (fetchedData.getRoutes() != null) {
                                    routeSize = fetchedData.getRoutes().size();
                                }
                                mPassengerPanel.print("拉取的路线总数：" + routeSize);
                                mPassengerPanel.postAction("绘制路线");
                            }
                        }

                        @Override
                        public void onRealSelectRouteResult(String routeId, int time, int result) {
                            super.onRealSelectRouteResult(routeId, time, result);
                            mPassengerPanel.print("路线真正选择结果：" + routeId + ", " + time + ", " + result);
                        }
                    });

                    mPassengerPanel.print("创建订单：" + order.getId());
                    mPassengerSync.getTLSPOrder().setOrderId(order.getId())
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    mPassengerPanel.postAction("开启同显");
                    mDriverPanel.postAction("绑定订单至送驾");
                    return true;
                }
                return false;
            }
        });

        mPassengerPanel.addAction("检索路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mMockSyncService.getOrder(mPassenger);
                final CountDownLatch sync = new CountDownLatch(1);
                TLSLatlng from = new TLSLatlng(order.getBegin().latitude, order.getBegin().longitude);
                TLSLatlng to = new TLSLatlng(order.getEnd().latitude, order.getEnd().longitude);
                mPassengerSync.searchRoutes(from, to, DrivingParam.Policy.TRIP,
                        new DrivingParam.Preference[]{
                                DrivingParam.Preference.REAL_TRAFFIC,
                                DrivingParam.Preference.NAV_POINT_FIRST,
                        }, new SyncProtocol.OnSearchResultListener() {
                            @Override
                            public void onSuccess(List<TLSBRoute> routeList) {
                                mTLSRoutes = routeList;
                                sync.countDown();
                            }

                            @Override
                            public void onFail(int errCode, String message) {
                                sync.countDown();
                            }
                        });

                try {
                    sync.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int routeCount = mTLSRoutes.size();
                mPassengerPanel.print("路线数量：" + routeCount);
                if (routeCount > 0) {
                    mPassengerPanel.postAction("绘制路线");
                    return true;
                }

                return false;
            }
        });

        mPassengerPanel.addAction("绘制路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerPanel.postPrint("点击地图中的路线标记选择");
                if (onTrip) {
                    mPassengerSync.getRouteManager().useRouteIndex(0);
                    return drawRoutes(mMapView.getMapApi(), mPassengerSync.getRouteManager().getRoutes());
                } else {
                    return drawRoutes(mMapView.getMapApi(), mTLSRoutes);
                }
            }
        });

        mPassengerPanel.addAction("上报路线数据", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mPassengerSync.addTLSPassengerListener(new SimplePsgDataListener() {
                    @Override
                    public void onPushRouteFail(int errCode, String errStr) {
                        super.onPushRouteFail(errCode, errStr);
                        result[0] = false;
                        sync.countDown();
                        mPassengerSync.removeTLSPassengerListener(this);
                    }

                    @Override
                    public void onPushRouteSuc() {
                        super.onPushRouteSuc();
                        result[0] = true;
                        sync.countDown();
                        mPassengerSync.removeTLSPassengerListener(this);
                    }
                });
                if (onTrip) {
                    mPassengerSync.uploadUsingRoute();
                } else {
                    mPassengerSync.uploadRoute(mTLSRoutes.get(mChosenRouteIndex));
                }
                try {
                    sync.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return result[0];
            }
        });

        mPassengerPanel.addAction("开启同显", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerSync.start();
                return true;
            }
        });
    }

    private final Streams.Callback selectCallbackOnTrip = new Streams.Callback() {
        @Override
        public void callback(Object o) {
            int integer = (Integer) o;
            String routeId = mPassengerSync.getRouteManager().getRoutes().get(integer).getRouteId();
            mPassengerSync.routeSelectByRouteId(routeId);
            mPassengerPanel.print("选择第" + integer + "条:" + routeId);
        }
    };

    private final Streams.Callback selectCallbackBeforeTrip = new Streams.Callback() {
        @Override
        public void callback(Object o) {
            mChosenRouteIndex = (Integer) o;
            mPassengerSync.uploadRoute(mTLSRoutes.get(mChosenRouteIndex));
            mPassengerPanel.print("选择第" + mChosenRouteIndex + "条:" + mTLSRoutes.get(mChosenRouteIndex).getRouteId());
        }
    };

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
        mDriverSync.init(this,
                TLSConfigPreference.create().setAccountId(mDriver.getId()).setDebuggable(true));
        mDriverSync.setNaviManager(mNaviManager);
        mDriverPanel.init("司机");
        mDriverPanel.addAction("绑定订单至接驾", new PanelView.Action<String>("") {
            @Override
            public String run() {
                mDriver.setPosition(MockSyncService.getRandomVisibleLatLng(mCarNaviView.getMapApi().getProjection()));
                MockOrder order = mMockSyncService.getOrder(mPassenger);
                order.setStatus(MockOrder.Status.Waiting);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                if (order.isWaiting()) {
                    mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    order = mMockSyncService.acceptPassenger(mDriver, mPassenger);
                    if (order.isAccepted()) {
                        mDriverSync.addTLSDriverListener(new SimpleDriDataListener() {
                            @Override
                            public void onSelectedRouteWantToChangeNotify(TLSBRoute selectedRoute) {
                                super.onSelectedRouteWantToChangeNotify(selectedRoute);
                                mDriverPanel.print("线路发生变更：" + selectedRoute.getRouteId());
                                mDriverSync.getRouteManager().useRouteId(selectedRoute.getRouteId());
                            }

                            @Override
                            public void onSelectedRouteNotFoundNotify(String selectedRouteId) {
                                super.onSelectedRouteNotFoundNotify(selectedRouteId);
                                mDriverPanel.print("线路未找到：" + selectedRouteId);
                            }
                        });
                        mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mDriverPanel.print("当前订单接驾中");
                        mDriverPanel.postAction("开启同显");
                    } else {
                        mDriverPanel.print("当前订单接驾失败");
                    }

                    return order.getId();
                }
                return "";
            }
        });

        mDriverPanel.addAction("绑定订单至送驾", new PanelView.Action<String>("") {
            @Override
            public String run() {
                mDriver.setPosition(MockSyncService.getRandomVisibleLatLng(mCarNaviView.getMapApi().getProjection()));
                MockOrder order = mMockSyncService.getOrder(mPassenger);
                order.setStatus(MockOrder.Status.Waiting);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                if (order.isWaiting()) {
                    order = mMockSyncService.acceptPassenger(mDriver, mPassenger);
                    if (order.isAccepted()) {
                        mDriverPanel.print("当前订单接驾中");

                        order = mMockSyncService.onTheWayPassenger(mDriver, mPassenger);
                        if (order.isOnTheWay()) {
                            mDriverSync.addTLSDriverListener(new SimpleDriDataListener() {
                                @Override
                                public void onSelectedRouteWantToChangeNotify(TLSBRoute selectedRoute) {
                                    super.onSelectedRouteWantToChangeNotify(selectedRoute);
                                    mDriverPanel.print("线路发生变更：" + selectedRoute.getRouteId());
                                    mDriverSync.getRouteManager().useRouteId(selectedRoute.getRouteId());
                                    mNaviManager.setMainRoute(selectedRoute.getRouteId());
                                }

                                @Override
                                public void onSelectedRouteNotFoundNotify(String selectedRouteId) {
                                    super.onSelectedRouteNotFoundNotify(selectedRouteId);
                                    mDriverPanel.print("线路未找到：" + selectedRouteId);
                                }
                            });
                            mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                            mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                            mDriverPanel.print("当前订单送驾中");
                            mDriverPanel.postAction("开启同显");
                        } else {
                            mDriverPanel.print("当前订单送驾失败");
                        }
                    } else {
                        mDriverPanel.print("当前订单接驾失败");
                    }

                    return order.getId();
                }
                return "";
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
                final MockOrder order = mMockSyncService.getOrder(mPassenger);
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<NavDriveRoute> routeData = new ArrayList<>();

                mDriverSync.pullNaviSession((webServiceRequestId, usingRouteId) -> {

                    mDriverPanel.print("WS算路请求ID[" + webServiceRequestId + "]");
                    mDriverPanel.print("待选中路线ID[" + usingRouteId + "]");
                    // 当前送驾路线
                    mDriverSync.searchCarRoutes(order.getId(),
                            ConvertUtils.toNaviPoi(order.getBegin()),
                            ConvertUtils.toNaviPoi(order.getEnd()),
                            new ArrayList<>(),
                            DriveRoutePlanOptions.Companion.newBuilder()
                                    .webServiceRequestId(webServiceRequestId)
                                    .initialRouteID(usingRouteId)
                                    .build(),
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
                });

                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mDriverPanel.print("路线总数：" + routeData.size());

                if (mDriverSync.getOrderManager().getUsingOrder().isTripStatus() && !routeData.isEmpty()) {
                    //送驾路线，默认选择0路线
                    mDriverSync.getRouteManager().useRouteIndex(0);
                    mDriverSync.uploadUsingRoute();
                    try {
                        mNaviManager.simulator().setEnable(true);
                        mNaviManager.startNavigation(mDriverSync.getRouteManager().getRouteId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (mDriverSync.getOrderManager().getUsingOrder().isPickUpStatus() && !routeData.isEmpty()) {
                    mDriverPanel.print("当前线路：" + mDriverSync.getRouteManager().getRouteId());
                    mDriverPanel.postAction("绘制路线");
                    mDriverPanel.postAction("上报路线");
                }

                return routeData.size() >= 1;
            }
        });

        mDriverPanel.addAction("绘制路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                return drawRoute(mCarNaviView.getMapApi(), mDriverSync);
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
                        mDriverSync.removeTLSDriverListener(this);
                    }
                });
                mDriverSync.uploadUsingRoute();
                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return result[0] == 1;
            }
        });

    }

    private boolean drawRoute(MapApi map, BaseSyncProtocol manager) {
        List<TLSLatlng> latlngs = manager.getRouteManager().getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }
        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
        List<LatLng> all = new ArrayList<>(mapLatLngs);
        map.addPolyline(new PolylineOptions()
                .width(25)
                .color(Color.argb(200, 0, 163, 255))
                .arrow(true)
                .addAll(mapLatLngs));

        TLSLatlng startPosition = manager.getRouteManager().getStartPosition();
        TLSLatlng destPosition = manager.getRouteManager().getDestPosition();

        if (startPosition != null) {
            map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(startPosition))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_start_point)));
            all.add(ConvertUtil.toLatLng(startPosition));
        }

        if (destPosition != null) {
            map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(destPosition))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_end_point)));
            all.add(ConvertUtil.toLatLng(destPosition));
        }
        MapUtils.fitsWithRoute(map, all,
                25, 25, 25, 25);
        return true;
    }

    List<Polyline> polylines = new ArrayList<>();

    private TencentMap.OnPolylineClickListener listener = new TencentMap.OnPolylineClickListener() {
        @Override
        public void onPolylineClick(Polyline polyline, LatLng latLng) {
            for (int i = 0; i < polylines.size(); i++) {
                Polyline the = polylines.get(i);
                if (the.getId().equals(polyline.getId())) {
                    if (onTrip) {
                        selectCallbackOnTrip.callback(i);
                    } else {
                        selectCallbackBeforeTrip.callback(i);
                    }
                } else {
                    the.remove();
                }
            }
        }
    };

    private boolean drawRoutes(MapApi map, List<TLSBRoute> tlsbRoutes) {
        map.removeOnPolylineClickListener(listener);
        List<TLSLatlng> latlngs = tlsbRoutes.get(0).getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }
        for (Polyline polyline : polylines) {
            if (polyline != null) {
                polyline.remove();
            }
        }
        polylines.clear();

        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
        List<LatLng> all = new ArrayList<>(mapLatLngs);
        polylines.add(map.addPolyline(new PolylineOptions()
                .width(25)
                .color(Color.argb(200, 0, 163, 255))
                .arrow(true)
                .zIndex(100)
                .addAll(mapLatLngs)));

        TLSLatlng startPosition = tlsbRoutes.get(0).getStartPosition();
        TLSLatlng destPosition = tlsbRoutes.get(0).getDestPosition();

        if (startPosition != null) {
            map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(startPosition))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_start_point)));
            all.add(ConvertUtil.toLatLng(startPosition));
        }

        if (destPosition != null) {
            map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(destPosition))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_end_point)));
            all.add(ConvertUtil.toLatLng(destPosition));
        }

        List<TLSBRoute> routes = new ArrayList<>(tlsbRoutes);
        if (routes.size() > 0) {
            routes.remove(0);
        }

        for (TLSBRoute route : routes) {
//            if (manager.getOrderManager().getOrderById(route.getOrderId()) != null) {
            List<LatLng> others = ConvertUtil.toLatLngList(route.getPoints());
            all.addAll(others);
            polylines.add(map.addPolyline(new PolylineOptions()
                    .width(20)
                    .arrow(true)
                    .color(Color.argb(200, 50, 203, 255))
                    .addAll(others)));
//            }
        }

        MapUtils.fitsWithRoute(map, all,
                25, 25, 25, 25);
        map.addOnPolylineClickListener(listener);
        return true;
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

        mDriverSync.destroy();
        mPassengerSync.destroy();
    }
}
