package com.tencent.mobility.synchro_v2.driver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lssupport.bean.TLSBOrder;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBOrderType;
import com.tencent.map.lssupport.bean.TLSBPosition;
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

/**
 * 快车司机端
 */
public class FastDriver extends DriverBase {

    String driverId = "OD_xc_10001";// 快车司机id
    String orderId = "xc_1112";
    int curOrderState = TLSBOrderStatus.TLSDOrderStatusNone;
    int curDrvierStatus = TLSDDrvierStatus.TLSDDrvierStatusListening;// 默认听单中
    int curOrderType = TLSBOrderType.TLSDOrderTypeNormal;

    // 这是司机的起终点
    NaviPoi from = new NaviPoi(40.041032,116.27245);
    NaviPoi to = new NaviPoi(39.868699,116.32198);
    ArrayList<TLSDWayPointInfo> ws = new ArrayList<>();// 拼单的上下车点

    int curRouteIndex = 0;
    RouteData curRoute;
    String curRouteId = "";

    MyLocListener locListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.ls_fast_driver);
        super.onCreate(savedInstanceState);

        locListener = new MyLocListener();
        initConfig(driverId);
        lsManager.addTLSDriverListener(new MyDriverListener());
    }

    @Override
    void init() {
        carNaviView = findViewById(R.id.car_navi_view);
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
       if(lsManager != null) {
           TLSBOrder order = lsManager.getTLSBOrder();
           if(order.getDrvierStatus() != TLSDDrvierStatus.TLSDDrvierStatusServing) {// 只有在服务中才有订单
               order.setOrderStatus(curOrderState)
                       .setOrderId(orderId).setOrderType(curOrderType)
                       .setDrvierStatus(curDrvierStatus);
           }
           startPullPsgPos();
       }
    }

    /**
     * 停止拉取
     * @param view
     */
    public void stopPullGuestPoints(View view) {
        stopPullPsgPos();
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
     * 普通快车
     * @param view
     */
    public void receiveFastOrder(View view) {
        if(lsManager == null)
            return;
        curOrderType = TLSBOrderType.TLSDOrderTypeNormal;
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
        curOrderState = TLSBOrderStatus.TLSDOrderStatusPickUp;
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

    class MyLocListener implements ITNKLocationCallBack {
        @Override
        public void requestLocationUpdatesResult(int i) {

        }

        @Override
        public void onLocationChanged(TencentLocation location, int i, String s) {
            /**
             * 定位成功的回调
             */
            if(lsManager != null && location != null) {
                lsManager.getTLSBOrder().setOrderStatus(curOrderState)// 更新订单状态
                        .setOrderId(orderId).setOrderType(curOrderType)
                        .setDrvierStatus(curDrvierStatus)
                        .setCityCode(location.getCityCode());
                lsManager.uploadPosition(ConvertHelper.tenPoToTLSDPo(location));// 更新订单状态
            }

            Log.e(LOG_TAG, "location suc -> lat : " + location.getLatitude()
                    + ", lng : " + location.getLongitude());
        }

        @Override
        public void onStatusUpdate(String s, int i, String s1) {

        }
    }

    /**
     * 司乘数据回调
     */
    class MyDriverListener implements DriDataListener.ITLSDriverListener {
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
        public void onPullLsInfoSuc(ArrayList<TLSBPosition> los) {
            Log.e(LOG_TAG, "onPullLsInfoSuc()");
            showPsgPosition(los);// 展示乘客位置
        }

        @Override
        public void onPullLsInfoFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "onPullLsInfoFail()");
        }
    }
}
