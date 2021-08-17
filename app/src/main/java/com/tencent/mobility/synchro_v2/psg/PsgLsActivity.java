package com.tencent.mobility.synchro_v2.psg;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.lsp.listener.PsgDataListener;
import com.tencent.map.lssupport.bean.TLSBDriverPosition;
import com.tencent.map.lssupport.bean.TLSBOrder;
import com.tencent.map.lssupport.bean.TLSBOrderStatus;
import com.tencent.map.lssupport.bean.TLSBOrderType;
import com.tencent.map.lssupport.bean.TLSBRoute;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSDDrvierStatus;
import com.tencent.map.lssupport.bean.TLSDFetchedData;
import com.tencent.mobility.R;
import com.tencent.mobility.location.GeoLocationAdapter;
import com.tencent.mobility.synchro_v2.helper.ConvertHelper;
import com.tencent.mobility.util.ToastUtils;
import com.tencent.navi.surport.utils.DeviceUtils;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;

import java.util.ArrayList;

public abstract class PsgLsActivity extends PsgBaseMapActivity {

    static final String LOG_TAG = "navi1234";

    protected static final int PSG_FAST = 0; // 快车
    protected static final int PSG_HITCH_HIKE = 1; // 顺丰车
    protected static final int PSG_CARPOOLING = 2; // 拼车

    String orderId = "test_driver_order_a_000001"; // 司机订单id
    String psgId = "test_passenger_000001"; // 乘客id
    String pOrderId = "test_passenger_order_a_000001"; // 乘客子订单id
    int curOrderType = TLSBOrderType.TLSDOrderTypeNormal; // 订单类型
    int curOrderState = TLSBOrderStatus.TLSDOrderStatusNone; // 送驾状态
    int curDriverState = TLSDDrvierStatus.TLSDDrvierStatusStopped; // 司机状态

    TSLPassengerManager tlspManager;// 司乘管理类

    protected int currCarType = PSG_HITCH_HIKE;

    Marker psgMarker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ToastUtils.init(getApplicationContext());// 初始化toast

