package com.tencent.mobility.synchro_v2;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.lbssearch.object.param.DrivingParam;
import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lsdriver.protocol.OrderRouteSearchOptions;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.lsp.listener.SimplePsgDataListener;
import com.tencent.map.lspassenger.protocol.SearchProtocol;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBRoute;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.protocol.RouteManager;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.map.navi.car.CarNaviView;
import com.tencent.map.navi.car.NaviMode;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.map.navi.data.RouteData;
import com.tencent.map.tools.Callback;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.synchro_v2.helper.ConvertHelper;
import com.tencent.mobility.synchro_v2.helper.SingleHelper;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.CommonUtils;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.mobility.util.GpsNavi;
import com.tencent.mobility.util.MapUtils;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.maps.model.Polyline;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * 1乘客1司机
 */
public abstract class OneDriverOnePassengerActivity extends BaseActivity {

    public static final String ACTION_SYNC_OPEN = "开启同显";
    public static final String ACTION_NAVI_SIMULATOR_OPEN = "开启模拟导航";
    public static final String ACTION_ROUTES_SEARCH = "检索路线";
    public static final String ACTION_ROUTES_PLAN = "路线规划";
    public static final String ACTION_ROUTES_RECTIFY_DEVIATION = "路线纠偏";
    public static final String ACTION_ROUTES_DRAW = "绘制路线";
    public static final String ACTION_ROUTES_UPLOAD = "上报路线数据";
    public static final String ACTION_ORDER_CREATE = "创建订单";
    public static final String ACTION_ORDER_BIND = "绑定订单";
    public static final String ACTION_ORDER_TO_TRIP = "订单到送驾";
    public static final String ACTION_ORDER_TO_PICKUP = "订单到接驾";
    private final GpsNavi gpsInfo = new GpsNavi();
    private final Map<String, Polyline> mPassengerLines = new HashMap<>();
    private final Map<String, Polyline> mDriverLines = new HashMap<>();
    private CarNaviView mCarNaviView;
    private MapView mMapView;
    private TencentCarNaviManager mNaviManager;
    private PanelView mDriverPanel;
    private PanelView mPassengerPanel;
    private TSLDExtendManager mDriverSync;
    private TSLPassengerManager mPassengerSync;
    private MockDriver mDriver;
    private MockPassenger mPassenger;
    private MockSyncService mMockSyncService;
    private Marker mPassengerStartMarker;
    private Marker mPassengerEndMarker;
    private Marker mDriverStartMarker;
    private Marker mDriverEndMarker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.one_driver_one_passenger_layout);

        mCarNaviView = findViewById(R.id.navi_car_view);
        mMapView = findViewById(R.id.map_view1);


        mDriverPanel = findViewById(R.id.group_panel_driver);
        mPassengerPanel = findViewById(R.id.group_panel_passenger);

        mMockSyncService = new MockSyncService(TLSConfigPreference.getGlobalKey(this));

        initPassengerPanel();
        initDriverPanel();
    }

    protected String[] getPassengerActionIndexes() {
        return null;
    }

    protected abstract String[] getPassengerActions();

    protected String[] getDriverActionIndexes() {
        return null;
    }

    protected abstract String[] getDriverActions();

    protected abstract void onCreatePassengerAction(final MockPassenger passenger, final TSLPassengerManager passengerSync, final PanelView passengerPanel, final MapView mapView);

    protected abstract void onCreateDriverAction(final MockDriver driver, final TSLDExtendManager driverSync, final PanelView driverPanel, final CarNaviView carNaviView);

    private void initPassengerPanel() {
        mPassenger = MockSyncService.newRandomPassenger(mMapView.getMap());
        mPassengerSync = TSLPassengerManager.newInstance();
        mPassengerSync.init(this, TLSConfigPreference.create()
                .setDebuggable(true)
                .setAccountId(mPassenger.getId()));
        mPassengerPanel.init("乘客", getPassengerActionIndexes(), getPassengerActions());
        mPassengerPanel.addAction(ACTION_ORDER_CREATE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mMockSyncService.newOrder(mMapView.getMap(), mPassenger);
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    mPassengerPanel.print("创建订单：" + order.getId());
                    mPassengerSync.getTLSPOrder().setOrderId(order.getId())
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    return true;
                }
                return false;
            }
        });

        mPassengerPanel.addAction(ACTION_ROUTES_SEARCH, new PanelView.Action<Boolean>(false) {
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
                        }, new SearchProtocol.OnSearchResultListener() {
                            @Override
                            public void onSuccess(List<TLSBRoute> routeList) {
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

                int routeCount = mPassengerSync.getRouteManager().getRoutes().size();
                mPassengerPanel.print("路线数量：" + routeCount);
                return routeCount > 0;
            }
        });

        mPassengerPanel.addAction(ACTION_ROUTES_DRAW, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                return drawRoute(mMapView.getMap(), mPassengerSync, mPassengerLines);
            }
        });

        mPassengerPanel.addAction(ACTION_ROUTES_UPLOAD, new PanelView.Action<Boolean>(false) {
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
                mPassengerSync.uploadUsingRoute();
                try {
                    sync.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return result[0];
            }
        });

        mPassengerPanel.addAction(ACTION_SYNC_OPEN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerSync.start();
                return true;
            }
        });

        onCreatePassengerAction(mPassenger, mPassengerSync, mPassengerPanel, mMapView);
    }

    private void initDriverPanel() {
        MockCar car = MockSyncService.newRandomCar();
        mDriver = MockSyncService.newRandomDriver(mCarNaviView.getMap(), car);
        mDriverSync = TSLDExtendManager.newInstance();
        mDriverSync.init(this,
                TLSConfigPreference.create()
                        .setDebuggable(true)
                        .setAccountId(mDriver.getId()));
        mNaviManager = SingleHelper.getNaviManager(this);
        mNaviManager.setMulteRoutes(true);
        mNaviManager.addNaviView(mCarNaviView);
        gpsInfo.setNavigationManager(mNaviManager);
        mCarNaviView.setNaviMapActionCallback(mNaviManager);
        mCarNaviView.setNaviMode(NaviMode.MODE_OVERVIEW);
        int margin = CommonUtils.getWidth(this) / 3;
        mCarNaviView.setEnlargedIntersectionRegionMargin(margin, 0, margin);
        mDriverSync.setNaviManager(mNaviManager);
        mDriverSync.setCarNaviView(mCarNaviView);
        mCarNaviView.setNavigationPanelVisible(false);
        mDriverPanel.init("司机", getDriverActionIndexes(), getDriverActions());

        mDriverPanel.addAction(ACTION_ORDER_BIND, new PanelView.Action<String>("") {
            @Override
            public String run() {
                MockOrder order = mMockSyncService.getOrder(mPassenger);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                return order.getId();
            }
        });

        mDriverPanel.addAction(ACTION_ORDER_TO_PICKUP, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mMockSyncService.getOrder(mPassenger);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                mMockSyncService.acceptPassenger(mDriver, mPassenger);
                if (order.isAccepted()) {
                    mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                    mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                    mDriverPanel.print("当前订单接驾中");
                    return true;
                }
                mDriverPanel.print("当前订单接驾失败");
                return false;
            }
        });

        mDriverPanel.addAction(ACTION_ORDER_TO_TRIP, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mMockSyncService.getOrder(mPassenger);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                mMockSyncService.onTheWayPassenger(mDriver, mPassenger);
                if (order.isOnTheWay()) {
                    mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                    mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                    mDriverPanel.print("当前订单送驾中");
                    return true;
                }
                mDriverPanel.print("当前订单送驾失败");
                return false;
            }
        });

        mDriverPanel.addAction(ACTION_SYNC_OPEN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.start();
                return true;
            }
        });

        mDriverPanel.addAction(ACTION_NAVI_SIMULATOR_OPEN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                try {
                    gpsInfo.enableGps(OneDriverOnePassengerActivity.this);
                    mNaviManager.startSimulateNavi(0);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        mDriverPanel.addAction(ACTION_ROUTES_RECTIFY_DEVIATION, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                TLSLatlng dest = mDriverSync.getRouteManager().getDestPosition();
                if (dest == null) {
                    return false;
                }

                mNaviManager.changeDestination(ConvertHelper.convertToNaviPoi(dest));
                return true;
            }
        });

        mDriverPanel.addAction(ACTION_ROUTES_PLAN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder order = mMockSyncService.getOrder(mPassenger);
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<RouteData> routeData = new ArrayList<>();
                // 当前送驾路线
                mDriverSync.searchCarRoutes(
                        ConvertUtils.toNaviPoi(order.getBegin()),
                        ConvertUtils.toNaviPoi(order.getEnd()),
                        new ArrayList<>(),
                        OrderRouteSearchOptions.create(order.getId()),
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

                mDriverPanel.print("路线总数：" + routeData.size());

                if (mDriverSync.getOrderManager().getUsingOrder().isTripStatus() && !routeData.isEmpty()) {
                    //送驾路线，默认选择0路线
                    mDriverSync.getRouteManager().useRouteIndex(0);
                }
                if (TextUtils.isEmpty(mDriverSync.getRouteManager().getRouteId())) {
                    mDriverPanel.postPrint("没有命中乘客选择的路线");
                } else {
                    mDriverPanel.print("当前线路：" + mDriverSync.getRouteManager().getRouteId());
                }

                return routeData.size() >= 1;
            }
        });

        mDriverPanel.addAction(ACTION_ROUTES_DRAW, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                return drawRoute(mCarNaviView.getMap(), mDriverSync, mDriverLines);
            }
        });

        mDriverPanel.addAction(ACTION_ROUTES_UPLOAD, new PanelView.Action<Boolean>(false) {
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
                mDriverSync.uploadRoutes();
                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return result[0] == 1;
            }
        });

        onCreateDriverAction(mDriver, mDriverSync, mDriverPanel, mCarNaviView);
    }

    private boolean drawRoute(TencentMap map, BaseSyncProtocol manager, Map<String, Polyline> cache) {
        List<TLSLatlng> latlngs = manager.getRouteManager().getPoints();
        clearNoUseLine(manager.getRouteManager(), cache);
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }

        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
        List<LatLng> all = new ArrayList<>(mapLatLngs);
        Polyline main = cache.get(manager.getRouteManager().getRouteId());
        if (main == null) {
            main = map.addPolyline(new PolylineOptions()
                    .width(25)
                    .color(Color.argb(200, 0, 163, 255))
                    .arrow(true)
                    .addAll(mapLatLngs));
            cache.put(manager.getRouteManager().getRouteId(), main);
        } else {
            main.setPoints(mapLatLngs);
        }
        updateStartAndEndPosition(map, manager, all);
        MapUtils.fitsWithRoute(map, all,
                25, 25, 25, 25);
        return true;
    }

    private void updateStartAndEndPosition(TencentMap map, BaseSyncProtocol manager, List<LatLng> all) {
        TLSLatlng startPosition = manager.getRouteManager().getStartPosition();
        TLSLatlng destPosition = manager.getRouteManager().getDestPosition();

        if (startPosition != null) {
            Marker startMarker = getStartMarker(manager);
            if (startMarker != null) {
                startMarker.setPosition(ConvertUtil.toLatLng(startPosition));
            } else {
                startMarker = map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(startPosition))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_start_point)));
                setStartMarker(manager, startMarker);
            }
            all.add(ConvertUtil.toLatLng(startPosition));
        }

        if (destPosition != null) {
            Marker endMarker = getEndMarker(manager);
            if (endMarker != null) {
                endMarker.setPosition(ConvertUtil.toLatLng(destPosition));
            } else {
                endMarker = map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(destPosition))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.line_end_point)));
                setEndMarker(manager, endMarker);
            }

            all.add(ConvertUtil.toLatLng(destPosition));
        }
    }

    private void setStartMarker(BaseSyncProtocol manager, Marker startMarker) {
        if (manager instanceof TSLPassengerManager) {
            mPassengerStartMarker = startMarker;
        } else {
            mDriverStartMarker = startMarker;
        }
    }

    private Marker getStartMarker(BaseSyncProtocol manager) {
        return manager instanceof TSLPassengerManager ? mPassengerStartMarker : mDriverStartMarker;
    }

    private void setEndMarker(BaseSyncProtocol manager, Marker endMarker) {
        if (manager instanceof TSLPassengerManager) {
            mPassengerEndMarker = endMarker;
        } else {
            mDriverEndMarker = endMarker;
        }
    }

    private Marker getEndMarker(BaseSyncProtocol manager) {
        return manager instanceof TSLPassengerManager ? mPassengerEndMarker : mDriverEndMarker;
    }

    private boolean drawRoutes(TencentMap map, BaseSyncProtocol manager, Map<String, Polyline> cache, Callback<Integer> selectCallback) {
        clearNoUseLine(manager.getRouteManager(), cache);

        List<TLSLatlng> latlngs = manager.getRouteManager().getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }

        List<Polyline> polylines = new ArrayList<>();
        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
        List<LatLng> allLatLngs = new ArrayList<>(mapLatLngs);
        String usingRouteId = manager.getRouteManager().getRouteId();
        Polyline mainline = cache.get(usingRouteId);
        if (mainline == null) {
            mainline = map.addPolyline(new PolylineOptions()
                    .width(25)
                    .color(Color.argb(200, 0, 163, 255))
                    .arrow(true)
                    .zIndex(100)
                    .addAll(mapLatLngs));
            cache.put(usingRouteId, mainline);
        } else {
            mainline.setPoints(mapLatLngs);
        }

        polylines.add(mainline);

        updateStartAndEndPosition(map, manager, allLatLngs);

        List<TLSBRoute> routes = new ArrayList<>(manager.getRouteManager().getRoutes());
        if (routes.size() > 0) {
            routes.remove(0);
        }

        for (TLSBRoute route : routes) {
            if (manager.getOrderManager().getOrderById(route.getOrderId()) != null) {
                List<LatLng> others = ConvertUtil.toLatLngList(route.getPoints());
                allLatLngs.addAll(others);
                Polyline subline = cache.get(route.getRouteId());
                if (subline == null) {
                    subline = map.addPolyline(new PolylineOptions()
                            .width(20)
                            .arrow(true)
                            .color(Color.argb(200, 50, 203, 255))
                            .addAll(others));
                    cache.put(route.getRouteId(), subline);
                } else {
                    subline.setPoints(others);
                }
                polylines.add(subline);
            }
        }

        MapUtils.fitsWithRoute(map, allLatLngs,
                25, 25, 25, 25);

        if (selectCallback != null) {
            map.setOnPolylineClickListener((polyline, latLng) -> {
                for (int i = 0; i < polylines.size(); i++) {
                    Polyline the = polylines.get(i);
                    if (the.getId().equals(polyline.getId())) {
                        selectCallback.callback(i);
                    } else {
                        the.remove();
                    }
                }
            });
        }

        return true;
    }

    private void clearNoUseLine(RouteManager routeManager, Map<String, Polyline> cache) {
        List<TLSBRoute> routes = routeManager.getRoutes();

        Set<String> usingIds = new HashSet<>();
        for (TLSBRoute route : routes) {
            usingIds.add(route.getRouteId());
        }
        for (Iterator<Map.Entry<String, Polyline>> entryIterator = cache.entrySet().iterator(); entryIterator.hasNext(); ) {
            Map.Entry<String, Polyline> entry = entryIterator.next();
            if (!usingIds.contains(entry.getKey())) {
                Polyline polyline = entry.getValue();
                if (polyline != null) {
                    polyline.remove();
                }
                entryIterator.remove();
            }
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
        mDriverSync.destroy();
        mNaviManager.stopSimulateNavi();
        mNaviManager.stopNavi();
        gpsInfo.disableGps();
        mCarNaviView.onDestroy();

        mPassengerSync.destroy();
        mMapView.onDestroy();
    }
}
