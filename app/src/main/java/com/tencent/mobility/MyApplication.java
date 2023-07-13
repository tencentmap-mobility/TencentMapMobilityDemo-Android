package com.tencent.mobility;

import android.app.Application;

import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.utils.DeviceUtils;
import com.tencent.navix.api.NavigatorConfig;
import com.tencent.navix.api.NavigatorZygote;
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TencentMapInitializer.setAgreePrivacy(this, true);
        NavigatorZygote.with(this).init(NavigatorConfig.builder()
                .setUserAgreedPrivacy(true)
                .setDeviceId(DeviceUtils.getDeviceId(getApplicationContext()))
                .setServiceConfig(NavigatorConfig.ServiceConfig.builder()
                        .build())
                .setMapOptions(NavigatorConfig.MapOptions.builder().build())
                .experiment().setUseSharedMap(false)
                .build());

        TLSConfigPreference.initGlobal(this);
    }
}
