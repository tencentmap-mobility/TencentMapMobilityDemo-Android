package com.tencent.mobility.spot;

import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.tencent.mobility.R;
import com.tencent.recommendspot.TMMRBDataManager;
import com.tencent.recommendspot.TMMRecommendedBoardManager;
import com.tencent.recommendspot.recospot.bean.TMMTraHubBean;
import com.tencent.recommendspot.ui.PointMarkerView;
import com.tencent.tencentmap.mapsdk.maps.MapView;

import java.util.List;

/**
 * 使用提供的PointMarkerView
 *
 * @author mjzuo
 */
public class SpotBaseUi extends SpotBase {

    static String LOG_TAG = ">>tag1234";

    View spotView;

    PointMarkerView dotPoint;

    @Override
    View getNearbyCarView() {
        if(spotView == null)
            return null;
        dotPoint = spotView.findViewById(R.id.point_marker);
        return spotView;
    }

    @Override
    protected MapView getMap() {
        if(spotView == null)
            spotView = LayoutInflater.from(this).inflate(R.layout.pickup_spot_ui, null);
        return spotView.findViewById(R.id.spot_map);
    }

    @Override
    void handleSpot() {
        if(spotView == null || dotPoint == null)
            return;
        spotManager.setPointAnimaListener(new TMMRecommendedBoardManager.TMMPointAnimaListener() {
            @Override
            public void startLoadingAnima() {
                dotPoint.startLoadingAnima();
            }

            @Override
            public void stopLoadingAnima() {
                dotPoint.stopLoadingAnima();
            }

            @Override
            public void startRippleAnima() {
                dotPoint.startRippleAnima();
            }

            @Override
            public void stopRippleAnima() {
                dotPoint.stopRippleAnima();
            }

            @Override
            public ObjectAnimator transactionAnimWithMarker() {
                if(dotPoint != null)
                    return dotPoint.transactionAnimWithMarker();
                return null;
            }
        });

        /**
         * 是否命中大型枢纽请求监听
         */
        spotManager.registerTraHubListener(new TMMRBDataManager.TMMTransportationHubListener() {
            @Override
            public void onSubTraHub(List<TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.SubFenceBean> subFenceBeans
                    , List<TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.HitSubFenceBean> hitSubFenceBeans
                    , List<List<TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.HitSubFenceBean.HitTraHubBean>> subBpModels) {
                if(subFenceBeans == null){
                    Log.e(LOG_TAG, "subFenceBeans null");
                    return;
                }

                for(TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.SubFenceBean subBp : subFenceBeans){
                    Log.e(LOG_TAG, "所有2级枢纽信息呢：title:" + subBp.getName() + "，id:" + subBp.getId());
                }
                for(TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.HitSubFenceBean hitSubBp : hitSubFenceBeans){
                    Log.e(LOG_TAG, "所有命中的2级枢纽信息：title:" + hitSubBp.getName()
                            + "，id:" + hitSubBp.getId());
                }
                int index = 0;
                for(List<TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.HitSubFenceBean.HitTraHubBean> hitTraHubs : subBpModels){
                    for(TMMTraHubBean.TraObjBean.DetailBean.TraHubBean.HitSubFenceBean.HitTraHubBean htb :hitTraHubs){
                        Log.e(LOG_TAG, "所有命中的2级枢纽上车点：" + index + "， title:" + htb.getTitle()
                                + ";\nid:"+htb.getId()+";\nlantlng:lat  " + htb.getLocation().getLat()
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
    }
}
