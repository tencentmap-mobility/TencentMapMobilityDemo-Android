package com.tencent.mobility.synchro_v2.driver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBOrderType;
import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.lssupport.bean.TLSBWayPointType;
import com.tencent.map.lssupport.bean.TLSDDrvierStatus;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.map.navi.car.CarRouteSearchOptions;
import com.tencent.map.navi.data.NaviPoi;
import com.tencent.map.navi.data.RouteData;
import com.tencent.map.navi.ui.car.CarNaviInfoPanel;
import com.tencent.mobility.R;
import com.tencent.mobility.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼车司机
 */
public class CarpoolingDriver extends DriverBase {

    static final String LOG_TAG = "tag12345";

    static final String P_ORDER_ID_A = "test_passenger_order_000011"; // 乘客A子订单
    static final String P_ORDER_ID_B = "test_passenger_order_000012"; // 乘客B子订单

    String driverId = "test_driver_000001";// 拼车司机id
    String orderId = "test_driver_order_000011";// 拼车订单id
    int curOrderState = TLSBOrderStatus.TLSDOrderStatusNone;
    int curDrvierStatus = TLSDDrvierStatus.TLSDDrvierStatusStopped;// 默认收车
    int curOrderType = TLSBOrderType.TLSBOrderTypeRidesharing;

    // 这是司机当前的位置
    NaviPoi from = new NaviPoi(40.041032,116.27245);
    ArrayList<TLSDWayPointInfo> ws = new ArrayList<>();// 拼单的上下车点

    int curRouteIndex = 0;
    RouteData curRoute;
    String curRouteId = "";

