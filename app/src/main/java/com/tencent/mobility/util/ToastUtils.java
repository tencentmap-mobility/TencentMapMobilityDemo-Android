package com.tencent.mobility.util;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;


public class ToastUtils {

    static Context mContext;
    private static ToastUtils mToast;

    private ToastUtils(){}

    public static ToastUtils instance() {
        if(mToast == null) {
            synchronized (ToastUtils.class){
                if(mToast == null){
                    mToast = new ToastUtils();
                }
            }
        }
        return mToast;
    }

    public static void init(Context applicationContext) {
        mContext = applicationContext;
    }

    public void destory() {
        mContext = null;
        mToast = null;
    }

    public void toast(String msg) {
        if (!TextUtils.isEmpty(msg) && mContext != null) {
            Toast.makeText(mContext.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