        tlspManager = TSLPassengerManager.getInstance();// 初始化司乘
        tlspManager.init(getApplicationContext(), TLSConfigPreference.create()
                .setAccountId(psgId)
                .setDeviceId(DeviceUtils.getImei(getApplicationContext())));
        tlspManager.addTLSPassengerListener(new MyPullDriverInfo());// 司乘回调
    }

    private void startGeoLocation() {
        GeoLocationAdapter.singleton.get().startGeoLocationAdapter(getApplicationContext());
        GeoLocationAdapter.singleton.get().addGeoLocationListener(
                (tencentGeoLocation) -> {
                    // 上传定位点
                    updatePsgLoc(tencentGeoLocation.getLocation());
                    // 展示自身位置
                    showPsgLoc(tencentGeoLocation.getLocation());
                });
    }

    private void updatePsgLoc(TencentLocation location) {
        // 支持快车
        if (currCarType == PSG_FAST) {
            TLSConfigPreference.create()
                    .setAccountId(psgId);

            tlspManager.getTLSPOrder()
                    .setOrderId(orderId)
                    .setOrderStatus(curOrderState)
                    .setOrderType(curOrderType)
                    .setCityCode(location.getCityCode());

            tlspManager.uploadPosition(ConvertHelper.tenPoTOTLSPo(location));
        }

    }

    private void showPsgLoc(TencentLocation location) {
        if (location != null) { // 展示自己位置
            if(psgMarker == null)
                psgMarker = addMarker(new LatLng
                                (location.getLatitude(), location.getLongitude())
                        , R.mipmap.psg_position_icon
                        , 0);
            else
                psgMarker.setPosition(new LatLng
                        (location.getLatitude(), location.getLongitude()));
        }
    }

    /**
     * 模拟拼单A
     * @param view
     */
    public void sendHitchHikeOrderA(View view) {
        if(tlspManager == null)
            return;
        curOrderType = TLSBOrderType.TLSDOrderTypeHitchRide;
        curOrderState = TLSBOrderStatus.TLSDOrderStatusTrip;// 顺风单都是送驾状态
        curDriverState = TLSDDrvierStatus.TLSDDrvierStatusServing;
        tlspManager.getTLSPOrder().setpOrderId(pOrderId)
                .setOrderId(orderId)
                .setOrderStatus(curOrderState)
                .setDrvierStatus(curDriverState)
                .setOrderType(curOrderType);
        if(!tlspManager.isRuning())
            tlspManager.start();
    }

    /**
     * 模拟听单B
     * @param view
     */
    public void sendHitchHikeOrderB(View view) {
        if(tlspManager == null)
            return;
        curOrderType = TLSBOrderType.TLSDOrderTypeHitchRide;
        curOrderState = TLSBOrderStatus.TLSDOrderStatusTrip;// 顺风单都是送驾状态
        curDriverState = TLSDDrvierStatus.TLSDDrvierStatusServing;
        tlspManager.getTLSPOrder().setpOrderId(pOrderId)
                .setOrderId(orderId)
                .setOrderStatus(curOrderState)
                .setDrvierStatus(curDriverState)
                .setOrderType(curOrderType);
        if(!tlspManager.isRuning())
            tlspManager.start();
    }

    /**
     * 模拟进入快车单C
     * @param view
     */
    public void sendHighOrder(View view) {
        if(tlspManager == null)
            return;
        curOrderType = TLSBOrderType.TLSDOrderTypeNormal;
        curOrderState = TLSBOrderStatus.TLSDOrderStatusPickUp;// 接驾
        curDriverState = TLSDDrvierStatus.TLSDDrvierStatusServing;
        tlspManager.getTLSPOrder().setpOrderId(pOrderId)// pOrderId == ""
                .setOrderId(orderId)
                .setOrderStatus(curOrderState)
                .setDrvierStatus(curDriverState)
                .setOrderType(curOrderType);
        if(!tlspManager.isRuning())
            tlspManager.start();
    }

    /**
     * 进入拼车单A
     * @param view
     */
    public void sendCarpoolingOrder(View view) {
        if(tlspManager == null)
            return;
        curOrderType = TLSBOrderType.TLSBOrderTypeRidesharing;
        curOrderState = TLSBOrderStatus.TLSDOrderStatusTrip;// 拼车单都是送驾状态，根顺风车一样
        curDriverState = TLSDDrvierStatus.TLSDDrvierStatusServing;
        tlspManager.getTLSPOrder().setpOrderId(pOrderId)
                .setOrderId(orderId)
                .setOrderStatus(curOrderState)
                .setDrvierStatus(curDriverState)
                .setOrderType(curOrderType);
        if(!tlspManager.isRuning())
            tlspManager.start();
    }

    /**
     * 结束司乘
     * @param view
     */
    public void finishOrder(View view) {
        ToastUtils.INSTANCE().Toast("结束司乘!!");
        if(tlspManager != null)
            tlspManager.stop();
        clearUi();
    }

    /**
     * 开启司乘
     * @param view
     */
    public void startLs(View view) {
        ToastUtils.INSTANCE().Toast("开启司乘");
        if(tlspManager != null)
            tlspManager.start();
    }

    /**
     * 开启定位
     * @param view
     */
    public void startLocation(View view) {
        startGeoLocation();
    }

    /**
     * 停止定位
     * @param view
     */
    public void stopLocation(View view) {
        GeoLocationAdapter.singleton.get().stopGeoLocationAdapter();
    }

    /**
     * 上报定位点
     * @param view
     */
    public void pushLocation(View view) {
        ToastUtils.INSTANCE().Toast("上报定位点");
        if(tlspManager != null)
            tlspManager.uploadPassengerPositionsEnabled(true);
    }

    /**
     * 取消上传定位点
     * @param view
     */
    public void stopPushLocation(View view) {
        ToastUtils.INSTANCE().Toast("停止上报定位点!!");
        if(tlspManager != null)
            tlspManager.uploadPassengerPositionsEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastUtils.INSTANCE().destory();
        if(tlspManager != null)
            tlspManager.stop();
    }

    /**
     * 拉取到的司机信息，子类做各种的处理
     * @param route
     * @param order
     * @param pos
     */
    abstract void updateDriverInfo(TLSBRoute route, TLSBOrder order, ArrayList<TLSBDriverPosition> pos);

    /**
     * 清空当前界面ui
     */
    abstract void clearUi();

    /**
     * 司乘sdk对外暴露的回调接口
     */
    class MyPullDriverInfo implements PsgDataListener.ITLSPassengerListener {

        @Override
        public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
            Log.e(LOG_TAG, "pull driver info suc !!");

            updateDriverInfo(fetchedData.getRoute(), fetchedData.getOrder(), fetchedData.getPositions());

        }

        @Override
        public void onPullLsInfoFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "pull driver info fail -> errCode : " +errCode + ", err" + errMsg);
        }

        @Override
        public void onPushPositionSuc() {
            /**
             * 注意：只支持快车司机展示乘客位置。
             */
            Log.e(LOG_TAG, "push location suc !!");
        }

        @Override
        public void onPushPositionFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "push location fail -> errCode : " +errCode + ", err" + errMsg);
        }
    }

}
