package com.tencent.mobility.sychro.driver;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.map.navi.tlocation.ITNKLocationCallBack;
import com.tencent.map.navi.tlocation.TNKLocationManager;
import com.tencent.mobility.sychro.driver.helper.SingleHelper;
import com.tencent.mobility.util.ToastUtils;
import com.tencent.navi.surport.utils.DeviceUtils;

public abstract class ExtendDriverBase extends DriverBase {
    static final String LOG_TAG = "navi1234";

    TSLDExtendManager lsManager;// 司乘管理类
    TNKLocationManager loManager;// 导航内部定位
    TencentCarNaviManager naviManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化toast
        ToastUtils.init(getApplicationContext());

        // 初始化导航
        naviManager = SingleHelper.getNaviManager(getApplicationContext());

        // 初始化司乘
        lsManager = TSLDExtendManager.getInstance();
        lsManager.setNaviManager(naviManager);

        // 初始化定位
        loManager = TNKLocationManager.getInstance();
        loManager.setContext(getApplicationContext());
    }

    /**
     * 初始化配置
     * @param driverId 司机id
     */
    public void initConfig(String driverId) {
        lsManager.init(getApplicationContext(), TLSConfigPreference.create()
                .setDeviceId(DeviceUtils.getImei(getApplicationContext()))// 设备id
                .setAccountId(driverId));// 司机id
    }

    /**
     * 开启定位
     */
    public void startLoc(ITNKLocationCallBack locListener) {
        ToastUtils.INSTANCE().Toast("开始定位");
        if(loManager != null)// 注册监听后，自动启动定位
            loManager.addLocationListener(locListener);
    }

    /**
     * 停止定位
     */
    public void stopLoc(ITNKLocationCallBack locListener) {
        ToastUtils.INSTANCE().Toast("停止定位!!");
        if(loManager != null)
            loManager.removeLicationListener(locListener);
    }

    /**
     * 开启司乘
     */
    public void startSync() {
        ToastUtils.INSTANCE().Toast("开启司乘");
        if(lsManager != null) {
            lsManager.start();
        }
    }

    /**
     * 结束司乘
     */
    public void stopSync() {
        ToastUtils.INSTANCE().Toast("停止司乘");
        if(lsManager != null) {
            lsManager.stop();
        }
        clearMapUi();
    }

    /**
     * 拉取乘客定位点
     */
    public void startPullPsgPos() {
        ToastUtils.INSTANCE().Toast("拉取乘客位置点");
        if(lsManager != null)
            lsManager.fetchPassengerPositionsEnabled(true);
    }

    /**
     * 停止拉取乘客位置
     */
    public void stopPullPsgPos() {
        ToastUtils.INSTANCE().Toast("停止拉取!!");
        if(lsManager != null)
            lsManager.fetchPassengerPositionsEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastUtils.INSTANCE().destory();
    }
}
