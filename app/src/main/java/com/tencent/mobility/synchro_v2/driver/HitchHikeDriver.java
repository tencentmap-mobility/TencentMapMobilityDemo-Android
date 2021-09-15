package com.tencent.mobility.synchro_v2.driver;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;

import com.tencent.map.geolocation.TencentLocation;
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
import com.tencent.map.navi.tlocation.ITNKLocationCallBack;
import com.tencent.mobility.R;
import com.tencent.mobility.synchro_v2.helper.ConvertHelper;
import com.tencent.mobility.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 顺风车司机端
 */
public class HitchHikeDriver extends DriverBase implements RadioGroup.OnCheckedChangeListener {

    String driverId = "test_driver_000001";// 顺风车司机id
    String orderId = "test_driver_order_a_000001";// 顺风车id
    String pOderId01 = "test_passenger_order_a_000001"; // 乘客1ID
    String pOderId02 = "test_passenger_order_000012"; // 乘客2ID

    int curOrderState = TLSBOrderStatus.TLSDOrderStatusNone;
    int curDrvierStatus = TLSDDrvierStatus.TLSDDrvierStatusStopped;// 默认收车
    int curOrderType = TLSBOrderType.TLSDOrderTypeHitchRide;

    // 这是司机的起终点
    NaviPoi from = new NaviPoi(40.041032,116.27245);
    NaviPoi to = new NaviPoi(39.868699,116.32198);
    ArrayList<TLSDWayPointInfo> ws = new ArrayList<>();// 拼单的上下车点

    int curRouteIndex = 0;
    RouteData curRoute;
    String curRouteId = "";
    MyLocListener locListener;

