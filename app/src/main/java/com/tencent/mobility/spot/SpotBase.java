package com.tencent.mobility.spot;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.map.navi.agent.regeo.beans.RegeoRsp;
import com.tencent.mobility.IModel;
import com.tencent.mobility.IPasView;
import com.tencent.mobility.MapBase;
import com.tencent.mobility.location.bean.MapLocation;
import com.tencent.mobility.model.PasLocationModel;
import com.tencent.recommendspot.TMMRBDataManager;
import com.tencent.recommendspot.TMMRecommendedBoardManager;
import com.tencent.recommendspot.recospot.bean.RecommendSpotInfo;
import com.tencent.recommendspot.recospot.bean.TMMLatlng;
import com.tencent.recommendspot.recospot.bean.TMMRecommendSpotBean;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.model.CameraPosition;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import com.tencent.recommendspot.recospot.bean.TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.HitSubFenceBean;
import com.tencent.recommendspot.recospot.bean.TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.SubFenceBean;

import java.util.List;

/**
 * 上车点
 *
 * @author mjzuo
 * @since 20/01/06
 */
public abstract class SpotBase extends MapBase implements IPasView {

    static final String LOG_TAG = "tag1234";

    PasLocationModel loModel;// 定位

    TMMRecommendedBoardManager spotManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getNearbyCarView());

        initSpot();
        getLocation();

        handleSpot();
    }

    @Override
    public void onChange(int eventType, Object obj) {
        switch (eventType) {
            case IModel.LOCATION :
                /**
                 * 移动地图到中心
                 * 注意：因为调用此方法后，sdk内部 onCameraChangeFinished 会自动请求上车点
                 * 不需要重复调用 getRecommendSpot 方法
                 */
                if (obj instanceof MapLocation && tencentMap != null) {
                    if(loModel != null)
                        loModel.stopLocation();
                    setPoi(new LatLng(((MapLocation) obj).getLatitude()
                            , ((MapLocation) obj).getLongitude()));
                }
                break;
        }
    }

    /**
     * 获取定位
     */
    void getLocation() {
        if (loModel == null) {
            loModel = new PasLocationModel(this);
            loModel.register(this);
        }
        loModel.postChangeLocationEvent();// 获取定位点
    }

    /**
     * 获取推荐上车点数据
     * @param latLng
     */
    void getSpot(TMMLatlng latLng) {
        if (spotManager == null)
            return;

        spotManager.getRecommendSpot(spotManager.getBoardOption()
                .latlng(latLng));
    }

    abstract View getNearbyCarView();

    protected abstract MapView getMap();

    abstract void handleSpot();

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
        if (loModel != null)
            loModel.unregister(this);
        loModel = null;
        if (spotManager != null)
            spotManager.destory();
        if (mapView != null)
            mapView.onDestroy();
    }

    private boolean checkTencentMap() {
        return mapView != null;
    }

    /**
     * default config
     */
    private void initSpot() {

        spotManager = new TMMRecommendedBoardManager(tencentMap);
        TMMRecommendedBoardManager.mContext = getApplicationContext();
        spotManager.setWebServiceKey("sn key");

        TMMRBDataManager.initSearchKey("检索key", "检索snKey");

        spotManager.getManagerConfig()
                .isRecommendSpotDefaultUI(true)// 使用默认上车点view
                .isAbsorbed(true);// 允许吸附
        spotManager.getUiStyle()
                .setMaxWordsPerLine(6);

        registerListener();
    }

    private void registerListener() {
        /**
         * 关联cameraChange监听
         */
        tencentMap.setOnCameraChangeListener(new TencentMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                spotManager.onCamerChangeListener(cameraPosition);
            }

            @Override
            public void onCameraChangeFinished(CameraPosition cameraPosition) {
                spotManager.onCameraChangeFinish(cameraPosition);
            }
        });

        /**
         * 推荐上车点数据回调
         */
        spotManager.registerRecommendSpotListener(new TMMRBDataManager.TMMRecommendedBoardListener() {
            @Override
            public void onRecommendspotSuc(List<TMMRecommendSpotBean.BoardingPointBean> bpBeans) {
                Log.e("tag1234", "推荐上车点 bpBeans:" + bpBeans.toString());

            }

            @Override
            public void onRecommendspotFail(int errorCode, String errorMsg) {
                Log.e("tag1234", "推荐上车点 errorCode:" + errorCode
                        + ",errorMsg:" + errorMsg);
            }
        });

        /**
         * 是否命中大型枢纽请求监听
         */
        spotManager.registerTraHubListener(new TMMRBDataManager.TMMTransportationHubListener() {
            @Override
            public void onSubTraHub(List<SubFenceBean> subFenceBeans
                    , List<HitSubFenceBean> hitSubFenceBeans
                    , List<List<HitSubFenceBean.HitTraHubBean>> subBpModels) {
                if (subFenceBeans == null){
                    Log.e(LOG_TAG, "subFenceBeans null");
                    return;
                }

                for (SubFenceBean subBp : subFenceBeans) {
                    Log.e(LOG_TAG, "所有2级枢纽信息呢：title:" +
                            subBp.getName() + "，id:" + subBp.getId());
                }
                for (HitSubFenceBean hitSubBp : hitSubFenceBeans) {
                    Log.e(LOG_TAG, "所有命中的2级枢纽信息：title:"
                            + hitSubBp.getName()
                            + "，id:" + hitSubBp.getId());
                }
                int index = 0;
                for (List<HitSubFenceBean.HitTraHubBean> hitTraHubs : subBpModels) {
                    for (HitSubFenceBean.HitTraHubBean htb :hitTraHubs) {
                        Log.e(LOG_TAG, "所有命中的2级枢纽上车点：" + index
                                + "， title:" + htb.getTitle()
                                + ";\nid:"+htb.getId()+";\nlantlng:lat  "
                                + htb.getLocation().getLat()
                                + ",lng  " + htb.getLocation().getLng()
                                + ";\ndistance:" + htb.getDistance());
                    }
                    index ++;
                }
            }

            @Override
            public void onFail(int errcode, String errMsg) {
                Log.e(LOG_TAG, "errorCode : " + errcode + ",errMsg : " + errMsg);
            }
        });

        /**
         * 吸附状态回调
         */
        spotManager.setOnAttachRecommendSpotListener
                (new TMMRBDataManager.TMMAttachRecommendSpotListener() {
            @Override
            public void onAttachRecommendSpot(RecommendSpotInfo info) {
                if (info.getLatLng() != null) {
                    Log.e(LOG_TAG, "吸附状态回调：title:" + info.getTitle()
                            +", latlng" + info.getLatLng().latitude + "--"
                            + info.getLatLng().getLongitude()
                            + ", isAttach :" + info.isAttach());
                } else {
                    Log.e(LOG_TAG, "吸附状态回调：title:" + info.getTitle()
                            +", latlng  null "
                            + ", isAttach :" + info.isAttach());
                }

            }

            @Override
            public void attachedRecommendSpotFailed(RegeoRsp addressBean) {
                if (addressBean != null)
                    Log.e(LOG_TAG, "返地理 AddressBean: "
                             + addressBean.getMessage());
                else
                    Log.e(LOG_TAG, "返地理编码失败");
            }

        });
    }
}
