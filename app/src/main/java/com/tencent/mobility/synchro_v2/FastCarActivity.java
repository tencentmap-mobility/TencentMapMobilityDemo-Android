package com.tencent.mobility.synchro_v2;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.ui.OneDriverOnePassengerActivity;
import com.tencent.mobility.ui.PanelView;
import com.tencent.navix.api.layer.NavigatorLayerRootDrive;
import com.tencent.navix.api.navigator.NavigatorDrive;

public class FastCarActivity extends OneDriverOnePassengerActivity {

    @Override
    protected String[] getPassengerActions() {
        return new String[] {
                ACTION_ORDER_SYNC_ORDER_CREATE,
                ACTION_SYNC_OPEN,
                ACTION_PULL,
                ACTION_SYNC_CLOSE
        };
    }

    @Override
    protected String[] getDriverActions() {
        return new String[] {
                ACTION_ORDER_SYNC_SET_OFF,
                ACTION_SYNC_OPEN,
                ACTION_PULL,
                ACTION_ROUTES_PLAN,
                ACTION_ROUTES_UPLOAD,
                ACTION_NAVI_SIMULATOR_OPEN,
                ACTION_NAVI_SIMULATOR_CLOSE,
                ACTION_ROUTES_DRAW,
                ACTION_ORDER_SYNC_ON_TRIP,
                ACTION_ORDER_SYNC_FINISH,
                ACTION_ORDER_SYNC_CANCEL
        };
    }

    @Override
    protected void onCreatePassengerAction(MockPassenger passenger, TSLPassengerManager passengerSync, PanelView passengerPanel, NavigatorLayerRootDrive mapView) {

    }

    @Override
    protected void onCreateDriverAction(MockDriver driver, TSLDExtendManager driverSync, PanelView driverPanel, NavigatorLayerRootDrive carNaviView, NavigatorDrive manager) {

    }
}
