package com.tencent.mobility.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.tencent.lbssearch.object.param.DrivingParam;
import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lsdriver.protocol.OrderRouteSearchOptions;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.anima.MarkerTranslateAnimator;
import com.tencent.map.lspassenger.lsp.listener.SimplePsgDataListener;
import com.tencent.map.lspassenger.protocol.SearchProtocol;
import com.tencent.map.lssupport.bean.TLSBDriverPosition;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBOrderType;
import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.lssupport.bean.TLSBRoute;
import com.tencent.map.lssupport.bean.TLSBRouteTrafficItem;
import com.tencent.map.lssupport.bean.TLSBWayPoint;
import com.tencent.map.lssupport.bean.TLSBWayPointType;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSDDrvierStatus;
import com.tencent.map.lssupport.bean.TLSDFetchedData;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.protocol.BaseSyncProtocol;
import com.tencent.map.lssupport.protocol.RouteManager;
import com.tencent.map.lssupport.protocol.SyncProtocol;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.map.navi.car.CarNaviView;
import com.tencent.map.navi.car.NaviMode;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.map.navi.data.CalcRouteResult;
import com.tencent.map.navi.data.NaviPoi;
import com.tencent.map.navi.data.RouteData;
import com.tencent.map.navi.ui.car.CarNaviInfoPanel;
import com.tencent.map.tools.Callback;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.util.Configs;
import com.tencent.mobility.util.CommonUtils;
import com.tencent.mobility.util.ConvertUtils;
import com.tencent.mobility.util.MapUtils;
import com.tencent.mobility.util.SingleHelper;
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
    public static final String ACTION_SYNC_CLOSE = "关闭同显";

    public static final String ACTION_ROUTES_SEARCH = "检索路线";
    public static final String ACTION_ROUTES_PLAN = "路线规划";
    public static final String ACTION_ROUTES_RECTIFY_DEVIATION = "路线纠偏";
    public static final String ACTION_ROUTES_DRAW = "绘制路线";
    public static final String ACTION_ROUTES_UPLOAD = "上报路线数据";
    public static final String ACTION_UPLOAD_POSITION = "上报空定位点";

    public static final String ACTION_PULL = "拉取数据";

    public static final String ACTION_ORDER_CREATE = "创建订单";
    public static final String ACTION_ORDER_BIND = "绑定订单";
    public static final String ACTION_ORDER_TO_TRIP = "订单到送驾";
    public static final String ACTION_ORDER_TO_PICKUP = "订单到接驾";

    public static final String ACTION_NAVI_SIMULATOR_OPEN = "开启模拟导航";
    public static final String ACTION_NAVI_OPEN = "开启导航";
    public static final String ACTION_NAVI_SIMULATOR_CLOSE = "关闭模拟导航";
    public static final String ACTION_NAVI_CLOSE = "关闭导航";

    public static final String ACTION_ARRIVED_GETON = "到达上车点";
    public static final String ACTION_ARRIVED_GETOFF = "到达下车点";

    public static final String ACTION_ORDER_STATUS_SYNC = "订单同步";

    public static final String ACTION_ORDER_SYNC_ORDER_CREATE = "订单同步-创建订单";
    public static final String ACTION_ORDER_SYNC_SET_OFF = "订单同步-转为接驾";
    public static final String ACTION_ORDER_SYNC_ON_TRIP = "订单同步-转为送驾";
    public static final String ACTION_ORDER_SYNC_FINISH = "订单同步-结束";
    public static final String ACTION_ORDER_SYNC_CANCEL = "订单同步-取消";

    public static final String ACTION_ORDER_SYNC_CARPOOLING_CREATE = "订单同步-拼车-创建订单";
    public static final String ACTION_ORDER_SYNC_CARPOOLING_DISPATCH = "订单同步-拼车-派单";
    public static final String ACTION_ORDER_SYNC_CARPOOLING_ARRIVED = "订单同步-拼车-到达乘车点";
    public static final String ACTION_ORDER_SYNC_CARPOOLING_SET_OFF = "订单同步-拼车-开始送驾";
    public static final String ACTION_ORDER_SYNC_CARPOOLING_FINISH = "订单同步-拼车-结束";
    public static final String ACTION_ORDER_SYNC_CARPOOLING_CANCEL_SINGLE = "订单同步-拼车-取消单个";
    public static final String ACTION_ORDER_SYNC_CARPOOLING_CANCEL_ALL = "订单同步-拼车-取消全部";
    public static final String ACTION_ORDER_SYNC_CARPOOLING_OVER = "订单同步-拼车-终止";
    public static final String ACTION_CARPOOLING_PLAN = "拼车-算路";

    private final Map<String, Polyline> mPassengerLines = new HashMap<>();
    private final Map<String, Polyline> mDriverLines = new HashMap<>();
    private final List<Marker> mPassengerWayMarkers = new ArrayList<>(20);
    private final List<Marker> mDriverWayMarkers = new ArrayList<>(20);
    private final List<Marker> driverList = new ArrayList<>(20);
    protected PassengerInfo mPassengerInfo;
    protected DriverInfo mDriverInfo;
    private CarNaviView mCarNaviView;
    private MapView mMapView;
    private TencentCarNaviManager mNaviManager;
    private PanelView mDriverPanel;
    private PanelView mPassengerPanel;
    private TSLDExtendManager mDriverSync;
    private final SimpleDriDataListener driDataListener = new SimpleDriDataListener() {

        @Override
        public void onSelectedRouteWantToChangeNotify(TLSBRoute selectedRoute) {
            super.onSelectedRouteWantToChangeNotify(selectedRoute);
            Log.d(Configs.TAG, "司机onSelectedRouteWantToChangeNotify" + "  " + Thread.currentThread().getName());
            mDriverPanel.print("线路发生变更：" + selectedRoute.getRouteId());
            Log.d(Configs.TAG, "偏航类型: " + selectedRoute.getRecalculateType() + "   剩余时间: " +
                    selectedRoute.getRemainingTime() + "   剩余距离: " + selectedRoute.getRemainingDistance() +
                    "   剩余红绿灯: " + selectedRoute.getRemainingTrafficCount());
            mDriverSync.getRouteManager().useRouteId(selectedRoute.getRouteId());
//            List<TLSBRoute> temp = new ArrayList<>(1);
//            temp.add(selectedRoute);
//            drawRoutes(mCarNaviView.getMap(), temp, true, null);
//            if (tencentCarNaviManager.isNavigating()) {
//                tencentCarNaviManager.changeToFollowedRoute(selectedRoute.getRouteId());
//            }
        }

        @Override
        public void onSelectedRouteNotFoundNotify(String selectedRouteId) {
            super.onSelectedRouteNotFoundNotify(selectedRouteId);
            Log.d(Configs.TAG, "司机onSelectedRouteNotFoundNotify" + "  " + Thread.currentThread().getName());
            mDriverPanel.print("没有匹配到对应路线");
        }

        @Override
        public void onPullLsInfoSuc(List<TLSBPosition> los) {
            if (los != null && !los.isEmpty()) {
                for (TLSBPosition position : los) {
                    if (position.getMockGPS() == 1) {
                        Log.e(Configs.TAG, "乘客位置出现惯导吸附点!!!!!!!!!!");
                    }
                }
                for (Marker marker : driverList) {
                    marker.remove();
                }
                driverList.clear();
                for (TLSBPosition position : los) {
                    Marker marker = mCarNaviView.getMap().addMarker(
                            new MarkerOptions(new LatLng(position.getLatitude(), position.getLongitude()))
                                    .title(position.getExtraInfo()));
                    marker.showInfoWindow();
                    driverList.add(marker);
                }
                Log.d(Configs.TAG, "拉取乘客位置成功：" + los.size());
            }
        }

        @Override
        public void onPullLsInfoFail(int errCode, String errMsg) {
            mDriverPanel.postPrint("拉取乘客位置失败：" + errCode + " " + errMsg);
        }

        @Override
        public void onNewDestinationNotify(final TLSLatlng newDest, final long changedTime) {
            mDriverPanel.postPrint("新目的地通知:" + changedTime);
//            showNewDest(new LatLng(newDest.getLatitude(), newDest.getLongitude()), true);
            mDriverSync.getRouteManager().editCurrent().setDestPosition(newDest).setDestPositionChanged(true);
//            tencentCarNaviManager.changeDestination(new NaviPoi(newDest.getLatitude(), newDest.getLongitude(), newDest.getPoiId()));
        }

        @Override
        public void onDestinationChangeResult(final int status, final String message) {
            mDriverPanel.postPrint("目的地修改:" + status + "  " + message);
        }
    };
    private TSLPassengerManager mPassengerSync;
    private MockDriver mDriver;
    private MockPassenger mPassenger;
    private Marker mPassengerStartMarker;
    private Marker mPassengerEndMarker;
    private Marker mDriverStartMarker;
    private Marker mDriverEndMarker;
    private Marker psg = null;
    private final SimplePsgDataListener psgDataListener = new SimplePsgDataListener() {
        @Override
        public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
            Log.d(Configs.TAG, "乘客onPullLsInfoSuc" + "  " + fetchedData.getPositions().size()
                    + "  " + Thread.currentThread().getName());
            if (fetchedData != null) {
                if (fetchedData.getPositions() != null) {
                    for (TLSBDriverPosition position : fetchedData.getPositions()) {
                        if (position.getMockGPS() == 1) {
                            Log.e(Configs.TAG, "惯导吸附点:" + position.getPointIndex() + "  " +
                                    position.getRemainingDistance() + "  " + position.getRemainingTime());
                        }
                    }
                    showLocation(mMapView.getMap(), fetchedData.getPositions(), fetchedData.getDriverPosition());
                }
                if (fetchedData.getRoutes() != null && !fetchedData.getRoutes().isEmpty()) {
                    Log.d(Configs.TAG, "乘客拉取路线:" + fetchedData.getRoutes().size() + "  " +
                            fetchedData.getRoute().getRouteId());
                    Log.d(Configs.TAG, "剩余" + fetchedData.getRoute().getRemainingDistance() / 1000.0
                            + "公里  " + fetchedData.getRoute().getRemainingTime() + "分钟  "
                            + fetchedData.getRoute().getRemainingTrafficCount() + "红绿灯");
                    Log.d(Configs.TAG, "偏航类型:" + fetchedData.getRoute().getRecalculateType());
                    int startIndex = fetchedData.getDriverPosition() == null ? 0 : fetchedData.getDriverPosition()
                            .getPointIndex();
                    startIndex = (startIndex == -1 ? 0 : startIndex);
                    if (mPassengerInfo.mMockSyncService.getOrder(mPassenger).isOnTheWay()) {
                        Log.d(Configs.TAG, "送驾中");
                    } else {
                        Log.d(Configs.TAG, "接驾中");
                    }
                    if (fetchedData.getGetOffWayPoint() != null) {
                        TLSBRoute subRoute = RouteManager.subRouteByRange(fetchedData.getRoute(), startIndex,
                                fetchedData.getGetOffWayPoint().getPointIndex());
                        mPassengerSync.getRouteManager().editCurrent().copy(subRoute);
                        Log.d(Configs.TAG, "下车点：" + fetchedData.getGetOffWayPoint().getPointIndex() + "  " +
                                fetchedData.getGetOffWayPoint().getRemainingDistance() / 1000.0 + "公里" + "  " +
                                fetchedData.getGetOffWayPoint().getRemainingTime() + "分钟");
                        if (!isLatLngSame(fetchedData.getGetOffWayPoint().getPosition(),
                                mPassengerInfo.mMockSyncService.getOrder(mPassenger).getEnd())) {
                            Log.d(Configs.TAG, "下车点与乘客终点不符：" + fetchedData.getGetOffWayPoint().getPosition().toString() + "  "
                                    + mPassengerInfo.mMockSyncService.getOrder(mPassenger).getEnd().toString());
                        }
                    }
                    if (fetchedData.getGetInWayPoint() != null) {
                        TLSBRoute subRoute = RouteManager.subRouteByRange(fetchedData.getRoute(), startIndex,
                                fetchedData.getGetInWayPoint().getPointIndex());
                        mPassengerSync.getRouteManager().editCurrent().copy(subRoute);
                        Log.d(Configs.TAG, "上车点：" + fetchedData.getGetInWayPoint().getPointIndex() + "  " +
                                fetchedData.getGetInWayPoint().getRemainingDistance() / 1000.0 + "公里" + "  " +
                                fetchedData.getGetInWayPoint().getRemainingTime() + "分钟");
                        if (!isLatLngSame(fetchedData.getGetInWayPoint().getPosition(),
                                mPassengerInfo.mMockSyncService.getOrder(mPassenger).getBegin())) {
                            Log.d(Configs.TAG, "上车点与乘客终点不符：" + fetchedData.getGetInWayPoint().getPosition().toString() + "  "
                                    + mPassengerInfo.mMockSyncService.getOrder(mPassenger).getBegin().toString());
                        }
                    }
                    mPassengerPanel.postAction(ACTION_ROUTES_DRAW);
                }
            } else {
                Log.d(Configs.TAG, "乘客拉取数据为空");
            }
        }

        @Override
        public void onPullLsInfoFail(int errCode, String errMsg) {
            Log.d(Configs.TAG, "乘客onPullLsInfoFail" + "  " + Thread.currentThread().getName() + "  " +
                    errCode + " " + errMsg);
            mPassengerPanel.postPrint("拉取失败:" + errCode + " " + errMsg);
        }

        @Override
        public void onRouteSelectSuccess() {
            Log.d(Configs.TAG, "乘客onRouteSelectSuccess" + "  " + Thread.currentThread().getName());
            mPassengerPanel.postPrint("路线选择成功");
        }

        @Override
        public void onRouteSelectFail(int status, String message) {
            Log.d(Configs.TAG, "乘客onRouteSelectFail" + "  " + Thread.currentThread().getName());
            mPassengerPanel.postPrint("路线选择失败:" + status + " " + message);
        }

        @Override
        public void onNewDestinationNotify(final TLSLatlng newDest, final long changedTime) {
            Log.d(Configs.TAG, "乘客onNewDestinationNotify" + "  " + Thread.currentThread().getName());
            mPassengerPanel.postPrint("新目的地通知:" + changedTime);
//            showNewDest(new LatLng(newDest.getLatitude(), newDest.getLongitude()), false);
        }

        @Override
        public void onDestinationChangeResult(final int status, final String message) {
            Log.d(Configs.TAG, "乘客onDestinationChangeResult" + "  " + Thread.currentThread().getName());
            mPassengerPanel.postPrint("目的地修改:" + status + "  " + message);
        }

        @Override
        public void onWayPointPassed(List<TLSBWayPoint> wayPoints) {
            Log.d(Configs.TAG, "乘客onWayPointPassed" + "  " + Thread.currentThread().getName());
            mPassengerPanel.postPrint("经过途径点:" + wayPoints);
        }
    };

    protected int getLayoutResId() {
        return R.layout.one_driver_one_passenger_layout;
    }

    public MockPassenger getPassenger() {
        return mPassenger;
    }

    protected String getDriverName() {
        return "司机";
    }

    protected String getPassengerName() {
        return "乘客";
    }

    protected void changePassenger(PassengerInfo passengerInfo) {
        mPassenger = passengerInfo.mPassenger;
        mPassengerSync = passengerInfo.mPassengerSync;
        runOnUiThread(this::initPassengerPanel);
    }

    protected String[] getPassengerActionIndexes() {
        return null;
    }

    protected abstract String[] getPassengerActions();

    protected String[] getDriverActionIndexes() {
        return null;
    }

    protected abstract String[] getDriverActions();

    protected abstract void onCreatePassengerAction(final MockPassenger passenger,
                                                    final TSLPassengerManager passengerSync,
                                                    final PanelView passengerPanel, final MapView mapView);

    protected abstract void onCreateDriverAction(final MockDriver driver, final TSLDExtendManager driverSync,
                                                 final PanelView driverPanel, final CarNaviView carNaviView,
                                                 final TencentCarNaviManager manager);

    protected PassengerInfo createPassenger() {
        mPassenger = MockSyncService.newRandomPassenger(mMapView.getMap());
        mPassengerSync = TSLPassengerManager.newInstance();
        mPassengerSync.init(this, TLSConfigPreference.create()
                .setDebuggable(true)
                .setAccountId(mPassenger.getId())
                .setDeviceId("device2"));
        return new PassengerInfo(mPassenger, mPassengerSync);
    }

    String fastCarOrderId = "";
    List<String> carpoolingOrderIds = new ArrayList<>();

    private void initPassengerPanel() {
        mPassengerPanel.init(getPassengerName(), getPassengerActionIndexes(), getPassengerActions());
        mPassengerPanel.addAction(ACTION_ORDER_CREATE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassenger.setPosition(MockSyncService.getRandomVisibleLatLng(mMapView.getMap()));
                MockOrder order = mPassengerInfo.mMockSyncService.newOrder(mMapView.getMap(), mPassenger);
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    mPassengerPanel.print("创建订单：" + order.getId());
                    MockOrder driverOrder = mDriverInfo.mMockSyncService.getOrder(mDriver);
                    if (driverOrder != null) {
                        mPassengerSync.getTLSPOrder().setOrderId(driverOrder.getId())
                                .setSubOrderId(order.getId())
                                .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    } else {
                        mPassengerSync.getTLSPOrder().setOrderId(order.getId())
                                .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    }
                    return true;
                }
                return false;
            }
        });

        mPassengerPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_CREATE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mPassengerInfo.mMockSyncService.newOrderLocal(mMapView.getMap(), mPassenger);
                carpoolingOrderIds.add(order.getId());
                mPassengerSync.getOrderManager().editCurrent()
                        .setSubOrderId(order.getId())
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusInit);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 0);
                requestMap.put("passenger_orderid", order.getId());
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mPassengerSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mPassengerPanel.addAction(ACTION_ORDER_SYNC_ORDER_CREATE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mPassengerInfo.mMockSyncService.newOrderLocal(mMapView.getMap(), mPassenger);
                fastCarOrderId = order.getId();
                mPassengerSync.getOrderManager().editCurrent()
                        .setOrderId(order.getId())
                        .setOrderType(TLSBOrderType.TLSDOrderTypeNormal)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusInit);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 1);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mPassengerSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mPassengerPanel.addAction(ACTION_ROUTES_SEARCH, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mPassengerInfo.mMockSyncService.getOrder(mPassenger);
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

        mPassengerPanel.addAction(ACTION_SYNC_CLOSE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerSync.stop();
                return true;
            }
        });

        mPassengerPanel.addAction(ACTION_PULL, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerSync.addTLSPassengerListener(psgDataListener);
                return true;
            }
        });

        mPassengerPanel.addAction(ACTION_ARRIVED_GETON, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder mockOrder = mPassengerInfo.mMockSyncService.getOrder(mPassenger);
                if (mockOrder == null) {
                    return false;
                }
                mDriverSync.arrivedPassengerStartPoint(mockOrder.getId());
                return true;
            }
        });

        mPassengerPanel.addAction(ACTION_ARRIVED_GETOFF, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder mockOrder = mPassengerInfo.mMockSyncService.getOrder(mPassenger);
                if (mockOrder == null) {
                    return false;
                }
                mDriverSync.arrivedPassengerEndPoint(mockOrder.getId());
                return true;
            }
        });

        mPassengerPanel.addAction(ACTION_ORDER_STATUS_SYNC, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mPassengerSync.orderStatusSync(new HashMap<>(), new SyncProtocol.OrderResultListener() {
                    @Override
                    public void onResult(int status, String message) {
                        Log.i(Configs.TAG, "status: " + status + ", message: " + message);
                    }
                });
                return true;
            }
        });

        mPassengerPanel.addAction(ACTION_ORDER_TO_PICKUP, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mPassengerInfo.mMockSyncService.getOrder(mPassenger);
                if (order != null) {
                    mPassengerSync.getOrderManager().editCurrent()
                            .setOrderId(order.getId())
                            .setDrvierStatus(TLSDDrvierStatus.TLSDDrvierStatusServing)
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                    mPassengerPanel.print("当前订单接驾中");
                    return true;
                }
                return false;
            }
        });

        mPassengerPanel.addAction(ACTION_ORDER_TO_TRIP, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mPassengerInfo.mMockSyncService.getOrder(mPassenger);
                if (order != null) {
                    mPassengerSync.getOrderManager().editCurrent()
                            .setOrderId(order.getId())
                            .setDrvierStatus(TLSDDrvierStatus.TLSDDrvierStatusServing)
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                    mPassengerPanel.print("当前订单送驾中");
                    return true;
                }
                return false;
            }
        });

        onCreatePassengerAction(mPassenger, mPassengerSync, mPassengerPanel, mMapView);
    }

    private void initDriverPanel() {
        mNaviManager = SingleHelper.getNaviManager(this);
        mNaviManager.setMulteRoutes(true);
        mNaviManager.addNaviView(mCarNaviView);
        mCarNaviView.setNaviMapActionCallback(mNaviManager);
        mCarNaviView.setNaviMode(NaviMode.MODE_OVERVIEW);
        // 使用默认UI
        CarNaviInfoPanel carNaviInfoPanel = mCarNaviView.showNaviInfoPanel();
        // 控制默认 UI 内的控件的显示和隐藏
        if (carNaviInfoPanel != null) {
            CarNaviInfoPanel.NaviInfoPanelConfig config = new CarNaviInfoPanel.NaviInfoPanelConfig();
            config.setRerouteViewEnable(true);
            carNaviInfoPanel.setNaviInfoPanelConfig(config);
        }
        int margin = CommonUtils.getWidth(this) / 3;
        mCarNaviView.setEnlargedIntersectionRegionMargin(margin, 0, margin);
        mDriverSync.setNaviManager(mNaviManager);
        mDriverSync.setCarNaviView(mCarNaviView);
        mCarNaviView.setNavigationPanelVisible(false);
        mDriverPanel.init(getDriverName(), getDriverActionIndexes(), getDriverActions());

        mDriverPanel.addAction(ACTION_UPLOAD_POSITION, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.uploadPosition(new TLSBPosition());
                return true;
            }
        });

        mDriverPanel.addAction(ACTION_ORDER_STATUS_SYNC, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.orderStatusSync(new HashMap<>(), new SyncProtocol.OrderResultListener() {
                    @Override
                    public void onResult(int status, String message) {
                        Log.i(Configs.TAG, "status: " + status + ", message: " + message);
                    }
                });
                return true;
            }
        });

        mDriverPanel.addAction(ACTION_ORDER_CREATE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriver.setPosition(MockSyncService.getRandomVisibleLatLng(mCarNaviView.getMap()));
                MockOrder order = mDriverInfo.mMockSyncService.newOrder(mMapView.getMap(), mDriver);
                if (order != null && !TextUtils.isEmpty(order.getId())) {
                    mDriverPanel.print("创建订单：" + order.getId());
                    mDriverSync.getTLSBOrder().setOrderId(order.getId())
                            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                    return true;
                }
                return false;
            }
        });

        mDriverPanel.addAction(ACTION_ORDER_SYNC_SET_OFF, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderId(fastCarOrderId)
                        .setOrderType(TLSBOrderType.TLSDOrderTypeNormal)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 2);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_ON_TRIP ,new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderId(fastCarOrderId)
                        .setOrderType(TLSBOrderType.TLSDOrderTypeNormal)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 3);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_FINISH ,new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderId(fastCarOrderId)
                        .setOrderType(TLSBOrderType.TLSDOrderTypeNormal)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 6);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CANCEL ,new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderId(fastCarOrderId)
                        .setOrderType(TLSBOrderType.TLSDOrderTypeNormal)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 4);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_CREATE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mDriverInfo.mMockSyncService.newOrderLocal(mMapView.getMap(), mDriver);
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderId(order.getId())
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                mPassengerSync.getOrderManager().editCurrent()
                        .setOrderId(order.getId());
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 2);
                requestMap.put("passenger_orderid", carpoolingOrderIds.get(0));
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
                    @Override
                    public void onResult(int status, String message) {
                        result[0] = true;
                        Log.i(Configs.TAG, "status: " + status + ", message: " + message);
                        mDriverPanel.print("派单给司机前需创建好第二个乘客");
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_DISPATCH, new PanelView.Action<Boolean>(false) {

            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                mPassengerSync.getOrderManager().editCurrent()
                        .setOrderId(mDriverSync.getOrderManager().getOrderId());
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 3);
                requestMap.put("passenger_orderid", carpoolingOrderIds.get(1));
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_ARRIVED, new PanelView.Action<Boolean>(false) {

            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 5);
                requestMap.put("passenger_orderid", carpoolingOrderIds.get(0));
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_SET_OFF, new PanelView.Action<Boolean>(false) {

            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 6);
                requestMap.put("passenger_orderid", carpoolingOrderIds.get(0));
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_FINISH, new PanelView.Action<Boolean>(false) {

            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusInit);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 7);
                requestMap.put("passenger_orderid", carpoolingOrderIds.get(0));
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_CANCEL_SINGLE, new PanelView.Action<Boolean>(false) {

            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 4);
                requestMap.put("passenger_orderid", carpoolingOrderIds.get(0));
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_CANCEL_ALL, new PanelView.Action<Boolean>(false) {

            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 8);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_ORDER_SYNC_CARPOOLING_OVER, new PanelView.Action<Boolean>(false) {

            @Override
            public Boolean run() {
                mDriverSync.getOrderManager().editCurrent()
                        .setOrderType(TLSBOrderType.TLSBOrderTypeRidesharing)
                        .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("city", "110000");
                requestMap.put("status", 9);
                final CountDownLatch sync = new CountDownLatch(1);
                final Boolean[] result = new Boolean[1];
                mDriverSync.orderStatusSync(requestMap, new SyncProtocol.OrderResultListener() {
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

        mDriverPanel.addAction(ACTION_CARPOOLING_PLAN, new PanelView.Action<Boolean>(false) {

            @Override
            public Boolean run() {
                final MockOrder order = mDriverInfo.mMockSyncService.getOrder(mDriver);
                if (order == null) {
                    return false;
                }
                List<TLSDWayPointInfo> ws = new ArrayList<>();

                for (String orderId : carpoolingOrderIds) {
                    MockOrder subOrder = mDriverInfo.mMockSyncService.getOrder(orderId);
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
                mDriverSync.requestBestSortedWayPoints(
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
                final List<RouteData> routeData = new ArrayList<>();
                mDriverSync.searchCarRoutes(
                        ConvertUtils.toNaviPoi(order.getBegin()),
                        ws,
                        OrderRouteSearchOptions.create(order.getId()),
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

                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mDriverPanel.print("路线总数：" + routeData.size());

                if (!routeData.isEmpty()) {
                    //送驾路线，默认选择0路线
                    mDriverSync.getRouteManager().useRouteIndex(0);
                }

                if (TextUtils.isEmpty(mDriverSync.getRouteManager().getRouteId())) {
                    mDriverPanel.postPrint("没有命中乘客选择的路线");
                } else {
                    mDriverPanel.print("当前线路：" + mDriverSync.getRouteManager().getRouteId());
                }

                mDriverSync.addRemoveWayPointCallBack(wayPoints -> {
                    // 剔除途经点的回调
                    Log.e(Configs.TAG, ">>>onRemoveWayPoint !!");
                    // app->停止导航，重新算路，开始导航
                    mNaviManager.stopSimulateNavi();
                    mNaviManager.stopNavi();
                    // from:当前司机起点,这里测试就写死了
                    // 开始算路
                    mDriverSync.searchCarRoutes(ConvertUtils.toNaviPoi(order.getBegin()),
                            wayPoints, OrderRouteSearchOptions.create(order.getId()),
                            new DriDataListener.ISearchCallBack() {
                                @Override
                                public void onCalcRouteSuccess(CalcRouteResult calcRouteResult) {
                                    Log.d(Configs.TAG, "onRouteSearchSuccess:" +
                                            calcRouteResult.getRoutes().get(0).getToWayPointInfos().size());
                                    try {
                                        mNaviManager.startSimulateNavi(0);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onCalcRouteFailure(CalcRouteResult calcRouteResult) {
                                    Log.d(Configs.TAG, "onRouteSearchFailure:"
                                            + calcRouteResult.getErrorCode() + " " + calcRouteResult.getErrorMsg());
                                }

                                @Override
                                public void onParamsInvalid(int errCode, String errMsg) {
                                    Log.d(Configs.TAG, "onParamsInvalid:" + errCode + " " + errMsg);
                                }
                            }
                    );
                });

                return routeData.size() >= 1;
            }
        });

        mDriverPanel.addAction(ACTION_ORDER_BIND, new PanelView.Action<String>("") {
            @Override
            public String run() {
                mDriver.setPosition(MockSyncService.getRandomVisibleLatLng(mCarNaviView.getMap()));
                MockOrder order = mPassengerInfo.mMockSyncService.getOrder(mPassenger);
                if (order == null) {
                    return "";
                }
                order.setStatus(MockOrder.Status.Accepted);
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
                return order.getId();
            }
        });

        mDriverPanel.addAction(ACTION_ORDER_TO_PICKUP, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                MockOrder order = mPassengerInfo.mMockSyncService.getOrder(mPassenger);
//                mDriverSync.getTLSBOrder().setOrderId(order.getId());
//                mDriverSync.getTLSBOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
//                mPassengerSync.getTLSPOrder().setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone);
                order = mDriverInfo.mMockSyncService.acceptPassenger(mDriver, mPassenger);
                if (order != null && order.isAccepted()) {
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
                MockOrder order = mDriverInfo.mMockSyncService.onTheWayPassenger(mDriver, mPassenger);
                if (order == null) {
                    mDriverPanel.print("当前订单送驾失败");
                    return false;
                }
                mDriverSync.getTLSBOrder().setOrderId(order.getId());
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

        mDriverPanel.addAction(ACTION_SYNC_CLOSE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.stop();
                return true;
            }
        });

        mDriverPanel.addAction(ACTION_NAVI_SIMULATOR_OPEN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                try {
                    mDriverSync.setPositionExtraInfo("12345测试test~!@#$%^&*><?-/");
                    mNaviManager.startSimulateNavi(0);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        mDriverPanel.addAction(ACTION_NAVI_OPEN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                try {
                    mDriverSync.setPositionExtraInfo(null);
                    mNaviManager.startNavi(0);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        mDriverPanel.addAction(ACTION_NAVI_CLOSE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                try {
                    mNaviManager.stopNavi();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        mDriverPanel.addAction(ACTION_NAVI_SIMULATOR_CLOSE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                try {
                    mNaviManager.stopSimulateNavi();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        mDriverPanel.addAction(ACTION_ROUTES_RECTIFY_DEVIATION,
                new PanelView.Action<Boolean>(false) {
                    @Override
                    public Boolean run() {
                        TLSLatlng dest = mDriverSync.getRouteManager().getDestPosition();
                        if (dest == null) {
                            return false;
                        }

                        NaviPoi naviPoi = new NaviPoi(dest.getLatitude(), dest.getLongitude(), dest.getPoiId());
                        mNaviManager.changeDestination(naviPoi);
                        return true;
                    }
                });

        mDriverPanel.addAction(ACTION_ROUTES_PLAN, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                final MockOrder order = mPassengerInfo.mMockSyncService.getOrder(mPassenger);
                if (order == null) {
                    return false;
                }
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
                mDriverSync.uploadRouteWithIndex(0);
                try {
                    syncWaiting.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return result[0] == 1;
            }
        });

        mDriverPanel.addAction(ACTION_PULL, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mDriverSync.addTLSDriverListener(driDataListener);
                return true;
            }
        });

        onCreateDriverAction(mDriver, mDriverSync, mDriverPanel, mCarNaviView, mNaviManager);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        mCarNaviView = findViewById(R.id.navi_car_view);
        mMapView = findViewById(R.id.map_view1);

        mDriverPanel = findViewById(R.id.group_panel_driver);
        mPassengerPanel = findViewById(R.id.group_panel_passenger);

        mPassengerInfo = createPassenger();
        mDriverInfo = createDriver();
        mPassenger = mPassengerInfo.mPassenger;
        mPassengerSync = mPassengerInfo.mPassengerSync;
        mDriver = mDriverInfo.mDriver;
        mDriverSync = mDriverInfo.mDriverManager;

        initPassengerPanel();
        initDriverPanel();
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

        List<TLSBRouteTrafficItem> trafficItems = manager.getRouteManager().getTrafficItemsWithInternalRoute();
        int[] colors = new int[trafficItems.size()];
        int[] indexes = new int[trafficItems.size()];

        if (main == null) {
            setRouteTraffic(trafficItems, indexes, colors);
            main = map.addPolyline(new PolylineOptions()
                    .width(25)
                    .colors(colors, indexes)
                    .arrow(true)
                    .addAll(mapLatLngs));
            cache.put(manager.getRouteManager().getRouteId(), main);
        } else {
            setRouteTraffic(trafficItems, indexes, colors);
            main.setColors(colors, indexes);
            main.setPoints(mapLatLngs);
        }
        updateStartAndEndPosition(map, manager, all);
        MapUtils.fitsWithRoute(map, all,
                25, 25, 25, 25);
        return true;
    }

    private static void setRouteTraffic(List<TLSBRouteTrafficItem> trafficItems, int[] indexes, int[] colors) {
        for (int i = 0; i < trafficItems.size(); i++) {
            TLSBRouteTrafficItem item = trafficItems.get(i);
            int colorInt = item.getColor();
            int from = item.getFrom();
            indexes[i] = from;
            int color = 0xFFFFFFFF;
            switch (colorInt) {
                case 0:
                    // 路况标签-畅通(绿色)
                    color = 0xFF00CC66;
                    break;
                case 1:
                    // 路况标签-缓慢(黄色)
                    color = 0xFFF5CC00;
                    break;
                case 2:
                    // 路况标签-拥堵(红色)
                    color = 0xFFF24854;
                    break;
                case 3:
                    // 路况标签-无路况
                    color = 0xFF4E8CFF;
                    break;
                case 4:
                    // 路况标签-特别拥堵（猪肝红）
                    color = 0xFF992529;
                    break;
            }
            colors[i] = color;
        }
    }

    private void updateStartAndEndPosition(TencentMap map, BaseSyncProtocol manager, List<LatLng> all) {
        TLSLatlng startPosition = manager.getRouteManager().getStartPosition();
        TLSLatlng destPosition = manager.getRouteManager().getDestPosition();
        List<TLSBWayPoint> wayPoints = manager.getRouteManager().getWayPoints();

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

        if (wayPoints != null && wayPoints.size() > 0) {
            List<Marker> wayMarkers = getWayMarkers(manager);
            if (wayMarkers != null && wayMarkers.size() > 0) {
                for (Marker marker : wayMarkers) {
                    marker.remove();
                }
                wayMarkers.clear();
            }
            for (TLSBWayPoint point : wayPoints) {
                Marker marker = map.addMarker(new MarkerOptions(ConvertUtil.toLatLng(point.getPosition()))
                        .title(point.getPassengerOrderId()));
                if (point.getWayPointType() == TLSBWayPointType.TLSDWayPointTypeGetOff) {
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                marker.showInfoWindow();
                wayMarkers.add(marker);
            }
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

    private List<Marker> getWayMarkers(BaseSyncProtocol manager) {
        return manager instanceof TSLPassengerManager ? mPassengerWayMarkers : mDriverWayMarkers;
    }

    private boolean drawRoutes(TencentMap map, BaseSyncProtocol manager, Map<String, Polyline> cache,
                               Callback<Integer> selectCallback) {
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
        for (Iterator<Map.Entry<String, Polyline>> entryIterator = cache.entrySet().iterator();
             entryIterator.hasNext(); ) {
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

    protected DriverInfo createDriver() {
        MockCar car = MockSyncService.newRandomCar();
        mDriver = MockSyncService.newRandomDriver(mCarNaviView.getMap(), car);
        mDriverSync = TSLDExtendManager.newInstance();
        mDriverSync.init(this,
                TLSConfigPreference.create()
                        .setDebuggable(true)
                        .setAccountId(mDriver.getId())
                        .setDeviceId("device1")
                        .setAllTimeLocation(true));
        return new DriverInfo(mDriver, mDriverSync);
    }

    private boolean showLocation(TencentMap map, List<TLSBDriverPosition> positions, TLSBDriverPosition driPos) {
        if (positions == null || positions.isEmpty()) {
            Log.d(Configs.TAG, "司机位置为空");
            return false;
        }
        LatLng[] latLngs = MapUtils.getLatLngsBySynchroLocation(positions);
        if (latLngs == null || latLngs.length == 0) {
            return false;
        }

        if (psg == null) {
            psg = map.addMarker(new MarkerOptions(new LatLng(driPos.getAttachLat(), driPos.getAttachLng()))
                    .anchor(0.5f, 0.5f)
                    .rotation(driPos.getMatchedCourse())
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_icon_driver))
                    .clockwise(false)
                    .flat(true)
                    .title("driver"));
        } else {
            TLSBDriverPosition temp = positions.get(positions.size() - 1);
            psg.setTitle(temp.getRemainingDistance() / 1000.0 + "公里" + "  " +
                    temp.getRemainingTime() + "分钟");
        }
        boolean rotateEnabled = false;
        float currentMatchedCourse = driPos.getMatchedCourse();
        for (TLSBDriverPosition position : positions) {
            if (Math.abs(currentMatchedCourse - position.getMatchedCourse()) > 1.0f) {
                rotateEnabled = true;
            }
        }
        psg.setRotation(currentMatchedCourse);
        MarkerTranslateAnimator animator = new MarkerTranslateAnimator(psg, 4000, latLngs, rotateEnabled);
        animator.startAnimation();
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
        mDriverSync.destroy();
        mNaviManager.stopSimulateNavi();
        mNaviManager.stopNavi();
        mNaviManager.removeNaviView(mCarNaviView);
        mCarNaviView.onDestroy();

        mPassengerSync.destroy();
        mMapView.onDestroy();
    }

    public static class PassengerInfo {

        public MockPassenger mPassenger;
        public TSLPassengerManager mPassengerSync;
        public MockSyncService mMockSyncService;

        public PassengerInfo(MockPassenger passenger, TSLPassengerManager passengerSync) {
            mPassenger = passenger;
            mPassengerSync = passengerSync;
            mMockSyncService = new MockSyncService(passengerSync);
        }
    }

    public static class DriverInfo {

        public MockCar mCar;
        public MockDriver mDriver;
        public TSLDExtendManager mDriverManager;
        public MockSyncService mMockSyncService;

        public DriverInfo(MockDriver driver, TSLDExtendManager driverManager) {
            mDriver = driver;
            mDriverManager = driverManager;
            mCar = driver.getCar();
            mMockSyncService = new MockSyncService(driverManager);
        }
    }

    private boolean isLatLngSame(TLSLatlng point1, LatLng point2) {
        if (Math.abs(point1.getLatitude() - point2.getLatitude()) >= 0.000001 ||
                Math.abs(point1.getLongitude() - point2.getLongitude()) >= 0.000001) {
            return false;
        }
        return true;
    }
}
