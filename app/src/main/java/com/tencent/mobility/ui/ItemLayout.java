package com.tencent.mobility.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.mobility.R;
import com.tencent.mobility.util.CommonUtils;

public class ItemLayout extends RelativeLayout {

    private View mItemLayout;

    private TextView tvTitle, tvContent;

    public ItemLayout(Context context) {
        this(context, null);
    }

    public ItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        createItem(context, attrs, defStyleAttr);
    }

    private void createItem(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs
                , R.styleable.ItemLayout, defStyleAttr, 0);
        String title = array.getString(R.styleable.ItemLayout_itemTitle);
        String content = array.getString(R.styleable.ItemLayout_itemContent);
        array.recycle();

        mItemLayout = LayoutInflater.from(context).inflate(R.layout.main_task_item, null);
        tvTitle = mItemLayout.findViewById(R.id.tv_title);
        tvContent = mItemLayout.findViewById(R.id.tv_content);
        tvTitle.setText(title);
        tvContent.setText(content);

        int height = (int) CommonUtils.dp2px(context, 42);
        RelativeLayout.LayoutParams params = new RelativeLayout
                .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        mItemLayout.setPadding((int) CommonUtils.dp2px(context, 12), 0
                , (int) CommonUtils.dp2px(context, 12), 0);
        params.topMargin = (int) CommonUtils.dp2px(context, 4);
        params.bottomMargin = (int) CommonUtils.dp2px(context, 4);
        addView(mItemLayout, params);
    }

    public void setTitle(String title) {
        if (tvTitle != null)
            tvTitle.setText(title);
    }

    public void setConent(String content) {
        if (tvContent != null)
            tvContent.setText(content);
    }

}
