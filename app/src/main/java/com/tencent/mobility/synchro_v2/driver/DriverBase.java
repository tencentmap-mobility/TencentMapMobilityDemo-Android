package com.tencent.mobility.synchro_v2.driver;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.lssupport.bean.TLSBWayPointType;
import com.tencent.map.lssupport.bean.TLSConfigPreference;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.map.navi.data.NaviPoi;
import com.tencent.map.navi.data.RouteData;
import com.tencent.map.navi.data.TrafficItem;
import com.tencent.map.navi.tlocation.ITNKLocationCallBack;
import com.tencent.map.navi.tlocation.TNKLocationManager;
import com.tencent.mobility.R;
import com.tencent.mobility.synchro_v2.helper.SHelper;
import com.tencent.mobility.synchro_v2.helper.SingleHelper;
import com.tencent.mobility.util.CommonUtils;
import com.tencent.mobility.util.ToastUtils;
import com.tencent.navi.surport.utils.DeviceUtils;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.Polyline;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public abstract class DriverBase extends DriverBaseMapActivity implements IHandleUiListener {

    static final String LOG_TAG = "navi1234";

    TSLDExtendManager lsManager;// 司乘管理类
    TNKLocationManager loManager;// 导航内部定位
    TencentCarNaviManager naviManager;

    ArrayList<Marker> markers = new ArrayList<>();// 起点终点
    ArrayList<Marker> wayMarkers = new ArrayList<>();
    int[] icons = new int[] {R.mipmap.waypoint1_1// 途经点图标
            , R.mipmap.waypoint1_2
            , R.mipmap.waypoint2_1
            , R.mipmap.waypoint2_2};
    Polyline polyline;
    Marker psgMarker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化toast
        ToastUtils.init(getApplicationContext());

        // 初始化导航
        naviManager = SingleHelper.getNaviManager(getApplicationContext());

        // 初始化司乘
        lsManager = TSLDExtendManager.getInstance();
        lsManager.setNaviManager(naviManager);

        // 初始化定位
        loManager = TNKLocationManager.getInstance();
        loManager.setContext(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastUtils.INSTANCE().destory();
    }

    /**
     * 初始化配置
     * @param driverId 司机id
     */
    public void initConfig(String driverId) {
        lsManager.init(getApplicationContext(), TLSConfigPreference.create()
                .setDeviceId(DeviceUtils.getImei(getApplicationContext()))// 设备id
                .setAccountId(driverId));// 司机id
    }

    /**
     * 开启定位
     */
    public void startLoc(ITNKLocationCallBack locListener) {
        ToastUtils.INSTANCE().Toast("开始定位");
        if(loManager != null)// 注册监听后，自动启动定位
            loManager.addLocationListener(locListener);
    }

    /**
     * 停止定位
     */
    public void stopLoc(ITNKLocationCallBack locListener) {
        ToastUtils.INSTANCE().Toast("停止定位!!");
        if(loManager != null)
            loManager.removeLicationListener(locListener);
    }

    /**
     * 开启司乘
     */
    public void startSync() {
        ToastUtils.INSTANCE().Toast("开启司乘");
        if(lsManager != null) {
            lsManager.start();
        }
    }

    /**
     * 结束司乘
     */
    public void stopSync() {
        ToastUtils.INSTANCE().Toast("停止司乘");
        if(lsManager != null) {
            lsManager.stop();
        }
        clearMapUi();
    }

    /**
     * 拉取乘客定位点
     */
    public void startPullPsgPos() {
        ToastUtils.INSTANCE().Toast("拉取乘客位置点");
        if(lsManager != null)
            lsManager.fetchPassengerPositionsEnabled(true);
    }

    /**
     * 停止拉取乘客位置
     */
    public void stopPullPsgPos() {
        ToastUtils.INSTANCE().Toast("停止拉取!!");
        if(lsManager != null)
            lsManager.fetchPassengerPositionsEnabled(false);
    }

    @Override
    public void clearMapUi() {
        if(polyline != null)
            polyline.remove();
        polyline = null;
        if(markers.size() != 0) {
            for(Marker m : markers)
                m.remove();
            markers.clear();
        }
        if(wayMarkers.size() != 0) {
            for(Marker m : wayMarkers)
                m.remove();
            wayMarkers.clear();
        }
    }

    @Override
    public void drawUi(RouteData curRoute, NaviPoi from, NaviPoi to, ArrayList<TLSDWayPointInfo> ws) {

        if(polyline != null)
            polyline.remove();
        int width = (int) (10 * getResources().getDisplayMetrics().density + 0.5);

        ArrayList<TrafficItem> traffics = getTrafficItemsFromList(curRoute.getTrafficIndexList());
        List<LatLng> mRoutePoints = curRoute.getRoutePoints();
        // 点的个数
        int pointSize = mRoutePoints.size();
        // 路段总数 三个index是一个路况单元，分别为：路况级别，起点，终点
        int trafficSize = traffics.size();
        // 路段index所对应的颜色值数组
        int[] trafficColors = new int[pointSize];
        // 路段index数组
        int[] trafficColorsIndex = new int[pointSize];
        int pointStart = 0;
        int pointEnd = 0;
        int trafficColor = 0;
        int index = 0;
        for (int j = 0; j < trafficSize; j++) {
            pointStart = traffics.get(j).getFromIndex();
            // 规避错乱数据
            if (pointStart < pointEnd) {
                pointStart = pointEnd;
            }
            if (pointStart == pointSize - 1) {
                break;
            }
            pointEnd = traffics.get(j).getToIndex();
            trafficColor = getTrafficColorByCode(traffics.get(j).getTraffic());
            for (int k = pointStart; k < pointEnd || k == pointSize - 1; k++) {
                trafficColors[index] = trafficColor;
                trafficColorsIndex[index] = index;
                index++;
            }
        }

        // 调整视图，使中心点为起点终点的中点
        SHelper.fitsWithRoute(tencentMap, curRoute.getRoutePoints()
                , (int) CommonUtils.dp2px(this, 32)
                , (int) CommonUtils.dp2px(this, 64)
                , (int) CommonUtils.dp2px(this, 32)
                , (int) CommonUtils.dp2px(this, 64));

        PolylineOptions options = new PolylineOptions()
                .addAll(curRoute.getRoutePoints())
                .width(width)
                .arrow(true);
        options.colors(trafficColors, trafficColorsIndex);
        options.zIndex(10);
        polyline = tencentMap.addPolyline(options);

        addMarker(from, to);
        addWayMarker(ws);
    }

    /**
     * 显示乘客位置
     * @param los
     */
    @Override
    public void showPsgPosition(ArrayList<TLSBPosition> los) {
        if(los == null || los.size() == 0)// 1:可能是快车乘客没有上传点，2:顺风车
            return;
        int size = los.size();
        if(psgMarker == null)
            psgMarker = addMarker(new LatLng(los.get(size - 1).getLatitude()
                            , los.get(size - 1).getLongitude())
                    , R.mipmap.psg_position_icon, 0);
        else
            psgMarker.setPosition(new LatLng(los.get(size - 1).getLatitude()
                    , los.get(size - 1).getLongitude()));
    }

    /**
     * 添加起点终点、途经点marker
     */
    private void addMarker(NaviPoi from, NaviPoi to) {
        if(markers.size() != 0) {
            for(Marker m : markers)
                m.remove();
            markers.clear();
        }

        markers.add(addMarker(new LatLng(from.getLatitude(), from.getLongitude())
                , R.mipmap.line_start_point, 0));
        if (to != null)
            markers.add(addMarker(new LatLng(to.getLatitude(), to.getLongitude())
                    , R.mipmap.line_end_point, 0));

    }

    private void addWayMarker(ArrayList<TLSDWayPointInfo> ways) {
        removeWaysMarker();

        ArrayList<TLSDWayPointInfo> ws = new ArrayList<>();
        ws.addAll(ways);
        int curIndex = 0;
        while (ws.size() != 0) {
            TLSDWayPointInfo wayPoint = ws.get(0);
            if(ws.size() == 1) {// 只有一个途经点
                addWaysMarker(wayPoint, curIndex);
                ws.remove(0);
                break;
            }
            for(int index = 1; index < ws.size(); index ++) {
                if(ws.get(index).getpOrderId().equals(wayPoint.getpOrderId())) {
                    addWaysMarker(ws.get(index), curIndex);
                    ws.remove(index);
                    break;
                }
            }
            addWaysMarker(wayPoint, curIndex);
            ws.remove(0);
            curIndex += 2;
        }
    }

    /**
     * 添加途经点
     * @param way
     * @param curIndex
     */
    private void addWaysMarker(TLSDWayPointInfo way, int curIndex) {
        if(way == null && curIndex >= icons.length)
            return;
        if(way.getWayPointType() == TLSBWayPointType.TLSDWayPointTypeGetIn) {// 上车点
            wayMarkers.add(addMarker(new LatLng(way.getLat(), way.getLng()), icons[curIndex]
                    , 0, 0.5f, 1f));
        }else if(way.getWayPointType() == TLSBWayPointType.TLSDWayPointTypeGetOff) {// 下车点
            wayMarkers.add(addMarker(new LatLng(way.getLat(), way.getLng()), icons[curIndex + 1]
                    , 0, 0.5f, 1f));
        }
    }

    private void removeWaysMarker() {
        if(wayMarkers.size() != 0) {
            for(Marker m : wayMarkers) {
                m.remove();
            }
            wayMarkers.clear();
        }
    }

    private int getTrafficColorByCode(int type) {
        int color = 0xFFFFFFFF;
        switch (type) {
            case 0:
                // 路况标签-畅通
                // 绿色
                color = 0xff3EBA79;
                break;
            case 1:
                // 路况标签-缓慢
                // 黄色
                color = 0xffF4BB45;
                break;
            case 2:
                // 路况标签-拥堵
                // 红色
                color = 0xffE85854;
                break;
            case 3:
                // 路况标签-无路况
                color = 0xff4F96EE;
                break;
            case 4:
                // 路况标签-特别拥堵（猪肝红）
                color = 0xffAF333D;
                break;
        }
        return color;
    }

    private ArrayList<TrafficItem> getTrafficItemsFromList(ArrayList<Integer> indexList) {
        ArrayList<TrafficItem> trafficItems = new ArrayList<>();
        for (int i = 0; i < indexList.size(); i = i + 3) {
            TrafficItem item = new TrafficItem();
            item.setTraffic(indexList.get(i));
            item.setFromIndex(indexList.get(i + 1));
            item.setToIndex(indexList.get(i + 2));
            trafficItems.add(item);
        }
        return trafficItems;
    }
}
