package com.tencent.mobility.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class CommonUtils {

    public static float dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return dp * scale + 0.5f;
    }

    public static float px2dp(Context context, float px) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return px / scale + 0.5f;
    }

    public static void toIntent(Activity a, Class c) {
        Intent intent = new Intent(a, c);
        a.startActivity(intent);
    }

}
