package com.tencent.mobility.search;

import android.content.Context;

import com.tencent.map.navi.agent.TencentSearchManager;
import com.tencent.map.navi.agent.data.SearchLatLng;
import com.tencent.map.navi.agent.regeo.RegeoOptions;
import com.tencent.map.navi.agent.regeo.interfaces.RegeoListener;
import com.tencent.map.navi.agent.routes.DrivingOptions;
import com.tencent.map.navi.agent.routes.WalkingOptions;
import com.tencent.map.navi.agent.routes.beans.DrivingRouteData;
import com.tencent.map.navi.agent.routes.beans.DrivingRouteRsp;
import com.tencent.map.navi.agent.routes.beans.DrivingTrafficItem;
import com.tencent.map.navi.agent.routes.beans.WalkingRouteData;
import com.tencent.map.navi.agent.routes.beans.WalkingRouteRsp;
import com.tencent.map.navi.agent.routes.interfaces.DrivingRouteListener;
import com.tencent.map.navi.agent.routes.interfaces.WalkingRouteListener;
import com.tencent.map.navi.agent.sug.SugOptions;
import com.tencent.map.navi.agent.sug.interfaces.SugListener;
import com.tencent.mobility.search.helper.SearchConvertHelper;
import com.tencent.mobility.util.RouteUtils;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class SearchModel implements IModel {

    IView mView;

    public SearchModel(IView view) {
        this.mView = view;
        mView.setModel(this);
        init();
    }

    @Override
    public void init() {
        TencentSearchManager.init(mView.getAppContext()
                , "key"
                , "sn key");
    }

    /**
     * sug请求。
     */
    @Override
    public void sugRequest(SugListener sugListener) {
        TencentSearchManager dataManager = new TencentSearchManager(mView.getAppContext());
        // 添加监听
        dataManager.setSugListener(sugListener);
        /**
         * policy:设置sug检索策略。
         * <ul>目前仅支持：
         * <li>policy=1：出行场景（网约车） – 起点查询
         * <li>policy=2：出行场景（网约车） – 终点查询
         * </ul>
         *
         * <p>region:设置限制城市范围，根据城市名称限制地域范围。
         * 如，仅获取"广州市"范围内的提示内容，则region = "广州"
         *
         * <p>keyword:设置用户输入的关键词（希望获取后续提示的关键词）。
         * 如，keyword = "南方"
         *
         * <p>location:设置位置，即定位坐标。
         * 传入后，若用户搜索关键词为类别词（如酒店、餐馆时），
         * 与此坐标距离近的地点将靠前显示。
         * 当policy=1时，此参数必填。
         *
         * <p> 更多参数信息，可查看{@link SugOptions}接口文档。
         */
        SugOptions sugOptions = new SugOptions();
        SearchLatLng locationBean = new SearchLatLng();
        locationBean.setLat(40.034852); // demo就写死了
        locationBean.setLng(116.319820);
        sugOptions.setPolicy("1")
                .setRegion("北京")
                .setKeyword("之春里")
                .setRegionFix(0)
                .setLocation(locationBean);
        dataManager.getSug(sugOptions);
    }

    /**
     * 逆地理编码。
     */
    @Override
    public void regeoRequest(RegeoListener regeoListener) {
        TencentSearchManager dataManager = new TencentSearchManager(mView.getAppContext());
        // 添加监听
        dataManager.setRegeoListener(regeoListener);
        // 提供给开发者的逆地址解析参数配置类。
        RegeoOptions addressOptions = new RegeoOptions();
        // 设置位置坐标。
        SearchLatLng locationBean = new SearchLatLng(40.034852, 116.319820);
        /**
         * 更多默认参数，参考接口文档{@link RegeoOptions}
         */
        addressOptions.setSearchLatLng(locationBean);
        dataManager.getRegeo(addressOptions);
    }

    /**
     * 驾车路线规划。
     */
    @Override
    public void drivingRequest(DrivingRouteListener drivingListener) {
        TencentSearchManager dataManager = new TencentSearchManager(mView.getAppContext());
        // 添加监听
        dataManager.setDrivingRouteListener(drivingListener);
        // 提供给开发者的驾车路线规划参数配置类。
        DrivingOptions drivingOptions = new DrivingOptions();
        // 设置起点位置坐标
        drivingOptions.setFrom(new SearchLatLng(40.034852, 116.319820));
        // 设置终点位置坐标
        drivingOptions.setTo(new SearchLatLng(40.034852, 117.319820));
        // 设置途径点
        //drivingOptions.setWaypoints(new ArrayList<SearchLatLng>());
        /**
         * 更多默认参数，可参数接口文档{@link DrivingOptions}
         */
        dataManager.getDriving(drivingOptions);
    }

    /**
     * 步行路线规划。
     */
    @Override
    public void walkingRequest(WalkingRouteListener walkingRouteListener) {
        TencentSearchManager dataManager = new TencentSearchManager(mView.getAppContext());
        // 添加监听
        dataManager.setWalkingRouteListener(walkingRouteListener);
        // 提供给开发者的步行路线规划参数配置类。
        WalkingOptions walkingOptions = new WalkingOptions();
        // 设置起点位置坐标
        walkingOptions.setFrom(new SearchLatLng(40.034852, 116.319820));
        // 设置终点位置坐标
        walkingOptions.setTo(new SearchLatLng(40.034852, 117.319820));
        /**
         * 更多默认参数，请参考接口文档{@link WalkingOptions}
         */
        dataManager.getWalking(walkingOptions);
    }

    /**
     * 绘制驾车路线示例。
     */
    public ArrayList<PolylineOptions> handleRouteData(DrivingRouteRsp RouteRsp
            , int selectedIndex) {
        Context context = mView.getAppContext();
        ArrayList<DrivingRouteData> routes = RouteRsp.getRoutes();
        ArrayList<PolylineOptions> options = new ArrayList<>();

        if (routes == null || options == null)
            return null;

        int width = (int) (10 * context.getResources().getDisplayMetrics().density + 0.5);
        for (int i = 0; i < routes.size(); i++) {
            DrivingRouteData routeData = routes.get(i);
            ArrayList<DrivingTrafficItem> traffics = routeData.getTrafficItems();
            List<LatLng> mRoutePoints = SearchConvertHelper.convertLatLng(routeData.getPoints());
            // 点的个数
            int pointSize = mRoutePoints.size();
            // 路段总数，三个index是一个路况单元：路况级别，起点，终点
            int trafficSize = traffics.size();
            // 路段index所对应的颜色值数组
            int[] trafficColors = new int[pointSize];
            // 路段index数组
            int[] trafficColorsIndex = new int[pointSize];
            int pointStart;
            int pointEnd;
            int trafficColor;

            int index = 0;
            for (int j = 0; j < trafficSize; j++) {
                pointStart = traffics.get(j).getFrom();
                pointEnd = traffics.get(j).getTo();

                if (i == selectedIndex) {
                    trafficColor = RouteUtils.getTrafficColorByCode
                            (traffics.get(j).getColor());
                } else {
                    trafficColor = RouteUtils.getTrafficColorNoSelector
                            (traffics.get(j).getColor());
                }
                for (int k = pointStart; k < pointEnd || k == pointSize - 1; k++) {
                    trafficColors[index] = trafficColor;
                    trafficColorsIndex[index] = index;
                    index++;
                }
            }

            PolylineOptions option = new PolylineOptions()
                    .addAll(mRoutePoints)
                    .width(width)
                    .arrow(true);
            if (i == selectedIndex) {
                option.colors(trafficColors, trafficColorsIndex);
                option.zIndex(10);
            } else {
                option.colors(trafficColors, trafficColorsIndex);
                option.zIndex(8);
            }

            options.add(option);

        }
        return options;
    }

    /**
     * 绘制步行路线示例。
     */
    public ArrayList<PolylineOptions> handleRouteData(WalkingRouteRsp routeRsp
            , int selectedIndex) {
        Context context = mView.getAppContext();
        // 路线宽度
        int width = (int) (10 * context.getResources()
                .getDisplayMetrics().density + 0.5);

        ArrayList<WalkingRouteData> routes = routeRsp.getRoutes();
        ArrayList<PolylineOptions> options = new ArrayList<>();

        if (routes == null || options == null)
            return null;

        for (int i = 0; i < routes.size(); i++) {
            WalkingRouteData route = routes.get(i);
            PolylineOptions option = new PolylineOptions()
                    .addAll(SearchConvertHelper.convertLatLng(route.getPoints()))
                    .width(width)
                    .arrow(true);
            if (i == selectedIndex) {
                option.color(0xff3EBA79);
                option.zIndex(10);
            } else {
                option.color(0xff6cbe89);
                option.zIndex(8);
            }

            options.add(option);
        }
        return options;
    }


}
