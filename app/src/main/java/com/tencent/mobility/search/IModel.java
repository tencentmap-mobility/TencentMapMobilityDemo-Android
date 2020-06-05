package com.tencent.mobility.search;

import com.tencent.map.navi.agent.regeo.interfaces.RegeoListener;
import com.tencent.map.navi.agent.routes.interfaces.DrivingRouteListener;
import com.tencent.map.navi.agent.routes.interfaces.WalkingRouteListener;
import com.tencent.map.navi.agent.sug.interfaces.SugListener;

public interface IModel {

    /**
     * 检索SDK初始化工作。
     */
    void init();

    /**
     * sug请求。
     */
    void sugRequest(SugListener sugListener);

    /**
     * 逆地理编码请求。
     */
    void regeoRequest(RegeoListener regeoListener);

    /**
     * 驾车路线规划请求。
     */
    void drivingRequest(DrivingRouteListener drivingListener);

    /**
     * 步行路线规划请求。
     */
    void walkingRequest(WalkingRouteListener walkingListeer);

}
