package com.tencent.mobility.util;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.map.lspassenger.anima.MarkerTranslateAnimator;
import com.tencent.map.lssupport.bean.TLSBDriverPosition;
import com.tencent.map.lssupport.bean.TLSBOrder;
import com.tencent.map.lssupport.bean.TLSBRoute;
import com.tencent.map.lssupport.bean.TLSBRouteTrafficItem;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.mobility.R;
import com.tencent.mobility.synchro_v2.view.BubbleView;
import com.tencent.navix.api.map.MapApi;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.maps.model.Polyline;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AnimatorUtils {

    private static final String TAG = "AnimatorUtils";
    private static final int ERASE_MSG = 0;

    private static TLSBDriverPosition lastPoint;
    private static final List<LatLng> points = new ArrayList<>();
    private static Map<String, EraseInfo> eraseInfoMap = new HashMap<>();
    private static String curRouteId;
    private static Polyline polyline;
    private static Marker carMarker;
    private static Marker bubbleMarker;

    private static int animationTime = 5000;   // 动画时间
    private static final HandlerThread eraseThread = new HandlerThread("car_erase_line");
    private static EraseHandler eraseHandler;
    private static MarkerTranslateAnimator mTranslateAnimator;
    private static BubbleView bubbleView;
    private static Context mContext;
    private static DecimalFormat df = new DecimalFormat("0.0");

    public static void init(Context context) {
        mContext = context;
        if (!eraseThread.isAlive()) {
            eraseThread.start();
        }
        eraseHandler = new EraseHandler(eraseThread.getLooper());
        eraseInfoMap.clear();
        bubbleView = new BubbleView(mContext);
    }

    public static void updateDriverInfo(MapApi tencentMap, TLSBRoute route, TLSBOrder order, List<TLSBDriverPosition> pos) {
        if (route == null || order == null || pos == null) {
            Log.e(TAG, "pull detailed info null.");
            return;
        }
        if (route.getPoints() == null || route.getPoints().size() <= 0) {
            return;
        }

        //绘制路线
        if (points.size() != 0) {
            points.clear();
        }
        points.addAll(ConvertUtil.toLatLngList(route.getPoints()));   //路线上的点

        List<TLSBRouteTrafficItem> trafficItems = route.getTrafficItemsWithInternalRoute();
        int[] colors = new int[trafficItems.size()];
        int[] indexes = new int[trafficItems.size()];
        setRouteTraffic(trafficItems, indexes, colors);
        // 绘制路线
        if (polyline == null) {
            polyline = tencentMap.addPolyline(new PolylineOptions()
                    .collisionBy(PolylineOptions.PolylineCollision.NONE)
                    .latLngs(points)
                    .colors(colors, indexes)
                    .arrow(true)
                    .eraseColor(0x00000000));
            //可以擦除已走路线
            polyline.setEraseable(true);
            MapUtils.fitsWithRoute(tencentMap, points, DensityUtil.dip2px(mContext, 20),
                    DensityUtil.dip2px(mContext, 80), DensityUtil.dip2px(mContext, 20),
                    DensityUtil.dip2px(mContext, 80));
        }
        // 初始时curRouteId为null，不需要setPoints()，避免polyline闪烁
        if (!route.getRouteId().equals(curRouteId) && curRouteId != null) {
            polyline.setPoints(points);
        }
        curRouteId = route.getRouteId();
        polyline.setColors(colors, indexes);

        if (!eraseInfoMap.containsKey(curRouteId)) {
            eraseInfoMap.put(curRouteId, new EraseInfo(ConvertUtil.toLatLng(route.getPoints().get(0)), 0));
        }

        carMarkerMove(tencentMap, pos, route, eraseInfoMap, polyline);
    }

    public static void carMarkerMove(MapApi tencentMap, List<TLSBDriverPosition> pos, TLSBRoute route, Map<String, EraseInfo> infoMap, Polyline line) {
        eraseInfoMap = infoMap;
        polyline = line;
        //小车平滑移动
        if (pos.size() != 0) {
            LinkedList<TLSBDriverPosition> allPositions = new LinkedList<>(pos);
            // 添加上一次点串的最后一个点，避免小车跳变
            if (lastPoint != null) {
                allPositions.addFirst(lastPoint);
            }

            //添加司机车辆小车
            addDriverCar(tencentMap, allPositions);
            boolean rotateEnabled = false;
            float currentMatchedCourse = allPositions.get(0).getMatchedCourse();
            for (TLSBDriverPosition position : allPositions) {
                if (Math.abs(currentMatchedCourse - position.getMatchedCourse()) > 1.0f) {
                    rotateEnabled = true;
                }
            }
            if (currentMatchedCourse < 1e-7) {
                rotateEnabled = true;
            } else {
                carMarker.setRotation(currentMatchedCourse);
            }

            LinkedList<LatLng> latLngs = new LinkedList<>(Arrays.asList(getLatLngsBySynchroLocation(allPositions)));
            LatLng[] ls = latLngs.toArray(new LatLng[0]);
            // 动画时间根据点数动态调整
            animationTime = (ls.length - 1) * 1000;
            translateAnima(ls, rotateEnabled, route, infoMap); // 平滑移动
            lastPoint = allPositions.get(allPositions.size() - 1);
            bubbleView.refreshData(df.format(route.getRemainingDistance() / 1000.0f), route.getRemainingTime());
            bubbleMarker.setIcon(BitmapDescriptorFactory.fromView(bubbleView));
            bubbleMarker.setPosition(offsetPixelFromLatlng(new LatLng(lastPoint.getLatitude(), lastPoint.getLongitude()), Offset.TOP, DensityUtil.dip2px(mContext, 30), tencentMap));
        }
    }

    public static void updateWaitingInfo(int interval) {
        if (bubbleView == null) {
            return;
        }
        mMainHandler.removeMessages(100);
        if (interval == 0) {
            // 未知灯态
            bubbleView.refreshWaitingDescTv(false);
        } else {
            // 红灯
            bubbleView.refreshWaitingDescTv(true);
            Message message = Message.obtain();
            message.what = 100;
            mMainHandler.sendMessageDelayed(message, interval * 1000L);
        }
    }

    private static Handler mMainHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100) {
                bubbleView.refreshWaitingDescTv(false);
            }
        }
    };

    public static void setRouteTraffic(List<TLSBRouteTrafficItem> trafficItems, int[] indexes, int[] colors) {
        for (int i = 0; i < trafficItems.size(); i++) {
            TLSBRouteTrafficItem item = trafficItems.get(i);
            int colorInt = item.getColor();
            int from = item.getFrom();
            indexes[i] = from;
            int color = 0xFFFFFFFF;
            switch (colorInt) {
                case 0:
                    // 路况标签-畅通(绿色)
                    color = 0xFF00CC66;
                    break;
                case 1:
                    // 路况标签-缓慢(黄色)
                    color = 0xFFF5CC00;
                    break;
                case 2:
                    // 路况标签-拥堵(红色)
                    color = 0xFFF24854;
                    break;
                case 3:
                    // 路况标签-无路况
                    color = 0xFF4E8CFF;
                    break;
                case 4:
                    // 路况标签-特别拥堵（猪肝红）
                    color = 0xFF992529;
                    break;
            }
            colors[i] = color;
        }
    }

    public static void clearUi() {
        if (mTranslateAnimator != null) {
            mTranslateAnimator.cancelAnimation();
            mTranslateAnimator = null;
        }
        if (polyline != null) {
            polyline.remove();
            polyline = null;
        }

        if (carMarker != null) {
            carMarker.remove();
            carMarker = null;
        }
        if (bubbleMarker != null) {
            bubbleMarker.remove();
            bubbleMarker = null;
        }
        points.clear();
        lastPoint = null;
        curRouteId = null;
    }

    /**
     * 平滑移动
     */
    private static void translateAnima(LatLng[] points, boolean rotateEnabled, TLSBRoute route, Map<String, EraseInfo> infoMap) {
        if (points == null || points.length <= 0) {
            return;
        }
        // 当司机没有新数据上传，防止拉取回上个点串的最后一个点
        if (points.length == 2 && equalOfLatlng(points[0], points[1])) {
            return;
        }

        if (mTranslateAnimator != null) {
            mTranslateAnimator.cancelAnimation();
        }
        mTranslateAnimator = new MarkerTranslateAnimator(
                //执行此平移动画的 marker
                carMarker,
                //动画持续时间
                animationTime,
                //平移动画点串
                points,
                //marker 是否会根据传入的点串计算并执行旋转动画, marker 方向将与移动方向保持一致
                rotateEnabled);
        mTranslateAnimator.startAnimation();
        mTranslateAnimator.setFloatValuesListener(new MarkerTranslateAnimator.IAnimaFloatValuesListener() {
            @Override
            public void floatValues(LatLng latLng) {
                if (!reCalcCurrentPointIndex(infoMap.get(route.getRouteId()), latLng, route)) {
                    return;
                }
                eraseRoute(route.getRouteId());
            }
        });
    }

    private static void eraseRoute(String routeId) {
        Message message = Message.obtain();
        message.obj = routeId;
        message.what = ERASE_MSG;
        eraseHandler.sendMessage(message);
    }


    // 平滑动画只需要使用一个marker即可
    private static void addDriverCar(MapApi tencentMap, List<TLSBDriverPosition> points) {
        if (carMarker == null) {
            carMarker = tencentMap.addMarker(
                    new MarkerOptions(new LatLng(points.get(0).getLatitude(), points.get(0).getLongitude()))
                            .anchor(0.5f, 0.5f)
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_icon_driver))
                            //设置此属性 marker 会跟随地图旋转
                            .flat(true)
                            //相同显示level，zIndex越大越靠上显示 level优先级大于zIndex
                            .level(100)
                            .zIndex(100.0f)
                            //marker 逆时针方向旋转
                            .clockwise(false));

        }
        if (bubbleMarker == null) {
            LatLng initLocation = new LatLng(points.get(0).getLatitude(), points.get(0).getLongitude());
            bubbleMarker = tencentMap.addMarker(
                    new MarkerOptions(offsetPixelFromLatlng(initLocation, Offset.TOP, DensityUtil.dip2px(mContext, 30), tencentMap))
                            .anchor(0.5f, 1f)
                            .icon(BitmapDescriptorFactory.fromView(bubbleView))
                            .flat(false)
                            .level(100)
                            .zIndex(100.0f)
            );
        }
    }

    private static LatLng offsetPixelFromLatlng(LatLng original, Offset direction, int pixel, MapApi mapApi) {
        Point point = mapApi.getProjection().toScreenLocation(original);
        switch (direction) {
            case LEFT:
                point.offset(-pixel, 0);
                break;
            case RIGHT:
                point.offset(pixel, 0);
                break;
            case TOP:
                point.offset(0, -pixel);
                break;
            case BOTTOM:
                point.offset(0, pixel);
                break;
            default:
                break;
        }
        return mapApi.getProjection().fromScreenLocation(point);
    }

    enum Offset {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    public static class EraseInfo {

        private LatLng latLng;
        private int curEraseIndex;

        public EraseInfo(LatLng latLng, int curEraseIndex) {
            this.latLng = latLng;
            this.curEraseIndex = curEraseIndex;
        }

        public LatLng getLatLng() {
            return latLng;
        }

        public void setLatLng(LatLng latLng) {
            this.latLng = latLng;
        }

        public int getCurEraseIndex() {
            return curEraseIndex;
        }

        public void setCurEraseIndex(int curEraseIndex) {
            this.curEraseIndex = curEraseIndex;
        }
    }

    private static class EraseHandler extends Handler {

        public EraseHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            try {
                if (msg.what == ERASE_MSG) {
                    String routeId = (String) (msg.obj);
                    if (routeId != null && polyline != null && eraseInfoMap.containsKey(routeId)) {
                        polyline.eraseTo(eraseInfoMap.get(routeId).getCurEraseIndex(), eraseInfoMap.get(routeId).getLatLng());
                    }
                    eraseHandler.removeMessages(ERASE_MSG);
                }
            } catch (Exception e) {
                Log.d(TAG, "erase handler handle message error:" + e.getMessage());
            }
        }
    }

    /**
     * 计算当前小车所在路线的擦除索引
     *
     * @param eraseInfo 擦除信息
     * @param latLng 当前位置
     * @param route 路线
     * @return 是否执行擦除
     */
    private static boolean reCalcCurrentPointIndex(EraseInfo eraseInfo, LatLng latLng, TLSBRoute route) {
        if (latLng == null || route == null || route.getPoints() == null) {
            return false;
        }
        LatLng curLatLng = eraseInfo.getLatLng();
        if (Math.abs(curLatLng.getLatitude() - latLng.getLatitude()) + Math.abs(curLatLng.getLongitude() - latLng.getLongitude()) < 1.5E-6) {
            return false;
        }
        eraseInfo.setLatLng(latLng);
        // 防止数组越界
        int pointIndex =  Math.max(0, Math.min(route.getPoints().size() - 2, eraseInfo.getCurEraseIndex()));
        int to = route.getPoints().size() - 1;
        if (route.getWayPoints() != null && route.getWayPoints().size() > 0) {
            to = route.getWayPoints().get(0).getPointIndex();
        }
        int[] result = calcPointIndexWithCoord(latLng, route, pointIndex, to, 7, true);
        if (result[0] == 1) {
            // 找到匹配的pointIndex
            eraseInfo.setCurEraseIndex(result[1]);
            return true;
        }

        // 没找到匹配的点，再从头找
        result = calcPointIndexWithCoord(latLng, route, 0, pointIndex, 5, false);
        if (result[0] == 1) {
            // 找到匹配的pointIndex
            eraseInfo.setCurEraseIndex(result[1]);
            return true;
        }
        return true;
    }

    private static int[] calcPointIndexWithCoord(LatLng latLng, TLSBRoute route, int from, int to, double threshold, boolean asc) {
        int[] result = new int[2];
        boolean found = false;
        int pointIndex = from;
        double tmpDistance = 0;
        if (asc) {
            for (int i = from; i < to; i++) {
                // 线段的起点和终点
                TLSLatlng startLatLng = route.getPoints().get(i);
                TLSLatlng toLatLng = route.getPoints().get(i + 1);
                // 计算当前小车到线段的距离
                double distance = distanceBetweenPoint(latLng, ConvertUtil.toLatLng(startLatLng), ConvertUtil.toLatLng(toLatLng));
                if (distance < threshold) {
                    // 找到了当前路线
                    if (found) {
                        if (distance < tmpDistance) {
                            pointIndex = i;
                            tmpDistance = distance;
                        } else {
                            break;
                        }

                    } else {
                        found = true;
                        pointIndex = i;
                        tmpDistance = distance;
                        if (distance < 1) {
                            break;
                        }
                    }
                } else if (found) {
                    break;
                }
            }
        } else {
            for (int i = to - 1; i >= from; i--) {
                // 线段的起点和终点
                TLSLatlng startLatLng = route.getPoints().get(i);
                TLSLatlng toLatLng = route.getPoints().get(i + 1);
                // 计算当前小车到线段的距离
                double distance = distanceBetweenPoint(latLng, ConvertUtil.toLatLng(startLatLng), ConvertUtil.toLatLng(toLatLng));
                if (distance < threshold) {
                    // 找到了当前路线
                    if (found) {
                        if (distance < tmpDistance) {
                            pointIndex = i;
                            tmpDistance = distance;
                        } else {
                            break;
                        }

                    } else {
                        found = true;
                        pointIndex = i;
                        tmpDistance = distance;
                        if (distance < 1) {
                            break;
                        }
                    }
                } else if (found) {
                    break;
                }
            }
        }
        result[0] = found ? 1 : 0;
        result[1] = pointIndex;
        return result;
    }

    /**
     * 计算点到线段的距离
     *
     * @param point 点
     * @param pointA 线段起点
     * @param pointB 线段终点
     * @return 距离
     */
    private static double distanceBetweenPoint(LatLng point, LatLng pointA, LatLng pointB) {
        LatLng nearestPoint;
        if (pointA.getLatitude() == pointB.getLatitude() && pointA.getLongitude() == pointB.getLongitude()) {
            nearestPoint = pointA;
        } else {
            double s0lat = deg2rad(point.latitude);
            double s0lng = deg2rad(point.longitude);
            double s1lat = deg2rad(pointA.latitude);
            double s1lng = deg2rad(pointA.longitude);
            double s2lat = deg2rad(pointB.latitude);
            double s2lng = deg2rad(pointB.longitude);

            double s2s1lat = s2lat - s1lat;
            double s2s1lng = s2lng - s1lng;
            double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                    / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
            if (u <= 0) {
                nearestPoint = pointA;
            } else if (u >= 1) {
                nearestPoint = pointB;
            } else {
                nearestPoint = new LatLng(pointA.latitude + (u * (pointB.latitude - pointA.latitude)),
                        pointA.longitude + (u * (pointB.longitude - pointA.longitude)));
            }
        }

        return distanceBetween(point.latitude, point.longitude, nearestPoint.latitude, nearestPoint.longitude);
    }

    /**
     * 自定义计算距离公式
     *
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    private static double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // km
        double dLat = deg2rad(lat2 - lat1);
        double dLon = deg2rad(lon2 - lon1);
        double gclat1 = deg2rad(lat1);
        double gclat2 = deg2rad(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(gclat1)
                * Math.cos(gclat2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;

        return d * 1000;
    }

    // 将角度转换为弧度
    private static double deg2rad(double degree) {
        return degree * Math.PI / 180;
    }

    /**
     * 获取小车平滑需要的点串信息
     *
     * @param locations
     */
    private static LatLng[] getLatLngsBySynchroLocation(List<TLSBDriverPosition> locations) {
        if (locations == null) {
            return null;
        }

        int size = locations.size();
        LatLng[] latLngs = new LatLng[size];
        for (int i = 0; i < size; i++) {
            TLSBDriverPosition driverPosition = locations.get(i);
            if (driverPosition.getAttachLat() == 0 && driverPosition.getAttachLng() == 0) {
                latLngs[i] = new LatLng(driverPosition.getLatitude(), driverPosition.getLongitude());
            } else {
                latLngs[i] = new LatLng(driverPosition.getAttachLat(), driverPosition.getAttachLng());
            }
        }
        return latLngs;
    }

    /**
     * 对比经纬度相等
     */
    private static boolean equalOfLatlng(LatLng latLng0, LatLng latLng1) {
        if (latLng0 == null || latLng1 == null) {
            return false;
        }

        return latLng0.getLongitude() == latLng1.getLongitude()
                && latLng0.getLatitude() == latLng1.getLatitude();
    }
}
