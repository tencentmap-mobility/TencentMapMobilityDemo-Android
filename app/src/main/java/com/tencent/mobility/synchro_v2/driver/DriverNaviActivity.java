package com.tencent.mobility.synchro_v2.driver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.DriDataListener;
import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.map.navi.car.CarNaviView;
import com.tencent.map.navi.car.CarRouteSearchOptions;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.map.navi.data.NaviPoi;
import com.tencent.map.navi.data.RouteData;
import com.tencent.map.navi.ui.car.CarNaviInfoPanel;
import com.tencent.mobility.BaseActivity;
import com.tencent.mobility.R;
import com.tencent.mobility.synchro_v2.helper.SingleHelper;

import java.util.ArrayList;

public class DriverNaviActivity extends BaseActivity {

    private static final String LOG_TAG = "navi1234";

    // 这是司机的起终点
    NaviPoi from = new NaviPoi(40.041032,116.27245);
    NaviPoi to = new NaviPoi(39.868699,116.32198);

    TSLDExtendManager lsManager;// 司乘管理类
    TencentCarNaviManager mNaviManager;// 导航
    CarNaviView carNaviView;

    int curRouteIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ls_layout_navi_layout);
        carNaviView = findViewById(R.id.navi_car_view);

        curRouteIndex = getIntent().getIntExtra("route_index", 0);

        mNaviManager = SingleHelper.getNaviManager(getApplicationContext());
        // 可设置途经点bitmap
        carNaviView.configWayPointMarkerpresentation(getWayMarker());
        mNaviManager.addNaviView(carNaviView);
        lsManager = TSLDExtendManager.getInstance();
        // lsManager.setNaviManager(mNaviManager); // demo ls是单例，在"听单页"已经被set
        lsManager.addTLSDriverListener(new MyDriverListener());// 数据callback
        lsManager.addRemoveWayPointCallBack(new DriDataListener.IRemoveWayByUserCallBack() {
            @Override
            public void onRemoveWayPoint(ArrayList<TLSDWayPointInfo> wayPoints) {
                // 剔除途经点的回调
                Log.e(LOG_TAG, ">>>onRemoveWayPoint !!");
                // app->停止导航，重新算路，开始导航
                mNaviManager.stopNavi();
                // from:当前司机起点,这里测试就写死了
                // 开始算路
                lsManager.searchCarRoutes(from, to, wayPoints
                        , CarRouteSearchOptions.create(), new MyDropWayListener());
            }
        });

        mNaviManager.setInternalTtsEnabled(true);
        CarNaviInfoPanel carNaviInfoPanel = carNaviView.showNaviInfoPanel();// 默认ui
        carNaviInfoPanel.setOnNaviInfoListener(new CarNaviInfoPanel.OnNaviInfoListener() {
            @Override
            public void onBackClick() {
                mNaviManager.stopSimulateNavi();
                finish();
            }
        });

        startSimulateNavi();
    }

    /**
     * 开启模拟导航
     */
    public void startSimulateNavi() {
        try {
            // 开始模拟导航
            mNaviManager.startSimulateNavi(curRouteIndex);
        }catch (Exception e) {
            Log.e(LOG_TAG, "start navi err : " + e.getMessage());
        }
    }

    /**
     * 剔除途经点上车点
     * @param view
     */
    public void RemoveWayStart(View view) {
        if(lsManager != null)
            lsManager.arrivedPassengerStartPoint("test_passenger_order_000011");
    }

    /**
     * 剔除途经点下车点
     * @param view
     */
    public void RemoveWayEnd(View view) {
        if(lsManager != null)
            lsManager.arrivedPassengerEndPoint("test_passenger_order_000011");
    }

    @Override
    public void onStart() {
        if (carNaviView != null) {
            carNaviView.onStart();
        }
        super.onStart();
    }

    @Override
    public void onRestart() {
        if (carNaviView != null) {
            carNaviView.onRestart();
        }
        super.onRestart();
    }

    @Override
    public void onResume() {
        if (carNaviView != null) {
            carNaviView.onResume();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (carNaviView != null) {
            carNaviView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (carNaviView != null) {
            carNaviView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (carNaviView != null) {
            carNaviView.onDestroy();
        }
        super.onDestroy();
    }

    /**
     * 设置途经点图片
     */
    private ArrayList<Bitmap> getWayMarker() {
        ArrayList<Bitmap> bps = new ArrayList<>();
        bps.add(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint1_1));
        bps.add(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint1_2));
        bps.add(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint2_1));
        bps.add(BitmapFactory.decodeResource(getResources(), R.mipmap.waypoint2_2));
        return bps;
    }

    class MyDropWayListener implements DriDataListener.ISearchCallBack {
        @Override
        public void onParamsInvalid(int errCode, String errMsg) {
            Log.e(LOG_TAG, ">>>onParamsInvalid !!");
        }

        @Override
        public void onRouteSearchFailure(int i, String s) {
            Log.e(LOG_TAG, ">>>onRouteSearchFailure !!");
        }

        @Override
        public void onRouteSearchSuccess(ArrayList<RouteData> arrayList) {
            curRouteIndex = 0;
            startSimulateNavi();// 开启模拟导航
        }
    }

    /**
     * 数据回调
     */
    class MyDriverListener implements DriDataListener.ITLSDriverListener {
        @Override
        public void onPushRouteSuc() {
            Log.e(LOG_TAG, "navigation onPushRouteSuc()");
        }

        @Override
        public void onPushRouteFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "navigation onPushRouteFail()");
        }

        @Override
        public void onPushPositionSuc() {
            Log.e(LOG_TAG, "navigation onPushPositionSuc()");
        }

        @Override
        public void onPushPositionFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "navigation onPushPositionFail()");
        }

        @Override
        public void onPullLsInfoSuc(ArrayList<TLSBPosition> los) {
            Log.e(LOG_TAG, "navigation onPullLsInfoSuc()");
        }

        @Override
        public void onPullLsInfoFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, "navigation onPullLsInfoFail()");
        }
    }

}
