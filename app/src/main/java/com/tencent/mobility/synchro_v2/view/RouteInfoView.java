package com.tencent.mobility.synchro_v2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.tencent.mobility.R;


public class RouteInfoView extends ConstraintLayout {

    private TextView etaView;
    private TextView edaView;
    private TextView trafficCountView;
    private TextView isMainView;

    public RouteInfoView(@NonNull Context context) {
        super(context);
    }

    public RouteInfoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RouteInfoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RouteInfoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    private void initView() {
        isMainView = findViewById(R.id.is_main_view);
        etaView = findViewById(R.id.eta_tv);
        edaView = findViewById(R.id.eda_tv);
        trafficCountView = findViewById(R.id.traffic_count_tv);
    }

    public void reset() {
        isMainView.setText("是否主路线");
        etaView.setText("剩余时间");
        edaView.setText("剩余距离");
        trafficCountView.setText("剩余红绿灯");
    }

    public void setData(int eta, int eda, int trafficCount) {
        etaView.setText(eta + "分");
        edaView.setText(eda + "米");
        trafficCountView.setText(trafficCount + "个");
    }

    public void isMain(boolean main) {
        isMainView.setText(main ? "主" : "备");
    }
}
