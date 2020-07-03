package com.tencent.mobility.synchro_v2.driver;

import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.map.navi.data.NaviPoi;
import com.tencent.map.navi.data.RouteData;

import java.util.ArrayList;

public interface IHandleUiListener {

    void drawUi(RouteData curRoute, NaviPoi from, NaviPoi to, ArrayList<TLSDWayPointInfo> ws);

    void showPsgPosition(ArrayList<TLSBPosition> los);

    void clearMapUi();
}
