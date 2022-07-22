package com.tencent.mobility.synchro_v2.psg;

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
import com.tencent.map.lssupport.bean.TLSDFetchedData;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.map.navi.car.CarNaviView;
import com.tencent.map.navi.car.NaviMode;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.map.navi.data.CalcRouteResult;
import com.tencent.map.navi.data.RouteData;
import com.tencent.map.tools.Callback;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.mobility.util.MapUtils;
import com.tencent.mobility.util.SingleHelper;
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

    private CarNaviView mCarNaviView;
    private MapView mMapView;

    private PanelView mDriverPanel;
    private PanelView mPassengerPanel;

    private TSLDExtendManager mDriverSync;
    private TSLPassengerManager mPassengerSync;

    private MockDriver mDriver;
    private MockPassenger mPassenger;

    private MockSyncService mMockSyncService;
    private boolean mPassengerDrawMultiRoutes;
    private boolean mPassengerDrawRoute;
    private TencentCarNaviManager mNaviManager;
    private List<TLSBRoute> mTLSRoutes;
    private int mChosenRouteIndex = 0;
    private boolean onTrip = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.one_driver_one_passenger_layout);

        mCarNaviView = findViewById(R.id.navi_car_view);
        mMapView = findViewById(R.id.map_view1);

        mDriverPanel = findViewById(R.id.group_panel_driver);
        mPassengerPanel = findViewById(R.id.group_panel_passenger);

        initPassengerPanel();
        initDriverPanel();

        mMockSyncService = new MockSyncService(mDriverSync);
    }

    private void initPassengerPanel() {
        mPassenger = MockSyncService.newRandomPassenger(mMapView.getMap());
        mPassengerSync = TSLPassengerManager.newInstance();
        mPassengerSync.init(this, TLSConfigPreference.create()
                .setAccountId(mPassenger.getId()));
        mPassengerPanel.init("乘客", "行前选路", "行中选路");
        mPassengerPanel.addAction("行前选路", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                onTrip = false;
                MockOrder order = mMockSyncService.newOrder(mMapView.getMap(), mPassenger);
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
                        public void onRouteSelectFail(int status, String message) {
                            super.onRouteSelectFail(status, message);
                            mPassengerPanel.print("路线选择失败！" + message);
                        }

                        @Override
                        public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
                            super.onPullLsInfoSuc(fetchedData);
                            if (fetchedData.getRoute() != null
                                    && !mPassengerDrawRoute
                                    && fetchedData.getRoute().getPoints() != null
                                    && !fetchedData.getRoute().getPoints().isEmpty()) {
                                mMapView.getMap().clearAllOverlays();
                                int routeSize = 1;
                                if (fetchedData.getRoutes() != null) {
                                    routeSize = fetchedData.getRoutes().size();
                                }
                                mPassengerPanel.print("拉取的路线总数：" + routeSize);
                                drawRoute(mMapView.getMap(), mPassengerSync);
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
                MockOrder order = mMockSyncService.newOrder(mMapView.getMap(), mPassenger);
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
                                    && !mPassengerDrawMultiRoutes
                                    && fetchedData.getRoute().getPoints() != null
                                    && !fetchedData.getRoute().getPoints().isEmpty()) {
                                mMapView.getMap().clearAllOverlays();
                                int routeSize = 1;
                                if (fetchedData.getRoutes() != null) {
                                    routeSize = fetchedData.getRoutes().size();
                                }
                                mPassengerPanel.print("拉取的路线总数：" + routeSize);
                                mPassengerPanel.postAction("绘制路线");
                                mPassengerDrawMultiRoutes = true;
                            }
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
                        }, new SearchProtocol.OnSearchResultListener() {
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
                    return drawRoutes(mMapView.getMap(), mPassengerSync.getRouteManager().getRoutes(), integer -> {
                        mPassengerSync.routeSelectByIndex(integer);
                        mPassengerPanel.print("选择第" + integer + "条:" + mPassengerSync.getRouteManager().getRouteId());
                    });
                } else {
                    return drawRoutes(mMapView.getMap(), mTLSRoutes, integer -> {
                        mChosenRouteIndex = integer;
                        mPassengerSync.uploadRoute(mTLSRoutes.get(mChosenRouteIndex));
                        mPassengerPanel.print("选择第" + integer + "条:" + mTLSRoutes.get(mChosenRouteIndex).getRouteId());

                    });
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

    private void initDriverPanel() {
        mNaviManager = SingleHelper.getNaviManager(this);
        mNaviManager.setMulteRoutes(true);
        mNaviManager.addNaviView(mCarNaviView);
        mCarNaviView.setNaviMapActionCallback(mNaviManager);
        mCarNaviView.setNaviMode(NaviMode.MODE_OVERVIEW);
        MockCar car = MockSyncService.newRandomCar();
        mDriver = MockSyncService.newRandomDriver(mCarNaviView.getMap(), car);
        mDriverSync = TSLDExtendManager.newInstance();
        mDriverSync.init(this,
                TLSConfigPreference.create().setAccountId(mDriver.getId()).setDebuggable(true));
        mDriverSync.setNaviManager(mNaviManager);
        mDriverSync.setCarNaviView(mCarNaviView);
        mCarNaviView.setNavigationPanelVisible(false);
        mDriverPanel.init("司机");
        mDriverPanel.addAction("绑定订单至接驾", new PanelView.Action<String>("") {
            @Override
            public String run() {
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
                MockOrder order = mMockSyncService.getOrder(mPassenger);
                order.setStatus(MockOrder.Status.Waiting);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                if (order.isWaiting()) {
                    mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    order = mMockSyncService.acceptPassenger(mDriver, mPassenger);
                    if (order.isAccepted()) {
                        mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                        mDriverPanel.print("当前订单接驾中");

                        order = mMockSyncService.onTheWayPassenger(mDriver, mPassenger);
                        if (order.isOnTheWay()) {
                            mDriverSync.addTLSDriverListener(new SimpleDriDataListener() {
                                @Override
                                public void onSelectedRouteWantToChangeNotify(TLSBRoute selectedRoute) {
                                    super.onSelectedRouteWantToChangeNotify(selectedRoute);
                                    mDriverPanel.print("线路发生变更：" + selectedRoute.getRouteId());
                                    mDriverSync.getRouteManager().useRouteId(selectedRoute.getRouteId());
                                    mNaviManager.changeToFollowedRoute(selectedRoute.getRouteId());
                                    mDriverPanel.postAction("绘制路线");
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
                mDriverPanel.postAction("请求路线");
                mDriverSync.start();
                return true;
            }
        });

        mDriverPanel.addAction("请求路线", new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder order = mMockSyncService.getOrder(mPassenger);
                final CountDownLatch syncWaiting = new CountDownLatch(1);
                final List<RouteData> routeData = new ArrayList<>();

                mDriverSync.pullNaviSession((naviSession, usingRouteId) -> {

                    mDriverPanel.print("导航会话ID[" + naviSession + "]");
                    mDriverPanel.print("待选中路线ID[" + usingRouteId + "]");
                    // 当前送驾路线
                    mDriverSync.searchCarRoutes(
                            ConvertUtils.toNaviPoi(order.getBegin()),
                            ConvertUtils.toNaviPoi(order.getEnd()),
                            new ArrayList<>(),
                            OrderRouteSearchOptions
                                    .create(order.getId())
                                    .setInitialNaviSessionID(naviSession)
                                    .setInitialRouteID(usingRouteId),
                            new DriDataListener.ISearchCallBack() {
                                @Override
                                public void onCalcRouteSuccess(CalcRouteResult calcRouteResult) {
                                    List<RouteData> arrayList = calcRouteResult.getRoutes();
                                    if (arrayList != null && !arrayList.isEmpty()) {
                                        routeData.addAll(arrayList);
                                    }
                                    syncWaiting.countDown();
                                }

                                @Override
                                public void onCalcRouteFailure(CalcRouteResult calcRouteResult) {
                                    syncWaiting.countDown();
                                }

                                @Override
                                public void onParamsInvalid(int errCode, String errMsg) {
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
                    try {
                        mNaviManager.startSimulateNavi(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (TextUtils.isEmpty(mDriverSync.getRouteManager().getRouteId())) {
                    mDriverPanel.postPrint("没有命中乘客选择的路线");
                } else {
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
                mCarNaviView.getMap().clearAllOverlays();
                return drawRoute(mCarNaviView.getMap(), mDriverSync);
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
                mDriverSync.uploadRoutes();
                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return result[0] == 1;
            }
        });

    }

    private boolean drawRoute(TencentMap map, BaseSyncProtocol manager) {
        List<TLSLatlng> latlngs = manager.getRouteManager().getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }

        List<Polyline> polylines = new ArrayList<>();
        List<LatLng> mapLatLngs = ConvertUtil.toLatLngList(latlngs);
        List<LatLng> all = new ArrayList<>(mapLatLngs);
        polylines.add(map.addPolyline(new PolylineOptions()
                .width(25)
                .color(Color.argb(200, 0, 163, 255))
                .arrow(true)
                .addAll(mapLatLngs)));

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

    private boolean drawRoutes(TencentMap map, List<TLSBRoute> tlsbRoutes, Callback<Integer> selectCallback) {
        List<TLSLatlng> latlngs = tlsbRoutes.get(0).getPoints();
        if (latlngs == null || latlngs.isEmpty()) {
            return false;
        }

        List<Polyline> polylines = new ArrayList<>();
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
        mNaviManager.stopSimulateNavi();
        mNaviManager.stopNavi();
        mNaviManager.removeNaviView(mCarNaviView);
        mCarNaviView.onDestroy();
        mMapView.onDestroy();

        mDriverSync.destroy();
        mPassengerSync.destroy();
    }
}
