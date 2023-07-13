package com.tencent.mobility.util;

import android.content.Context;

import com.tencent.navix.api.NavigatorZygote;
import com.tencent.navix.api.navigator.NavigatorDrive;

public class SingleHelper {

    private static NavigatorDrive sTencentNaviManager = null;

    public static NavigatorDrive getNaviManager(Context context) {
        if (sTencentNaviManager == null) {
            synchronized (SingleHelper.class) {
                if (sTencentNaviManager == null) {
                    sTencentNaviManager = NavigatorZygote.with(context).navigator(NavigatorDrive.class);
                }
            }
        }
        return sTencentNaviManager;
    }
}
