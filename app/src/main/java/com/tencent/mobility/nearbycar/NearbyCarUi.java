package com.tencent.mobility.nearbycar;

import android.view.LayoutInflater;
import android.view.View;

import com.tencent.map.carpreview.ui.TencentCarsMap;
import com.tencent.mobility.R;

public class NearbyCarUi extends NearbyCarBase {

    View carView;

    @Override
    View getNearbyCarView() {
        carView = LayoutInflater.from(this).inflate(R.layout.nearby_car_ui, null);
        return carView;
    }

    @Override
    TencentCarsMap getCarsMap() {
        return carView.findViewById(R.id.cars_map);
    }
}
