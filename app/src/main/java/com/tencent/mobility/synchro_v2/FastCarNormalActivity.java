package com.tencent.mobility.synchro_v2;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lsdriver.protocol.OrderRouteSearchOptions;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.lsp.listener.SimplePsgDataListener;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
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
import com.tencent.map.navi.ui.car.CarNaviInfoPanel;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockOrder;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.ui.PanelView;
import com.tencent.mobility.util.AnimatorUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FastCarNormalActivity extends BaseActivity {

    private CarNaviView mCarNaviView;
    private MapView mMapView;

    private PanelView mDriverPanel;
    private PanelView mPassengerPanel;

    private TSLDExtendManager mDriverSync;
    private TSLPassengerManager mPassengerSync;

    private MockDriver mDriver;
    private MockPassenger mPassenger;

    private MockSyncService mDriverSyncService;
    private MockSyncService mPassengerSyncService;

    private TencentCarNaviManager mNaviManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.normal_fast_car_layout);

        mCarNaviView = findViewById(R.id.driver_navi_car_view);
        mMapView = findViewById(R.id.psg_map_view);

        mDriverPanel = findViewById(R.id.group_panel_driver);
        mPassengerPanel = findViewById(R.id.group_panel_passenger);

        initPassengerPanel();
        initDriverPanel();

        AnimatorUtils.init();
    }

    private void initPassengerPanel() {
        mPassenger = MockSyncService.newRandomPassenger(mMapView.getMap());
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
                mPassenger.setPosition(MockSyncService.getRandomVisibleLatLng(mMapView.getMap()));
                MockOrder order = mPassengerSyncService.newOrder(mMapView.getMap(), mPassenger);
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
                        mPassengerPanel.postPrint("拉取成功");
                        if (fetchedData != null) {
                            drawMarker(mMapView.getMap(), mPassengerSync);
                            AnimatorUtils.updateDriverInfo(mMapView.getMap(), fetchedData.getRoute(), fetchedData.getOrder(), fetchedData.getPositions());
                        }
                    }

                    @Override
                    public void onPullLsInfoFail(int errCode, String errMsg) {
                        mPassengerPanel.postPrint("拉取失败：" + errCode);
                    }
                });

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
        mCarNaviView.setNaviMode(NaviMode.MODE_REMAINING_OVERVIEW);
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
        mCarNaviView.setNavigationPanelVisible(false);

        MockCar car = MockSyncService.newRandomCar();
        mDriver = MockSyncService.newRandomDriver(mCarNaviView.getMap(), car);
        mDriverSync = TSLDExtendManager.newInstance();
        mDriverSyncService = new MockSyncService(mDriverSync);
        mDriverSync.init(FastCarNormalActivity.this,
                TLSConfigPreference.create().setDebuggable(true).setAccountId(mDriver.getId()));
        mDriverSync.setNaviManager(mNaviManager);
        mDriverSync.setCarNaviView(mCarNaviView);
        mDriverPanel.init("司机");

        mDriverPanel.addAction("绑定订单", new PanelView.Action<String>("") {
            @Override
            public String run() {
                mDriver.setPosition(MockSyncService.getRandomVisibleLatLng(mCarNaviView.getMap()));
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
                mDriverPanel.postAction("上报路线");

                try {
                    mNaviManager.startSimulateNavi(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                MapUtils.fitsWithRoute(mCarNaviView.getMap(), ConvertUtils.transformLatLngs(mDriverSync.getRouteManager().getPoints()),
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

    Marker aMarkerStart = null;
    Marker aMarkerEnd = null;
    private void drawMarker(TencentMap map, BaseSyncProtocol manager) {
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
        AnimatorUtils.clearUi();
        mDriverSync.destroy();
        mPassengerSync.destroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
