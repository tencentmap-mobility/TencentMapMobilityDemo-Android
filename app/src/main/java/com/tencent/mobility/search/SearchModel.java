package com.tencent.mobility.search;

import com.tencent.map.navi.agent.TencentSearchManager;
import com.tencent.map.navi.agent.data.SearchLatLng;
import com.tencent.map.navi.agent.regeo.RegeoOptions;
import com.tencent.map.navi.agent.regeo.interfaces.RegeoListener;
import com.tencent.map.navi.agent.routes.DrivingOptions;
import com.tencent.map.navi.agent.routes.WalkingOptions;
import com.tencent.map.navi.agent.routes.interfaces.DrivingRouteListener;
import com.tencent.map.navi.agent.routes.interfaces.WalkingRouteListener;
import com.tencent.map.navi.agent.sug.SugOptions;
import com.tencent.map.navi.agent.sug.interfaces.SugListener;

public class SearchModel implements IModel {

    IView mView;

    boolean isHasKey;

    public SearchModel(IView view) {
        this.mView = view;
        mView.setModel(this);
        init();
    }

    @Override
    public void init() {
        TencentSearchManager.init(mView.getAppContext()
                , "key"
                , "secretKey");
        // 在这里设置标志位的目的，是提醒用户设置key
        isHasKey = false;
    }

    /**
     * sug请求
     */
    @Override
    public void sugRequest(SugListener sugListener) {
        if(!isHasKey) {
            sugListener.onError(-1, "no key");
            return;
        }

        //创建对象
        TencentSearchManager dataManager = new TencentSearchManager(mView.getAppContext());
        dataManager.setSugListener(sugListener);
        //请求参数的封装（关键词、根据名称限制区域范围等）
        SugOptions sugOptions = new SugOptions();
        SearchLatLng locationBean = new SearchLatLng();
        locationBean.setLat(40.034852);// demo就写死了
        locationBean.setLng(116.319820);
        sugOptions.setPolicy("1")
                .setRegion("北京")
                .setKeyword("之春里")
                .setLocation(locationBean);
        dataManager.getSug(sugOptions);
    }

    /**
     * 逆地理编码
     *
     * @param regeoListener
     */
    @Override
    public void regeoRequest(RegeoListener regeoListener) {
        if(!isHasKey) {
            regeoListener.onError(-1, "no key");
            return;
        }

        TencentSearchManager dataManager = new TencentSearchManager(mView.getAppContext());
        dataManager.setRegeoListener(regeoListener);
        RegeoOptions addressOptions = new RegeoOptions();
        SearchLatLng locationBean = new SearchLatLng();
        locationBean.setLat(40.034852);
        locationBean.setLng(116.319820);
        addressOptions.setSearchLatLng(locationBean);
        dataManager.getRegeo(addressOptions);
    }

    /**
     * 驾车路线
     *
     * @param drivingListener
     */
    @Override
    public void drivingRequest(DrivingRouteListener drivingListener) {
        if(!isHasKey) {
            drivingListener.onError(-1, "no key");
            return;
        }

        TencentSearchManager dataManager = new TencentSearchManager(mView.getAppContext());
        dataManager.setDrivingRouteListener(drivingListener);
        DrivingOptions drivingOptions = new DrivingOptions();
        SearchLatLng from = new SearchLatLng();
        from.setLat(40.034852);
        from.setLng(116.319820);
        drivingOptions.setFrom(from);
        SearchLatLng to = new SearchLatLng();
        to.setLat(40.034852);
        to.setLng(117.319820);
        drivingOptions.setTo(to);
        dataManager.getDriving(drivingOptions);
    }

    /**
     * 步行路线
     *
     * @param walkingRouteListener
     */
    @Override
    public void walkingRequest(WalkingRouteListener walkingRouteListener) {
        if(!isHasKey) {
            walkingRouteListener.onError(-1, "no key");
            return;
        }

        TencentSearchManager dataManager = new TencentSearchManager(mView.getAppContext());
        dataManager.setWalkingRouteListener(walkingRouteListener);
        WalkingOptions walkingOptions = new WalkingOptions();
        SearchLatLng from = new SearchLatLng(40.034852, 116.319820);
        walkingOptions.setFrom(from);
        SearchLatLng to = new SearchLatLng(40.034852, 117.319820);
        walkingOptions.setTo(to);
        dataManager.getWalking(walkingOptions);
    }

}
