package com.tencent.mobility;

import android.app.Application;

import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.navi.TencentNavi;
import com.tencent.navi.surport.utils.DeviceUtils;
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TencentMapInitializer.setAgreePrivacy(true);
        TencentNavi.setUserAgreePrivacy(true);
        TencentNavi.Config config = new TencentNavi.Config();
        config.setDeviceId(DeviceUtils.getImei(getApplicationContext()));
        TencentNavi.init(this, config);
        TLSConfigPreference.initGlobal(this);
    }

}
