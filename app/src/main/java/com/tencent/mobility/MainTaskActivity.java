package com.tencent.mobility;

import android.os.Bundle;
import android.view.View;

import com.tencent.mobility.nearbycar.NearbyCarActivity;
import com.tencent.mobility.search.SearchActivity;
import com.tencent.mobility.spot.SpotActivity;
import com.tencent.mobility.synchro_v2.CarpoolingNormalActivity;
import com.tencent.mobility.synchro_v2.ChangeDestinationActivity;
import com.tencent.mobility.synchro_v2.FastCarActivity;
import com.tencent.mobility.synchro_v2.FastCarNormalActivity;
import com.tencent.mobility.synchro_v2.FastCarNormalDriverActivity;
import com.tencent.mobility.synchro_v2.HitchHikeNormalActivity;
import com.tencent.mobility.synchro_v2.HitchHikeOneVipActivity;
import com.tencent.mobility.synchro_v2.driver.DriverRelayOrderActivity;
import com.tencent.mobility.synchro_v2.driver.DriverRelayOrderAsyncActivity;
import com.tencent.mobility.synchro_v2.driver.DriverRelayOrderUnrestrictedActivity;
import com.tencent.mobility.synchro_v2.psg.PassengerSelectRoutesActivity;
import com.tencent.mobility.util.CommonUtils;

public class MainTaskActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_task_activity);
    }

    public void OnHitchHikeSingle(View view) {
        // 顺风车-单人
        CommonUtils.toIntent(this, HitchHikeOneVipActivity.class);
    }

    public void OnHitchHikeMulti(View view) {
        // 顺风车-多人
        CommonUtils.toIntent(this, HitchHikeNormalActivity.class);
    }

    public void onFastCar(View view) {
        // 快车场景
        CommonUtils.toIntent(this, FastCarActivity.class);
    }

    public void onFastNormalCar(View view) {
        // 快车场景
        CommonUtils.toIntent(this, FastCarNormalActivity.class);
    }

    public void onFastNormalCarDriver(View view) {
        // 快车场景-司机
        CommonUtils.toIntent(this, FastCarNormalDriverActivity.class);
    }

    public void OnCarpooling(View view) {
        // 拼车-至少两人
        CommonUtils.toIntent(this, CarpoolingNormalActivity.class);
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

    public void onRelayOrder(View view) {
        CommonUtils.toIntent(this, DriverRelayOrderActivity.class);
    }

    public void onRelayOrderAsync(View view) {
        CommonUtils.toIntent(this, DriverRelayOrderAsyncActivity.class);
    }

    public void onRelayOrderUnrestricted(View view) {
        CommonUtils.toIntent(this, DriverRelayOrderUnrestrictedActivity.class);
    }

    public void onSelectRoutes(View view) {
        CommonUtils.toIntent(this, PassengerSelectRoutesActivity.class);
    }

    public void onChangeDestination(View view) {
        CommonUtils.toIntent(this, ChangeDestinationActivity.class);
    }

}
