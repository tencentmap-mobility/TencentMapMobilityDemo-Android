package com.tencent.mobility;

import android.os.Bundle;
import android.view.View;

import com.tencent.mobility.nearbycar.NearbyCarUi;
import com.tencent.mobility.search.SearchActivity;
import com.tencent.mobility.spot.SpotList;
import com.tencent.mobility.sychro.driver.CarpoolingDriver;
import com.tencent.mobility.sychro.driver.FastDriver;
import com.tencent.mobility.sychro.driver.HitchHikeDriver;
import com.tencent.mobility.synchro_v2.psg.PsgActivity;
import com.tencent.mobility.util.CommonUtils;

public class MainTaskActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_task_activity);
    }

    /**
     * 顺风车-司机端
     */
    public void OnHitchHikeDri(View view) {
        CommonUtils.toIntent(this, HitchHikeDriver.class);
    }

    /**
     * 顺风车-乘客端
     */
    public void OnHitchHikePsg(View view) {
        CommonUtils.toIntent(this, PsgActivity.class);
    }

    /**
     * 快车-司机端
     */
    public void onFastDri(View view) {
        CommonUtils.toIntent(this, FastDriver.class);
    }

    /**
     * 快车-乘客端
     */
    public void onFastPsg(View view) {
        CommonUtils.toIntent(this, PsgActivity.class);
    }

    /**
     * 拼车-司机端
     */
    public void OnCarpoolingDri(View view) {
        CommonUtils.toIntent(this, CarpoolingDriver.class);
    }

    /**
     * 拼车-客户端
     */
    public void OnCarpoolingPsg(View view) {
        CommonUtils.toIntent(this, PsgActivity.class);
    }

    /**
     * 附近车辆
     */
    public void onNearbyCar(View view) {
        CommonUtils.toIntent(this, NearbyCarUi.class);
    }

    /**
     * 推荐上车点
     */
    public void onCarPoi(View view) {
        CommonUtils.toIntent(this, SpotList.class);
    }

    /**
     * 检索
     */
    public void onSearch(View view) {
        CommonUtils.toIntent(this, SearchActivity.class);
    }

}
