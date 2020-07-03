package com.tencent.mobility.spot;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.tencent.mobility.R;

public class ChangeCityPopWindow extends PopupWindow {

    private View rootView;

    private Context context;

    private Handler handlerUi = new Handler(Looper.getMainLooper());

    private InputMethodManager imm;

    private popWindowListener listener;

    EditText editText;

    interface popWindowListener {
        void onSave();
        void editStr(String etTxt);
    }

    public ChangeCityPopWindow(Context context, popWindowListener listener) {
        this.context = context;
        this.listener = listener;
        init();
    }

    private void init() {
        rootView = LayoutInflater.from(context)
                .inflate(R.layout.change_city_pop_window, null);
        setContentView(rootView);
        setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);

        setOutsideTouchable(false);
        setFocusable(true);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // 弹出、退出动画效果
        setAnimationStyle(R.style.hub_popup_window_anim);

        rootView.findViewById(R.id.tv_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowing())
                    dismiss();
                if (listener != null)
                    listener.onSave();
            }
        });

        editText = rootView.findViewById(R.id.et_city);

        editText.setFocusable(true);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (listener != null)
                    listener.editStr(s.toString());
            }
        });
    }

    public void show() {
        if (isShowing())
            return;

        showAtLocation(rootView, Gravity.BOTTOM, 0, 0);

        // 弹出键盘
        handlerUi.post(new Runnable() {
            @Override
            public void run() {
                imm = (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

    }
}
