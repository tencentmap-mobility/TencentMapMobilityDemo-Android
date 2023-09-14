package com.tencent.mobility.synchro_v2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.tencent.mobility.R;

public class BubbleView extends ConstraintLayout {

    private TextView waitingDescTv;
    private TextView edaAndUnitTv;
    private TextView etaAndUnitTv;

    public BubbleView(@NonNull Context context) {
        this(context, null);
    }

    public BubbleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        View rootView = LayoutInflater.from(getContext()).inflate(R.layout.bubble_layout, this);
        waitingDescTv = rootView.findViewById(R.id.waiting_light_tv);
        edaAndUnitTv = rootView.findViewById(R.id.eda_tv);
        etaAndUnitTv = rootView.findViewById(R.id.eta_tv);

        waitingDescTv.setVisibility(GONE);
    }

    public void refreshData(String eda, int eta) {
        edaAndUnitTv.setText(eda + "公里 ");
        etaAndUnitTv.setText(eta + "分钟");
    }

    public void refreshWaitingDescTv(boolean waitingLight) {
        if (waitingLight) {
            waitingDescTv.setVisibility(VISIBLE);
        } else {
            waitingDescTv.setVisibility(GONE);
        }
    }
}
