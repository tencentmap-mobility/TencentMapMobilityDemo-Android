package com.tencent.mobility.synchro_v2.driver;

import com.tencent.map.lssupport.bean.TLSBPosition;
import com.tencent.map.lssupport.bean.TLSDWayPointInfo;
import com.tencent.map.navi.data.NaviPoi;
import com.tencent.map.navi.data.RouteData;

import java.util.ArrayList;
import java.util.List;

public interface IHandleUiListener {

    void drawUi(RouteData curRoute, NaviPoi from, NaviPoi to, List<TLSDWayPointInfo> ws);

    void showPsgPosition(List<TLSBPosition> los);

    void clearMapUi();
}
