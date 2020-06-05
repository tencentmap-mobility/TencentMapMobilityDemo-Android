package com.tencent.mobility.search;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.map.navi.agent.regeo.beans.RegeoRsp;
import com.tencent.map.navi.agent.regeo.interfaces.RegeoListener;
import com.tencent.map.navi.agent.routes.beans.DrivingRouteRsp;
import com.tencent.map.navi.agent.routes.beans.WalkingRouteRsp;
import com.tencent.map.navi.agent.routes.interfaces.DrivingRouteListener;
import com.tencent.map.navi.agent.routes.interfaces.WalkingRouteListener;
import com.tencent.map.navi.agent.sug.beans.SugRsp;
import com.tencent.map.navi.agent.sug.interfaces.SugListener;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;

/**
 * 检索sdk
 *
 * 运行demo前，请设置key
 */
public class SearchActivity extends BaseActivity implements IView {

    private static final String LOG_TAG = "search1234";

    IModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);
        new SearchModel(this);
    }

    @Override
    public void setModel(IModel model) {
        this.model = model;
    }

    public void sugSearch(View view) {
        if(model != null)
            model.sugRequest(new SugListener() {
                @Override
                public void onSuccess(SugRsp sugRsp) {
                    Log.e(LOG_TAG, "sug suc !!");
                }

                @Override
                public void onError(int i, String s) {
                    Log.e(LOG_TAG, "sug err : " + s + " !!");
                }
            });
    }

    public void reGroSearch(View view) {
        if(model != null)
            model.regeoRequest(new RegeoListener() {
                @Override
                public void onSuccess(RegeoRsp regeoRsp) {
                    Log.e(LOG_TAG, "re geo suc !!");
                }

                @Override
                public void onError(int i, String s) {
                    Log.e(LOG_TAG, "re geo err : " + s + " !!");
                }
            });
    }

    public void routeDriSearch(View view) {
        if(model != null)
            model.drivingRequest(new DrivingRouteListener() {
                @Override
                public void onSuccess(DrivingRouteRsp drivingRouteRsp) {
                    Log.e(LOG_TAG, "route drive suc !!");
                }

                @Override
                public void onError(int i, String s) {
                    Log.e(LOG_TAG, "route drive err : " + s + " !!");
                }
            });
    }

    public void routeWalkSearch(View view) {
        if(model != null)
            model.walkingRequest(new WalkingRouteListener() {
                @Override
                public void onSuccess(WalkingRouteRsp walkingRouteRsp) {
                    Log.e(LOG_TAG, "route walk suc !!");
                }

                @Override
                public void onError(int i, String s) {
                    Log.e(LOG_TAG, "route walk err : " + s + " !!");
                }
            });
    }

    @Override
    public Context getAppContext() {
        return getApplicationContext();
    }
}
