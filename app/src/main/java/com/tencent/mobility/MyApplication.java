package com.tencent.mobility;

import android.app.Application;

import com.tencent.map.navi.TencentNavi;
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TencentMapInitializer.setAgreePrivacy(true);
        TencentNavi.setUserAgreePrivacy(true);
    }

}
