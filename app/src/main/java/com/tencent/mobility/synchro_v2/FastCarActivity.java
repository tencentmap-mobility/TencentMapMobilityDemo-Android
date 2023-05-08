package com.tencent.mobility.synchro_v2;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.navi.car.CarNaviView;
import com.tencent.map.navi.car.TencentCarNaviManager;
import com.tencent.mobility.mock.MockCar;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.ui.OneDriverOnePassengerActivity;
import com.tencent.mobility.ui.PanelView;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

public class FastCarActivity extends OneDriverOnePassengerActivity {

    @Override
    protected String[] getPassengerActions() {
        return new String[]{
                ACTION_ORDER_CREATE,
                ACTION_SYNC_OPEN,
                ACTION_PULL,
                ACTION_ROUTES_DRAW,
                ACTION_SYNC_CLOSE,
        };
    }

    @Override
    protected String[] getDriverActions() {
        return new String[]{
                ACTION_SYNC_OPEN,
                ACTION_DISPATCH_ORDER,
                ACTION_ORDER_TO_PICKUP,
                ACTION_ORDER_TO_TRIP,
                ACTION_ROUTES_PLAN,
                ACTION_ROUTES_DRAW,
                ACTION_ROUTES_UPLOAD,
                ACTION_PULL,
                ACTION_NAVI_SIMULATOR_OPEN,
                ACTION_NAVI_SIMULATOR_CLOSE,
                ACTION_NAVI_OPEN,
                ACTION_NAVI_CLOSE,
                ACTION_SYNC_CLOSE,
        };
    }

    @Override
    protected void onCreatePassengerAction(MockPassenger passenger, TSLPassengerManager passengerSync,
            PanelView passengerPanel, MapView mapView) {

        //设置乘客固定起终点
//        passenger.setStart(new LatLng(40.042879, 116.270723));
//        passenger.setEnd(new LatLng(40.098958, 116.27824));

    }

    @Override
    protected void onCreateDriverAction(MockDriver driver, TSLDExtendManager driverSync,
            PanelView driverPanel, CarNaviView carNaviView,
            TencentCarNaviManager manager) {
        //设置司机的类型
        driver.setCarType(MockCar.CarType.All);
        driver.setBizType(MockCar.BizType.RealTime);

        //设置司机固定起终点
//        driver.setStart(new LatLng(40.002229, 116.323806));
//        driver.setEnd(new LatLng(40.103269, 116.269314));
    }
}
