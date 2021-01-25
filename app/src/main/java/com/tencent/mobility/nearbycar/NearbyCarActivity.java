package com.tencent.mobility.nearbycar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.map.carpreview.CarTypeConfig;
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

    // 默认软件园南街
    private LatLng lastLanlng = new LatLng(40.040959,116.272608);

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
        previewMapManager.setWebServiceKey("key", true);
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

        try {
            previewMapManager.setCarTypeConfigMap(getCarTypeConfig());
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

    public void changeCarIcon(View view) {
        HashMap<String, CarTypeConfig> typeResMap = getCarTypeConfig();
        CarTypeConfig carTypeConfig1 = new CarTypeConfig();
        carTypeConfig1.setCarIconBitmap(BitmapFactory
                .decodeResource(getResources(), R.mipmap.ic_launcher));
        carTypeConfig1.setWillRotate(true);
        typeResMap.put("1", carTypeConfig1);
        try {
            previewMapManager.setCarTypeConfigMap(typeResMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, CarTypeConfig> getCarTypeConfig() {
        /*HashMap<String, Integer> typeResMap = new HashMap<>();
        typeResMap.put("1", R.mipmap.car1);
        typeResMap.put("2", R.mipmap.car2);
        typeResMap.put("3", R.mipmap.car3);
        typeResMap.put("4", R.mipmap.car4);
        typeResMap.put("5", R.mipmap.car5);
        try {
            previewMapManager.setCarsTypeResMap(typeResMap); // 设置carType 对应 Res
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        // 可以控制 carType 对应 Res 是否旋转
        // 可替代 PreviewMapManager#setCarsTypeResMap 方法
        HashMap<String, CarTypeConfig> typeResMap = new HashMap<>();
        CarTypeConfig carTypeConfig1 = new CarTypeConfig();
        carTypeConfig1.setRes(R.mipmap.car1);
        carTypeConfig1.setWillRotate(true);

        CarTypeConfig carTypeConfig2 = new CarTypeConfig();
        carTypeConfig2.setRes(R.mipmap.car2);
        carTypeConfig2.setWillRotate(true);

        CarTypeConfig carTypeConfig3 = new CarTypeConfig();
        carTypeConfig3.setRes(R.mipmap.car3);
        carTypeConfig3.setWillRotate(true);

        CarTypeConfig carTypeConfig4 = new CarTypeConfig();
        carTypeConfig4.setRes(R.mipmap.car4);
        carTypeConfig4.setWillRotate(true);

        CarTypeConfig carTypeConfig5 = new CarTypeConfig();
        carTypeConfig5.setRes(R.mipmap.car5);
        carTypeConfig5.setWillRotate(true);

        typeResMap.put("1", carTypeConfig1);
        typeResMap.put("2", carTypeConfig2);
        typeResMap.put("3", carTypeConfig3);
        typeResMap.put("4", carTypeConfig4);
        typeResMap.put("5", carTypeConfig5);

        return typeResMap;
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
        double start, dest;
        if (!((start = tencentLocation.getLatitude()) == 0
                || (dest = tencentLocation.getLongitude()) == 0)) {
            lastLanlng = new LatLng(start, dest);
        }

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
