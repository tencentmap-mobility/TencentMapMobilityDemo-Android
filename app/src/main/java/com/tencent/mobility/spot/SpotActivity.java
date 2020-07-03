package com.tencent.mobility.spot;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.map.navi.agent.TencentSearchManager;
import com.tencent.map.navi.agent.data.SearchLatLng;
import com.tencent.map.navi.agent.regeo.RegeoOptions;
import com.tencent.map.navi.agent.regeo.beans.RegeoRsp;
import com.tencent.map.navi.agent.regeo.interfaces.RegeoListener;
import com.tencent.recommendspot.TMMRBDataManager;
import com.tencent.recommendspot.TMMRecommendedBoardManager;
import com.tencent.recommendspot.constant.TMMAbsorbedFailedReason;
import com.tencent.recommendspot.recospot.bean.RecommendSpotInfo;
import com.tencent.recommendspot.recospot.bean.TMMLatlng;
import com.tencent.recommendspot.recospot.bean.TMMRecommendSpotBean;
import com.tencent.recommendspot.recospot.bean.TMMSubTraHubBean;
import com.tencent.recommendspot.recospot.bean.TMMTraHubBean
        .TraObjBean.DetailBean.TraHubBean;
import com.tencent.recommendspot.recospot.bean.TMMTraHubBean
        .TraObjBean.DetailBean.TraHubBean.HitSubFenceBean;
import com.tencent.recommendspot.recospot.bean.TMMTraHubBean
        .TraObjBean.DetailBean.TraHubBean.SubFenceBean;
import com.tencent.recommendspot.ui.PointMarkerLayout;
import com.tencent.recommendspot.ui.PointMarkerView;
import com.tencent.tencentmap.mapsdk.maps.CameraUpdate;
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.CameraPosition;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import com.tencent.mobility.R;

public class SpotActivity extends AppCompatActivity {

    private static final String LOG_TAG = ">>tag";

    private static final int REQUEST_FROM = 1;
    private static final int CHANGE_CITY = 2;

    private TMMRecommendedBoardManager pickupSpotManager;

    TencentSearchManager dataManager; // 逆地理编码

    private HubPopWindow popWindow; // 大型枢纽popWindow

    private ChangeCityPopWindow cityPopWindow; // 切换城市pop window

    private PointMarkerLayout pointMarkerLayout;

    private PointMarkerView pointMarkerView;

    private TencentMap tencentMap;

    private MapView mapView;

    private TextView tvCityTxt;

    private TextView tvSearchTxt;

    private String currCity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_spot_layout);

        ActionBar actionBar = super.getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        pointMarkerLayout = findViewById(R.id.tmm_point_marker_kayout);
        pointMarkerView = findViewById(R.id.tmm_point_marker);
        tvCityTxt = findViewById(R.id.change_city);
        tvSearchTxt = findViewById(R.id.search_txt);

        mapView = findViewById(R.id.spot_map);
        tencentMap = mapView.getMap();
        tencentMap.setMapStyle(TencentMap.MAP_TYPE_NAVI);
        tencentMap.getUiSettings().setCompassEnabled(false);
        tencentMap.getUiSettings().setZoomControlsEnabled(false);
        tencentMap.getUiSettings().setMyLocationButtonEnabled(false);
        tencentMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        tencentMap.setDrawPillarWith2DStyle(true);

        // 检索key
        TencentSearchManager.init(this
                , "检索key"
                , "检索sn key");

        pickupSpotManager = new TMMRecommendedBoardManager(tencentMap);
        TMMRecommendedBoardManager.mContext = getApplicationContext();
        pickupSpotManager.setWebServiceKey(null);
        pickupSpotManager.getManagerConfig()
                .isRecommendSpotDefaultUI(true)
                .isOpen(true)
                .setMinMapZoomLevel(10)
                .isAbsorbed(true);
        pickupSpotManager.getUiStyle()
                .setMaxWordsPerLine(10);

        /**
         * 设置地图锚点。
         *
         * <p>用户可使用默认大头针View，
         * 需要相应更改tmm_point_marker layout布局锚点。
         * 用户也可以使用自定义大头针。大头针及动画完全与SDK解耦。
         */
        pickupSpotManager.setCameraCenterProportion(0.5f, 0.25f);
        // 按地图中心点缩放，上车点SDK默认 true
