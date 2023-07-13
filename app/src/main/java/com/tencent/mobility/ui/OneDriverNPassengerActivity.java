package com.tencent.mobility.ui;

import android.os.Bundle;
import android.util.SparseArray;

import com.tencent.map.lsdriver.TSLDExtendManager;
import com.tencent.map.lspassenger.TSLPassengerManager;
import com.tencent.mobility.R;
import com.tencent.mobility.mock.MockDriver;
import com.tencent.mobility.mock.MockPassenger;
import com.tencent.navix.api.layer.NavigatorLayerRootDrive;
import com.tencent.navix.api.navigator.NavigatorDrive;
import com.tencent.tencentmap.mapsdk.maps.MapView;

import java.util.ArrayList;
import java.util.List;

public abstract class OneDriverNPassengerActivity extends OneDriverOnePassengerActivity {

    public static final String ACTION_DRIVER_ORDER_CREATE_CARPOOLING = "创建拼车单";
    public static final String ACTION_PASSENGER_ACCOUNT_CHANGE = "切换账户";
    private final SparseArray<PassengerInfo> mPassengers = new SparseArray<>();
    private int mCurrentPassengerNo;

    protected int getLayoutResId() {
        return R.layout.one_driver_n_passenger_layout;
    }

    @Override
    protected final String getPassengerName() {
        return super.getPassengerName() + "[" + (getPassengerNumber() + 1) + "]";
    }

    @Override
    protected final String[] getPassengerActions() {
        String[] actions = getPassengerActions(getPassengerNumber());

        if (actions != null) {
            String[] newActions = new String[actions.length + 1];
            newActions[0] = ACTION_PASSENGER_ACCOUNT_CHANGE;
            System.arraycopy(actions, 0, newActions, 1, actions.length);
            return newActions;
        }

        return new String[]{ACTION_PASSENGER_ACCOUNT_CHANGE};
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPassengers.put(mCurrentPassengerNo, mPassengerInfo);
    }

    @Override
    protected final String[] getPassengerActionIndexes() {
        return getPassengerActionIndexes(getPassengerNumber());
    }

    public List<MockPassenger> getPassengers() {
        List<MockPassenger> passengers = new ArrayList<>();
        for (int i = 0; i < mPassengers.size(); i++) {
            PassengerInfo passengerInfo = mPassengers.get(i);
            passengers.add(passengerInfo.mPassenger);
        }
        return passengers;
    }

    public PassengerInfo getPassengerInfo(MockPassenger passenger) {
        for (int i = 0; i < mPassengers.size(); i++) {
            PassengerInfo passengerInfo = mPassengers.get(i);
            if (passengerInfo.mPassenger == passenger) {
                return passengerInfo;
            }
        }
        return null;
    }

    protected int getPassengerNumber() {
        return mCurrentPassengerNo;
    }

    protected abstract int getPassengerSize();

    protected abstract String[] getPassengerActions(int passengerNo);

    protected abstract String[] getPassengerActionIndexes(int passengerNo);

    @Override
    protected final void onCreatePassengerAction(MockPassenger passenger, TSLPassengerManager passengerSync,
                                                 PanelView passengerPanel, NavigatorLayerRootDrive mapView) {
        passengerPanel.addAction(ACTION_PASSENGER_ACCOUNT_CHANGE, new PanelView.Action<Boolean>(false) {
            @Override
            public Boolean run() {
                int size = getPassengerSize();
                int number = getPassengerNumber();
                number++;
                if (number >= size) {
                    number = 0;
                }
                mCurrentPassengerNo = number;
                PassengerInfo passengerInfo = mPassengers.get(mCurrentPassengerNo);
                if (passengerInfo == null) {
                    passengerInfo = createPassenger();
                    mPassengers.put(mCurrentPassengerNo, passengerInfo);
                }
                changePassenger(passengerInfo);
                return true;
            }
        });

        onCreatePassengerAction(getPassengerNumber(), passenger, passengerSync, passengerPanel, mapView);
    }

    protected abstract void onCreatePassengerAction(int number, MockPassenger passenger,
                                                    TSLPassengerManager passengerSync,
                                                    PanelView passengerPanel, NavigatorLayerRootDrive mapView);

    @Override
    protected void onCreateDriverAction(MockDriver driver, TSLDExtendManager driverSync,
                                        PanelView driverPanel, NavigatorLayerRootDrive carNaviView,
                                        NavigatorDrive manager) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < mPassengers.size(); i++) {
            PassengerInfo passengerInfo = mPassengers.get(i);
            passengerInfo.mPassengerSync.destroy();
        }
    }
}