    RadioGroup radioGroup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.ls_hitchhike_driver_layout);
        super.onCreate(savedInstanceState);

        locListener = new MyLocListener();
        initConfig(driverId);
        lsManager.addTLSDriverListener(new MyDriverListener());
    }

    @Override
    void init() {
        carNaviView = findViewById(R.id.car_navi_view);
        radioGroup = findViewById(R.id.cur_account);
        radioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.hh_a:
                if(radioGroup != null)
                    radioGroup.check(R.id.hh_a);
                break;
            case R.id.hh_b:
                if(radioGroup != null)
                    radioGroup.check(R.id.hh_b);
                break;
            case R.id.hh_a_and_b:
                if(radioGroup != null)
                    radioGroup.check(R.id.hh_a_and_b);
                break;
            case R.id.hh_a_and_b_sort:
                if(radioGroup != null)
                    radioGroup.check(R.id.hh_a_and_b_sort);
                break;
        }
    }

    /**
     * 开启定位
     * @param view
     */
    public void startDLocation(View view) {
        startLoc(locListener);
    }

    /**
     * 停止定位
     * @param view
     */
    public void stopDLocation(View view) {
        stopLoc(locListener);
    }

    /**
     * 拉取乘客定位点
     * @param view
     */
    public void pullGuestPoints(View view) {
        // 在顺风单中，司机不能拉取乘客位置
        ToastUtils.INSTANCE().Toast("顺风车不支持拉取乘客位置!!");
    }

    /**
     * 停止拉取
     * @param view
     */
    public void stopPullGuestPoints(View view) {
        ToastUtils.INSTANCE().Toast("顺风车不支持拉取乘客位置!!");
    }

    /**
     * 模拟听单页
     * 在app实际使用的时候，会在进入听单页时开启司乘
     * @param view
     */
    public void startReceiveOrder(View view) {
        if(lsManager != null) {
            lsManager.getTLSBOrder().setDrvierStatus(TLSDDrvierStatus.TLSDDrvierStatusListening);
            startSync();
        }
    }

    /**
     * 结束司乘
     * @param view
     */
    public void stopReceiveOrder(View view) {
        stopSync();
    }

    /**
     * 顺风车订单
     * @param view
     */
    public void receiveHitchHikeOrder(View view) {
        if(ws.size() != 0)
            ws.clear();
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.hh_a:// 订单A
                ws.add(addWayAFrom());
                ws.add(addWayATo());
                search();// 算路
                break;
            case R.id.hh_b:
                ws.add(addWayBFrom());// 订单B
                ws.add(addWayBTo());
                search();// 算路
                break;
            case R.id.hh_a_and_b:
                ws.add(addWayBFrom());
                ws.add(addWayBTo());
                ws.add(addWayAFrom());
                ws.add(addWayATo());
                search();// 算路
                break;
            case R.id.hh_a_and_b_sort:// 获取AB最优送驾顺序
                ws.add(addWayAFrom());
                ws.add(addWayATo());
                ws.add(addWayBFrom());
                ws.add(addWayBTo());

                /**
                 * 需要先获取最优送驾顺序
                 */
                lsManager.requestBestSortedWayPoints(from, to, ws, new DriDataListener.ISortedWayPointsCallBack() {
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
                break;
        }

    }

    /**
     * 开始算路
     */
    public void search() {
        if(lsManager == null)
            return;
        lsManager.setCarNaviView(carNaviView);
        lsManager.searchCarRoutes(from, to, ws, CarRouteSearchOptions.create()
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
                        drawUi(curRoute, from, to, ws);
                    }
                });
    }

    /**
     * 开始接驾
     * @param view
     */
    public void startMeetingGuest(View view) {
        curDrvierStatus = TLSDDrvierStatus.TLSDDrvierStatusServing;// 服务中
        curOrderState = TLSBOrderStatus.TLSDOrderStatusTrip;// 顺风车都是送驾
        lsManager.getTLSBOrder().setOrderStatus(curOrderState)
                .setOrderId(orderId).setOrderType(curOrderType)
                .setDrvierStatus(curDrvierStatus);
        lsManager.uploadRouteWithIndex(curRouteIndex);// 上传路线

        Intent intent = new Intent(this, DriverNaviActivity.class);
        intent.putExtra("route_index", curRouteIndex);
        intent.putExtra("routeId", curRouteId);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSync();
    }

    private TLSDWayPointInfo addWayAFrom() {
        TLSDWayPointInfo w1 = new TLSDWayPointInfo();
        w1.setpOrderId(pOderId01);// 乘客1订单id
        w1.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetIn);
        w1.setLat(39.940080);
        w1.setLng(116.355257);

        /**
         * 如果算路所在carNaviView和导航carNaviView相同，
         * 则可这样设置途经点bitmap
         */
        w1.setImage(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint1_1));
        return w1;
    }

    private TLSDWayPointInfo addWayATo() {
        TLSDWayPointInfo w2 = new TLSDWayPointInfo();
        w2.setpOrderId(pOderId01);// 乘客1订单id
        w2.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetOff);
        w2.setLat(39.923890);
        w2.setLng(116.344700);
        return w2;
    }

    private TLSDWayPointInfo addWayBFrom() {
        TLSDWayPointInfo w1 = new TLSDWayPointInfo();
        w1.setpOrderId(pOderId02);// 乘客2订单id
        w1.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetIn);
        w1.setLat(39.932446);
        w1.setLng(116.363153);
        return w1;
    }

    private TLSDWayPointInfo addWayBTo() {
        TLSDWayPointInfo w2 = new TLSDWayPointInfo();
        w2.setpOrderId(pOderId02);// 乘客2订单id
        w2.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetOff);
        w2.setLat(39.923297);
        w2.setLng(116.360407);
        return w2;
    }

    /**
     * {@link com.tencent.map.navi.tlocation.TNKLocationManager}
     * 是导航SDK内部对于腾讯定位SDK的一个封装
     * 用户可以通过{@code TNKLocationManager}非常方便的获取定位信息
     *
     * <p>注册监听后，如果定位还未启动，则会启动定位
     *
     * <p>司机在⾮导航态，需要⽤户管理定位点的上报⼯作。
     * ⽤户可通过定位SDK获取定位信息，随后将定位点通过司乘SDK的上报接口进行上报。
     */
    class MyLocListener implements ITNKLocationCallBack {
        @Override
        public void requestLocationUpdatesResult(int i) {

        }

        @Override
        public void onLocationChanged(TencentLocation location, int i, String s) {
            /**
             * 在非导航态，上传定位点时，也需要更新TLSBOrder信息。
             */
            if(lsManager != null && location != null) {
                lsManager.getTLSBOrder()
                        .setOrderId("-1") // SDK默认-1，不能为空
                        .setOrderType(curOrderType) // 默认快车
                        .setDrvierStatus(curDrvierStatus); // 听单中
                lsManager.uploadPosition(ConvertHelper.tenPoToTLSDPo(location));
            }
        }

        @Override
        public void onStatusUpdate(String s, int i, String s1) {

        }

        @Override
        public void onGnssInfoChanged(Object o) {

        }

        @Override
        public void onNmeaMsgChanged(String s) {

        }
    }

    /**
     * 司乘数据回调
     */
    class MyDriverListener extends SimpleDriDataListener {
        @Override
        public void onPushRouteSuc() {
            Log.e(LOG_TAG, "onPushRouteSuc()");
        }

        @Override
        public void onPushRouteFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "onPushRouteFail()");
        }

        @Override
        public void onPushPositionSuc() {
            Log.e(LOG_TAG, "onPushPositionSuc()");
        }

        @Override
        public void onPushPositionFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "onPushPositionFail()");
        }

        @Override
        public void onPullLsInfoSuc(List<TLSBPosition> los) {
            Log.e(LOG_TAG, "onPullLsInfoSuc()");
            showPsgPosition(los);// 展示乘客位置
        }

        @Override
        public void onPullLsInfoFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "onPullLsInfoFail()");
        }
    }

}
