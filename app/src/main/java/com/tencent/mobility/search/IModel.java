package com.tencent.mobility.search;

import com.tencent.map.navi.agent.regeo.interfaces.RegeoListener;
import com.tencent.map.navi.agent.routes.beans.DrivingRouteRsp;
import com.tencent.map.navi.agent.routes.beans.WalkingRouteRsp;
import com.tencent.map.navi.agent.routes.interfaces.DrivingRouteListener;
import com.tencent.map.navi.agent.routes.interfaces.WalkingRouteListener;
import com.tencent.map.navi.agent.sug.interfaces.SugListener;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.util.ArrayList;

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

    /**
     * 绘制驾车路线接口。
     *
     * @param routeRsp
     * @param selectedIndex
     * @return
     */
    ArrayList<PolylineOptions> handleRouteData(DrivingRouteRsp routeRsp
            , int selectedIndex);

    /**
     * 绘制步行路线接口。
     *
     * @param routeRsp
     * @param selectedIndex
     * @return
     */
    ArrayList<PolylineOptions> handleRouteData(WalkingRouteRsp routeRsp
            , int selectedIndex);

}
