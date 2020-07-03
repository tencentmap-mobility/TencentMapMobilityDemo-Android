package com.tencent.mobility;

import android.os.Bundle;
import android.view.View;

import com.tencent.mobility.nearbycar.NearbyCarActivity;
import com.tencent.mobility.search.SearchActivity;
import com.tencent.mobility.spot.SpotActivity;
import com.tencent.mobility.synchro_v2.driver.CarpoolingDriver;
import com.tencent.mobility.synchro_v2.driver.FastDriver;
import com.tencent.mobility.synchro_v2.driver.HitchHikeDriver;
import com.tencent.mobility.synchro_v2.psg.PsgActivity;
import com.tencent.mobility.util.CommonUtils;

public class MainTaskActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_task_activity);
    }

    public void OnHitchHikeDri(View view) {
        // 顺风车-司机端
        CommonUtils.toIntent(this, HitchHikeDriver.class);
    }

    public void OnHitchHikePsg(View view) {
        // 顺风车-乘客端
        CommonUtils.toIntent(this, PsgActivity.class);
    }

    public void onFastDri(View view) {
        // 快车-司机端
        CommonUtils.toIntent(this, FastDriver.class);
    }

    public void onFastPsg(View view) {
        // 快车-乘客端
        CommonUtils.toIntent(this, PsgActivity.class);
    }

    public void OnCarpoolingDri(View view) {
        // 拼车-司机端
        CommonUtils.toIntent(this, CarpoolingDriver.class);
    }

    public void OnCarpoolingPsg(View view) {
        // 拼车-客户端
        CommonUtils.toIntent(this, PsgActivity.class);
    }

    public void onNearbyCar(View view) {
        // 附近车辆
        CommonUtils.toIntent(this, NearbyCarActivity.class);
    }

    public void onCarPoi(View view) {
        // 推荐上车点
        CommonUtils.toIntent(this, SpotActivity.class);
    }

    public void onSearch(View view) {
        // 检索
        CommonUtils.toIntent(this, SearchActivity.class);
    }

}
