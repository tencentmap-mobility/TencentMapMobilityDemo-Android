package com.tencent.mobility.nearbycar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.map.carpreview.PreviewMapManager;
import com.tencent.map.carpreview.nearby.beans.NearbyBean;
import com.tencent.map.carpreview.nearby.contract.INearbyListener;
import com.tencent.map.carpreview.ui.TencentCarsMap;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.CameraPosition;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;

import com.tencent.mobility.R;

public class NearbyCarActivity extends AppCompatActivity
        implements TencentLocationListener {

    public static String TAG = ">>Tag";

    private TencentCarsMap mTencentCarsMap;

    private PreviewMapManager previewMapManager;

    private LatLng lastLanlng;

    /**
     * 需要将地图挪动监听，同步给周边车辆SDK
     */
    private TencentMap.OnCameraChangeListener cameraChangeListener
            = new TencentMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {

        }

        @Override
        public void onCameraChangeFinished(CameraPosition cameraPosition) {
            if (previewMapManager != null)
                previewMapManager.onCameraChangeFinish(cameraPosition);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nearby_car_layout);

        ActionBar actionBar = super.getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        initPermission();

        mTencentCarsMap = findViewById(R.id.cars_map);

        PreviewMapManager.init(this);
        previewMapManager = new PreviewMapManager();
        // 可通过setKey接口，动态设置key
//        previewMapManager.setKey("key");
        // 签名校验
        previewMapManager.setWebServiceKey("key"
                , true);
        previewMapManager.isOpenLog(true);

        previewMapManager.setCarsCount(10);
        previewMapManager.setRadius(100);
        previewMapManager.setCity(110000);
        ArrayList types = new ArrayList();
        types.add("1");
        types.add("2");
        types.add("3");
        types.add("4");
        types.add("5");
        types.add("6");
        previewMapManager.setCarsType(types);
        previewMapManager.setMock(true);
        previewMapManager.attachCarsMap(mTencentCarsMap);

        HashMap<String, Integer> typeResMap = new HashMap<>();
        typeResMap.put("1", R.mipmap.car1);
        typeResMap.put("2", R.mipmap.car2);
        typeResMap.put("3", R.mipmap.car3);
        typeResMap.put("4", R.mipmap.car4);
        typeResMap.put("5", R.mipmap.car5);
        try {
            previewMapManager.setCarsTypeResMap(typeResMap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        addListener();

        // 添加地图拖动监听
        mTencentCarsMap.getTencentMap().setOnCameraChangeListener(cameraChangeListener);

    }

    public void location(View view) {
        getLocation();
    }

    public void getLocation() {
        TencentLocationRequest request = TencentLocationRequest.create();
        request.setInterval(200);
        TencentLocationManager locationManager = TencentLocationManager
                .getInstance(NearbyCarActivity.this);
        int error = locationManager.requestSingleFreshLocation(request
                , this
                , getMainLooper());
        Log.d(TAG, "getLocation : " + error);
    }

    @Override
    public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
        lastLanlng = new LatLng(tencentLocation.getLatitude()
                , tencentLocation.getLongitude());
        Log.d(TAG, "onLocationChanged lat : " + tencentLocation.getLatitude()
                + " lng : " + tencentLocation.getLongitude());

        try {
            // 周边车辆
            previewMapManager.setCurrentLatLng(lastLanlng);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusUpdate(String s, int i, String s1) {

    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            String[] permissions = {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            if (checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, 0);
            } else {
                getLocation();
            }
        }
    }

    private void addListener() {
        previewMapManager.registerNearbyCarsListener(new INearbyListener() {
            @Override
            public void onNearbyDataSu(ArrayList<NearbyBean.DriversBean> driversBeans) {
                Log.e("tag123", "driversBeans : " + driversBeans.toString());
            }

            @Override
            public void onNearbyDataErr(String msg) {
                Log.e("tag123", "onNearbyDataErr : " + msg);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mTencentCarsMap != null)
            mTencentCarsMap.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mTencentCarsMap != null)
            mTencentCarsMap.onStart();
        previewMapManager.startRefresh(30);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTencentCarsMap != null)
            mTencentCarsMap.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mTencentCarsMap != null)
            mTencentCarsMap.onStop();
        previewMapManager.stopRefresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode
            , String[] permissions
            , int[] grantResults) {
        super.onRequestPermissionsResult(requestCode
                , permissions
                , grantResults);
        getLocation();
    }

}
