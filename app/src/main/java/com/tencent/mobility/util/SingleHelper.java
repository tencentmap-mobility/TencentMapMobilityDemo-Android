package com.tencent.mobility.util;

import android.content.Context;

import com.tencent.map.navi.car.TencentCarNaviManager;

public class SingleHelper {

    private static TencentCarNaviManager sTencentNaviManager = null;

    public static TencentCarNaviManager getNaviManager(Context context) {
        if (sTencentNaviManager == null) {
            synchronized (SingleHelper.class) {
                if (sTencentNaviManager == null) {
                    sTencentNaviManager = new TencentCarNaviManager(context);
                }
            }
        }
        return sTencentNaviManager;
    }
}