//        tencentMap.getUiSettings().setGestureScaleByMapCenter(true);

        Intent intent = getIntent();
        if(0 != intent.getDoubleExtra("latitude", 0)
                && 0 != intent.getDoubleExtra("longitude", 0)) {
            // 推荐上车点
            getCurrRecommendSpot(intent.getDoubleExtra("latitude", 0)
                    , intent.getDoubleExtra("longitude", 0));
        }

        addListener();

        addAnimListener();

        tencentMap.setOnCameraChangeListener(new TencentMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                pickupSpotManager.onCamerChangeListener(cameraPosition);
            }

            @Override
            public void onCameraChangeFinished(CameraPosition cameraPosition) {
                pickupSpotManager.onCameraChangeFinish(cameraPosition);
                if (cameraPosition != null) {
                    String str = cameraPosition.target.latitude
                            + "," + cameraPosition.target.longitude;
                    tvSearchTxt.setText(str);
                }

            }
        });

    }

    /**
     * 选择城市。
     * @param view
     */
    public void changeCity(View view) {
        if (cityPopWindow == null)
            cityPopWindow = new ChangeCityPopWindow(this
                    , new ChangeCityPopWindow.popWindowListener() {
                @Override
                public void onSave() {
                    if (currCity != null && !currCity.isEmpty())
                        tvCityTxt.setText(currCity);
                }

                @Override
                public void editStr(String etTxt) {
                    currCity = etTxt;
                }
            });
        cityPopWindow.show();
    }

    /**
     * 跳转检索界面。
     */
    public void sugSearch(View view) {
        PoiSearchActivity.searchPoiStart(REQUEST_FROM
                , this
                , tencentMap.getCameraPosition().target
                , tvCityTxt.getText() != null ? tvCityTxt.getText().toString() : "");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(LOG_TAG, "onActivityResult !!");
        if(popWindow != null)
            popWindow.dismiss();

        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_FROM:
                    POIBean poiBean = (POIBean) data.getExtras()
                            .getSerializable("result_poi");
                    if (poiBean != null) {
                        String str = poiBean.lat
                                + "," + poiBean.lng;
                        tvSearchTxt.setText(str);
                        // 挪动地图
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition
                                (new LatLng(poiBean.lat, poiBean.lng), 16, 0f, 0));
                        tencentMap.moveCamera(cameraUpdate);
//                        getCurrRecommendSpot(poiBean.lat, poiBean.lng);
                    }
                    break;
            }
        }
    }

    private void getCurrRecommendSpot(double lat, double lng) {
        String str = lat + "," + lng;
        tvSearchTxt.setText(str);
        pickupSpotManager.getRecommendSpot(pickupSpotManager.getBoardOption()
                .latlng(new TMMLatlng(lat, lng)));
    }

    private void addListener() {

        /**
         * 推荐上车点监听。
         */
        pickupSpotManager.registerRecommendSpotListener(new TMMRBDataManager
                .TMMRecommendedBoardListener() {
            @Override
            public void onRecommendspotSuc(List<TMMRecommendSpotBean.BoardingPointBean> bpBeans) {
                Log.e(LOG_TAG, "推荐上车点 :" + bpBeans.toString());
                stopAnimaLoading();
            }

            @Override
            public void onRecommendspotFail(int errorCode, String errorMsg) {
                Log.e(LOG_TAG, "推荐上车点 errorCode:"
                        + errorCode + ",errorMsg:" + errorMsg);
                stopAnimaLoading();
            }
        });

        /**
         * 吸附状态监听。
         */
        pickupSpotManager.setOnAttachRecommendSpotListener(new TMMRBDataManager.TMMRecommendSpotListener() {
            @Override
            public void onAttachRecommendSpot(RecommendSpotInfo info) {
                if (info.getLatLng() != null) {
                    Log.e(LOG_TAG, "吸附: title:" + info.getTitle()
                            + ", latlng " + info.getLatLng().getLatitude()
                            + ", " + info.getLatLng().getLongitude()
                            + ", isAttach : " + info.isAttach());
                } else {
                    Log.e(LOG_TAG, "吸附: title:" + info.getTitle()
                            + ", latlng  null"
                            + ", isAttach :" + info.isAttach());
                }

            }

            @Override
            public void attachedRecommendSpotFailed(int errCode) {
                // 吸附失败，请求逆地理编码
                LatLng currLatlng = tencentMap.getCameraPosition().target;
                toGeoAddress(currLatlng);

                String errMsg = null;

                switch (errCode) {
                    case TMMAbsorbedFailedReason.REASON_DISTANCE:
                        errMsg = "大于吸附阈值";
                        break;
                    case TMMAbsorbedFailedReason.REASON_MAP_ZOOM_LEVEL:
                        errMsg = "地图zoom level 小于minMapZoomLevel";
                        break;
                    case TMMAbsorbedFailedReason.REASON_QEQUEST_FAIL:
                        errMsg = "请求推荐上车点失败造成的";
                        break;
                    case TMMAbsorbedFailedReason.REASON_ABSOR_BED_IS_FAISE:
                        errMsg = "isAbsorbed为false造成没有吸附";
                        break;
                }

                Log.e(LOG_TAG, "attachedRecommendSpotFailed : " + errMsg);

            }

            @Override
            public void onMovedInTraHub(TraHubBean traHub) {
                Log.e(LOG_TAG, "onMovedInTraHub !!" + "name : "
                        + (traHub != null ? traHub.getName() : null));

                String currFenceName = traHub.getName();
                List<SubFenceBean> subFenceBeans = traHub.getSub_fence();
                List<HitSubFenceBean> hitSubFenceBeans = traHub.getHit_sub_fence();
                HitSubFenceBean currHit = null;
                if (hitSubFenceBeans != null && hitSubFenceBeans.size() != 0)
                    currHit = hitSubFenceBeans.get(0);

                // 弹出框
                showHubPopWindow(currFenceName, subFenceBeans
                        , currHit != null ? currHit.getId() : "-1");
                stopAnimaLoading();
            }

            @Override
            public void onMovedOutTraHub() {
                Log.e(LOG_TAG, "onMovedOutTraHub !!");
                if(popWindow != null)
                    popWindow.dismiss();
            }

        });

        /**
         * 大型枢纽数据监听。
         */
        pickupSpotManager.registerTraHubListener(new TMMRBDataManager
                .TMMTransportationHubListener() {
            @Override
            public void onSubTraHub(TraHubBean trHubaBean) {
                if (trHubaBean == null)
                    return;

                List<SubFenceBean> subFenceBeans = trHubaBean.getSub_fence();
                List<HitSubFenceBean> hitSubFenceBeans = trHubaBean.getHit_sub_fence();
                if (hitSubFenceBeans == null)
                    return;
                List<List<HitSubFenceBean.HitTraHubBean>> subBpModels = new ArrayList<>();
                for (HitSubFenceBean hitSFB : hitSubFenceBeans) {
                    List<HitSubFenceBean.HitTraHubBean> hitTraHubBean = hitSFB.getData();
                    subBpModels.add(hitTraHubBean);
                }

                if(subFenceBeans == null){
                    Log.e(LOG_TAG, "subFenceBeans null");
                    return;
                }

                for(SubFenceBean subBp : subFenceBeans){
                    Log.e(LOG_TAG, "所有2级枢纽: title : "
                            + subBp.getName()
                            + ", id : " + subBp.getId());
                }

                for(HitSubFenceBean hitSubBp : hitSubFenceBeans){
                    Log.e(LOG_TAG, "命中的2级枢纽 : title:"
                            + hitSubBp.getName()
                            + ", id : " + hitSubBp.getId());
                }

                int index = 0;
                for(List<HitSubFenceBean.HitTraHubBean> hitTraHubs : subBpModels){
                    for(HitSubFenceBean.HitTraHubBean htb :hitTraHubs){
                        Log.e(LOG_TAG, "命中2级枢纽上车点 " + index + " : "
                                + "title : " + htb.getTitle()
                                + ";\nid : " +htb.getId()
                                + ";\nlantlng : lat " + htb.getLocation().getLat()
                                + ",lng " + htb.getLocation().getLng()
                                + ";\ndistance : " + htb.getDistance());
                    }
                    index ++;
                }

            }

            @Override
            public void onFail(int errcode, String errMsg) {
                Log.e(LOG_TAG, "errorCode : "
                        + errcode + ",errMsg : " + errMsg);
            }
        });

        /**
         * 获取特定2级围栏数据回调。
         */
        pickupSpotManager.registerSubHubListener(new TMMRBDataManager.TMMSubHubListener() {
            @Override
            public void onSubHubSuc(TMMSubTraHubBean.DataBeanX subHub) {
                Log.e(LOG_TAG, "二级枢纽数据byId: "
                         + subHub.getDetail().getName());
            }

            @Override
            public void onSubHubFail(int errorCode, String errorMsg) {
                Log.e(LOG_TAG, "二级枢纽数据byId: errorCode : "
                         + errorCode + ", errorMsg" + errorMsg);
            }
        });

    }

    /**
     * 逆地理编码逻辑
     */
    private void toGeoAddress(LatLng currentLatlng) {
        if(currentLatlng == null)
            return;
        if(dataManager == null) {
            dataManager = new TencentSearchManager(this);
            dataManager.setRegeoListener(new RegeoListener() {
                @Override
                public void onSuccess(RegeoRsp regeoRsp) {
                    if(regeoRsp != null)
                        Log.e(LOG_TAG, "返地理: " + regeoRsp.getRegeoInfo().getAddress());
                    else
                        Log.e(LOG_TAG, "返地理编码失败");
                }

                @Override
                public void onError(int i, String s) {
                    Log.e(LOG_TAG, "返地理编码失败");
                }
            });
        }

        // 检索的key和snKey
        RegeoOptions addressOptions = new RegeoOptions();
        SearchLatLng locationBean = new SearchLatLng();
        locationBean.setLat(currentLatlng.latitude);
        locationBean.setLng(currentLatlng.longitude);
        addressOptions.setSearchLatLng(locationBean);
        dataManager.getRegeo(addressOptions);

    }

    /**
     * 动画监听。
     */
    private void addAnimListener() {
        pickupSpotManager.setPointAnimaListener(new TMMRecommendedBoardManager
                .TMMPointAnimaListener() {
            @Override
            public void startLoadingAnima() {
                Log.e(LOG_TAG, "startLoadingAnima");
                if(pointMarkerView != null)
                    pointMarkerView.startLoadingAnima();
            }

            @Override
            public void stopLoadingAnima() {
                Log.e(LOG_TAG, "stopLoadingAnima");
                if(pointMarkerView != null)
                    pointMarkerView.stopLoadingAnima();
            }

            @Override
            public void startRippleAnima() {
                Log.e(LOG_TAG, "startRippleAnima");
                if(pointMarkerView != null) {
                    pointMarkerView.stopLoadingAnima();
                    pointMarkerView.startRippleAnima();
                }
            }

            @Override
            public void stopRippleAnima() {
                Log.e(LOG_TAG, "stopRippleAnima");
                if(pointMarkerView != null)
                    pointMarkerView.stopLoadingAnima();
            }

            @Override
            public ObjectAnimator transactionAnimWithMarker() {
                Log.e(LOG_TAG, "transactionAnimWithMarker");
                if(pointMarkerView != null)
                    return pointMarkerView.transactionAnimWithMarker();
                return null;
            }
        });

    }

    private void stopAnimaLoading() {
        if(pickupSpotManager != null && pointMarkerView != null)
            pointMarkerView.stopLoadingAnima();
    }

    private void showHubPopWindow(String currFenceName, List<SubFenceBean> subFenceBeans
            , String hitSubId) {
        if (popWindow == null) {
            popWindow = new HubPopWindow(this
                    , new HubPopWindow.IClickListener() {
                @Override
                public void onClick(String hubId) {
                    pickupSpotManager.getSubHubDataById(hubId);
                }
            });
        }
        popWindow.setSubData(currFenceName, subFenceBeans, hitSubId);
        popWindow.show();
    }

    public void clear(View view) {
        if(pickupSpotManager != null)
            pickupSpotManager.removeSpotMarkers();
    }

    public void open(View view) {
        if(pickupSpotManager != null)
            pickupSpotManager.isOpen(true);
    }

    public void close(View view) {
        if(pickupSpotManager != null)
            pickupSpotManager.isOpen(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkTencentMap())
            mapView.onResume();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (checkTencentMap())
            mapView.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (checkTencentMap())
            mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (checkTencentMap())
            mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if(pickupSpotManager != null)
            pickupSpotManager.destory();
    }

    private boolean checkTencentMap() {
        return mapView != null;
    }
}