    boolean isNaving = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.ls_driver_carpooling_layout);
        super.onCreate(savedInstanceState);

        initConfig(driverId);
    }

    @Override
    void init() {
        carNaviView = findViewById(R.id.car_navi_view);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ws.clear();
    }

    /**
     * 模拟听单页
     * 在app实际使用的时候，会在进入听单页时开启司乘
     * @param view
     */
    public void startSync(View view) {
        if(lsManager != null) {
            lsManager.getTLSBOrder().setDrvierStatus(TLSDDrvierStatus.TLSDDrvierStatusListening);
            startSync();
        }
    }

    /**
     * 结束司乘
     * @param view
     */
    public void stopSync(View view) {
        stopSync();
    }

    /**
     * 接收到拼单A
     */
    public void ReceiveCpA(View view) {
        if (isNaving)
            stopNavi(findViewById(R.id.stop_navi));
        ws.clear();
        ws.add(addWayAFrom());
        ws.add(addWayATo());

        sortedCpWays();
    }

    /**
     * 接收到订单AB
     * @param view
     */
    public void receiveCpAB(View view) {
        if (isNaving)
            stopNavi(findViewById(R.id.stop_navi));
        ws.clear();
        ws.add(addWayAFrom());
        ws.add(addWayATo());
        ws.add(addWayBFrom());
        ws.add(addWayBTo());

        sortedCpWays();
    }

    /**
     * 已到达途经点A上车点
     */
    public void arriveCpDestOfA(View view) {
        lsManager.arrivedPassengerStartPoint("test_passenger_order_000011");
    }

    public void arriveCpToOfA(View view) {
        lsManager.arrivedPassengerEndPoint("test_passenger_order_000011");
    }

    /**
     * 已到达途经点B上车点
     */
    public void arriveCpDestOfB(View view) {
        lsManager.arrivedPassengerStartPoint("test_passenger_order_000012");
    }

    public void arriveCpToOfB(View view) {
        lsManager.arrivedPassengerEndPoint("test_passenger_order_000012");
    }

    /**
     * 开始导航
     *
     * 导航页和地图展示页放在一起
     */
    public void startNavi(View view) {
        /**
         * 司乘上传
         */
        uploadRoute();

        clearMapUi(); // 清除路线规划时的绘制元素

        naviManager.addNaviView(carNaviView); // 关联view
        naviManager.setInternalTtsEnabled(true); // 开启播报
        // 使用导航默认UI
        CarNaviInfoPanel carNaviInfoPanel = carNaviView.showNaviInfoPanel();
        carNaviInfoPanel.setOnNaviInfoListener(new CarNaviInfoPanel.OnNaviInfoListener() {
            @Override
            public void onBackClick() {
                stopNavi(findViewById(R.id.stop_navi));
            }
        });
        CarNaviInfoPanel.NaviInfoPanelConfig naviInfoPanleConfig = new CarNaviInfoPanel.NaviInfoPanelConfig();
        carNaviInfoPanel.setNaviInfoPanelConfig(naviInfoPanleConfig);
        // 拼车需要将终点icon隐藏掉
//        carNaviView.configEndPointMarkerpresentation(null, null);
        // 设置途经点图标
//        carNaviView.configWayPointMarkerpresentation(getWayMarker());

        lsManager.addTLSDriverListener(new MyDriverListener());// 数据callback
        lsManager.addRemoveWayPointCallBack(new DriDataListener.IRemoveWayByUserCallBack() {
            @Override
            public void onRemoveWayPoint(List<TLSDWayPointInfo> wayPoints) {
                // 剔除途经点的回调
                Log.e(LOG_TAG, ">>>onRemoveWayPoint !!");
                // app->停止导航，重新算路，开始导航
                stopNavi(findViewById(R.id.stop_navi));
                // from:当前司机位置,这里测试就写死了
                // 开始算路
                ws.clear();
                ws.addAll(wayPoints);
                search();
            }
        });

        try {
            isNaving = true;
            // 开始模拟导航
            naviManager.startSimulateNavi(curRouteIndex);
        } catch (Exception e) {
            isNaving = false;
            Log.e(LOG_TAG, "start navi err : " + e.getMessage());
        }
    }

    /**
     * 结束当前导航
     */
    public void stopNavi(View view) {
        isNaving = false;
        naviManager.stopSimulateNavi();
        carNaviView.hideNaviInfoPanel();
        carNaviView.setNaviPanelEnabled(false);
        carNaviView.clearAllRouteUI();
    }

    private void uploadRoute() {
        curDrvierStatus = TLSDDrvierStatus.TLSDDrvierStatusServing;// 服务中
        curOrderState = TLSBOrderStatus.TLSDOrderStatusTrip;
        lsManager.getTLSBOrder().setOrderStatus(curOrderState)
                .setOrderId(orderId).setOrderType(curOrderType)
                .setDrvierStatus(curDrvierStatus);
        lsManager.uploadRouteWithIndex(curRouteIndex);// 上传路线
    }

    /**
     * 拼车的最优送驾顺序
     */
    private void sortedCpWays() {
        if (ws.size() == 0)
            return;
        lsManager.requestBestSortedWayPoints(from, ws, new DriDataListener.ISortedWayPointsCallBack() {
            @Override
            public void onSortedWaysSuc(List<TLSDWayPointInfo> sortedWays) {
                ws.clear();
                ws.addAll(sortedWays);// 排好序的途经点
                // 获取最优顺序后，开始算路
                search();
            }

            @Override
            public void onSortedWayFail(int errCode, String errMsg) {
                Log.e(LOG_TAG, ">>>errCode : " + errCode + ", errMsg : " + errMsg);
            }
        });
    }

    private void search() {
        /**
         * 将carNaviView托管给司乘，司乘自动管理途经点图标
         * 也可以不跟司乘关联，自行通过导航sdk提供方法设置图标
         *
         * <p>需在算路之前设置
         */
        lsManager.setCarNaviView(carNaviView);
        lsManager.searchCarRoutes(from, ws, CarRouteSearchOptions.create()
                , new DriDataListener.ISearchCallBack() {
                    @Override
                    public void onParamsInvalid(int errCode, String errMsg) {
                        ToastUtils.INSTANCE().Toast("参数不合法!!");
                    }

                    @Override
                    public void onRouteSearchFailure(int i, String s) {
                        ToastUtils.INSTANCE().Toast("算路失败!!");
                    }

                    @Override
                    public void onRouteSearchSuccess(ArrayList<RouteData> arrayList) {
                        /**
                         * 算路成功回调
                         */
                        ToastUtils.INSTANCE().Toast("算路成功");
                        curRoute = arrayList.get(curRouteIndex);
                        curRouteId = curRoute.getRouteId();
                        // 绘制路线
                        drawUi(curRoute, from, null, ws);
                    }
                });

    }

    private TLSDWayPointInfo addWayAFrom() {
        TLSDWayPointInfo w1 = new TLSDWayPointInfo();
        w1.setpOrderId(P_ORDER_ID_A);// 乘客1订单id
        w1.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetIn);
        w1.setLat(39.940080);
        w1.setLng(116.355257);
        w1.setImage(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint1_1));
        return w1;
    }

    private TLSDWayPointInfo addWayATo() {
        TLSDWayPointInfo w2 = new TLSDWayPointInfo();
        w2.setpOrderId(P_ORDER_ID_A);// 乘客1订单id
        w2.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetOff);
        w2.setLat(39.923890);
        w2.setLng(116.344700);
        w2.setImage(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint1_2));
        return w2;
    }

    private TLSDWayPointInfo addWayBFrom() {
        TLSDWayPointInfo w1 = new TLSDWayPointInfo();
        w1.setpOrderId(P_ORDER_ID_B);// 乘客2订单id
        w1.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetIn);
        w1.setLat(39.932446);
        w1.setLng(116.363153);
        w1.setImage(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint2_1));
        return w1;
    }

    private TLSDWayPointInfo addWayBTo() {
        TLSDWayPointInfo w2 = new TLSDWayPointInfo();
        w2.setpOrderId(P_ORDER_ID_B);// 乘客2订单id
        w2.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetOff);
        w2.setLat(39.923297);
        w2.setLng(116.360407);
        w2.setImage(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint2_2));
        return w2;
    }

    /**
     * 设置途经点图片
     */
    private ArrayList<Bitmap> getWayMarker() {
        ArrayList<Bitmap> bps = new ArrayList<>();
        bps.add(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint1_1));
        bps.add(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint1_2));
        bps.add(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint2_1));
        bps.add(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint2_2));
        return bps;
    }

    /**
     * 数据回调
     */
    class MyDriverListener extends SimpleDriDataListener {
        @Override
        public void onPushRouteSuc() {
            Log.e(LOG_TAG, "navigation onPushRouteSuc()");
        }

        @Override
        public void onPushRouteFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "navigation onPushRouteFail()");
        }

        @Override
        public void onPushPositionSuc() {
            Log.e(LOG_TAG, "navigation onPushPositionSuc()");
        }

        @Override
        public void onPushPositionFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "navigation onPushPositionFail()");
        }

        @Override
        public void onPullLsInfoSuc(List<TLSBPosition> los) {
            Log.e(LOG_TAG, "navigation onPullLsInfoSuc()");
        }

        @Override
        public void onPullLsInfoFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "navigation onPullLsInfoFail()");
        }
    }

}
