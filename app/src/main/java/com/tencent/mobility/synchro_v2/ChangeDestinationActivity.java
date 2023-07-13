package com.tencent.mobility.synchro_v2;

import android.os.Bundle;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lsdriver.lsd.listener.SimpleDriDataListener;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.map.lspassenger.lsp.listener.SimplePsgDataListener;
import com.tencent.map.lssupport.bean.TLSDFetchedData;
import com.tencent.map.lssupport.bean.TLSLatlng;
import com.tencent.map.lssupport.utils.ConvertUtil;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.mobility.mock.MockSyncService;
import com.tencent.mobility.ui.OneDriverOnePassengerActivity;
import com.tencent.mobility.ui.PanelView;
import com.tencent.navix.api.layer.NavigatorLayerRootDrive;
import com.tencent.navix.api.navigator.NavigatorDrive;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

public class ChangeDestinationActivity extends OneDriverOnePassengerActivity {

    private static final String ACTION_ROUTES_INIT = "初始化路线";
    private static final String ACTION_CHANGE_DEST = "修改目的地";

    private TLSLatlng driverNewDest;

    @Override
    protected String[] getPassengerActionIndexes() {
        return new String[]{"1", "5"};
    }

    @Override
    protected String[] getPassengerActions() {
        return new String[]{
                ACTION_ORDER_CREATE,
                ACTION_CHANGE_DEST
        };
    }

    @Override
    protected String[] getDriverActionIndexes() {
        return new String[]{"2", "3.1", "3.2", "4", "5"};
    }

    @Override
    protected String[] getDriverActions() {
        return new String[]{
                ACTION_ORDER_BIND,
                ACTION_ORDER_TO_PICKUP,
                ACTION_ORDER_TO_TRIP,
                ACTION_ROUTES_INIT,
                ACTION_CHANGE_DEST
        };
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    protected void onCreatePassengerAction(final MockPassenger passenger,
                                           final TSLPassengerManager passengerSync,
                                           final PanelView passengerPanel,
                                           final NavigatorLayerRootDrive mapView) {

        passengerSync.addTLSPassengerListener(new SimplePsgDataListener() {

            @Override
            public void onPullLsInfoSuc(final TLSDFetchedData fetchedData) {
                super.onPullLsInfoSuc(fetchedData);
                passengerSync.getRouteManager().useRouteIndex(0);
                if (fetchedData.getRoutes().size() <= 0) {
                    return;
                }
                passengerPanel.postAction(ACTION_ROUTES_DRAW);
            }

            @Override
            public void onNewDestinationNotify(final TLSLatlng newDest, final long changedTime) {
                super.onNewDestinationNotify(newDest, changedTime);
                passengerPanel.print("司机的新目的地[" + newDest + "]:" + changedTime);
                passengerSync.getRouteManager().editCurrent().setDestPosition(newDest);
                passengerPanel.postAction(ACTION_ROUTES_DRAW);
            }

            @Override
            public void onDestinationChangeResult(final int status, final String message) {
                super.onDestinationChangeResult(status, message);
                passengerPanel.print("更改目的地[" + status + "]:" + message);
            }
        });

        passengerPanel.addAction(ACTION_CHANGE_DEST, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                LatLng latLng = MockSyncService.getRandomVisibleLatLng(mapView.getMapApi().getProjection());
                passengerSync.changeDestination(ConvertUtil.toTLSLatLng(latLng));
                return true;
            }
        });

        passengerPanel.postAction(ACTION_SYNC_OPEN);
    }

    @Override
    protected void onCreateDriverAction(final MockDriver driver,
                                        final TSLDExtendManager driverSync,
                                        final PanelView driverPanel,
                                        final NavigatorLayerRootDrive carNaviView,
                                        final NavigatorDrive manager) {

        driverSync.addTLSDriverListener(new SimpleDriDataListener() {

            @Override
            public void onPullLsInfoSuc(final String result) {
                super.onPullLsInfoSuc(result);
                if (driverSync.getRouteManager().getRoutes().size() <= 0) {
                    return;
                }
                driverPanel.postAction(ACTION_ROUTES_DRAW);
            }

            @Override
            public void onNewDestinationNotify(final TLSLatlng newDest, final long changedTime) {
                super.onNewDestinationNotify(newDest, changedTime);
                driverPanel.print("乘客的新目的地[" + newDest + "]:" + changedTime);

                //标记更新
                driverSync.getRouteManager().editCurrent()
                        .setDestPosition(newDest)
                        .setDestPositionChanged(true);
                //触发偏航
                driverPanel.postAction(ACTION_ROUTES_RECTIFY_DEVIATION);
            }

            @Override
            public void onDestinationChangeResult(final int status, final String message) {
                super.onDestinationChangeResult(status, message);
                driverPanel.print("更改目的地[" + status + "]:" + message);
                if (status == 0) {
                    //触发偏航
                    //标记更新
                    driverSync.getRouteManager().editCurrent()
                            .setDestPosition(driverNewDest)
                            .setDestPositionChanged(true);
                    driverPanel.postAction(ACTION_ROUTES_RECTIFY_DEVIATION);
                }
            }
        });

        driverPanel.addAction(ACTION_CHANGE_DEST, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                LatLng latLng = MockSyncService.getRandomVisibleLatLng(carNaviView.getMapApi().getProjection());
                driverNewDest = ConvertUtil.toTLSLatLng(latLng);
                driverSync.changeDestination(driverNewDest);
                return true;
            }
        });

        driverPanel.addAction(ACTION_ROUTES_INIT, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                driverPanel.postAction(ACTION_ROUTES_PLAN);
                driverPanel.postAction(ACTION_ROUTES_DRAW);
                driverPanel.postAction(ACTION_ROUTES_UPLOAD);
                driverPanel.postAction(ACTION_NAVI_SIMULATOR_OPEN);
                return true;
            }
        });

        driverPanel.postAction(ACTION_SYNC_OPEN);
    }
}
