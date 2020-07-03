package com.tencent.mobility.spot;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.recommendspot.recospot.bean.TMMTraHubBean
        .TraObjBean.DetailBean.TraHubBean.SubFenceBean;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.tencent.mobility.R;

public class HubPopWindow extends PopupWindow {

    List<SubFenceBean> subFenceBeans = new ArrayList<>();

    private View rootView;

    private Context context;

    private RecyclerView recyclerView;

    private HubRecycler adapter;

    private IClickListener listener;

    private String currFenceName = "";

    private int currHitIndex = -1;

    public HubPopWindow(Context context
            , IClickListener listener) {
        this.context = context;
        this.listener = listener;
        init();
    }

    public void setSubData(String currFenceName, List<SubFenceBean> bs, String subhitId) {
        subFenceBeans.clear();
        subFenceBeans.addAll(bs);

        this.currFenceName = currFenceName;

        int index = 0;
        currHitIndex = -1;
        for (SubFenceBean bean : subFenceBeans) {
            if (bean != null && bean.getId().equals(subhitId)) {
                currHitIndex = index;
                break;
            }
            index++;
        }

        if (isShowing())
            initAdapter();
    }

    private void init() {
        rootView = LayoutInflater.from(context).inflate(R.layout.hub_activity, null);
        recyclerView = rootView.findViewById(R.id.pop_recycler_view);
        setContentView(rootView);
        setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        // 设置PopupWindow区域外触碰交互不受影响
        setOutsideTouchable(false);
        setFocusable(false);
        // 弹出、退出动画效果
        setAnimationStyle(R.style.hub_popup_window_anim);
    }

    private void initAdapter() {
        LinearLayoutManager manager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(new HubRecycler(listener));
        if (adapter == null) {
            adapter = new HubRecycler(listener);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    public void show() {
        if (isShowing() || subFenceBeans.size() == 0)
            return;
        showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
        initAdapter();
    }

    interface IClickListener {
        void onClick(String hubId);
    }

    class HubRecycler extends RecyclerView.Adapter<HubRecycler.ViewHolder>{

        private int currClickIndex = currHitIndex;

        private IClickListener clickListener;

        public HubRecycler(IClickListener listener) {
            this.clickListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.hub_task_recycler_item
                            , parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String content = subFenceBeans.get(position).getName();
            holder.tvContent.setText(currFenceName + "-" + content);

            if (currClickIndex == position)
                holder.tvContent.setTextColor(0xff6cbe89);
            else
                holder.tvContent.setTextColor(0xff333333);

            holder.listener.position = position;
            holder.listener.setViewHolder(holder);
            holder.tvContent.setOnClickListener(holder.listener);
        }

        @Override
        public int getItemCount() {
            return subFenceBeans.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            MyClickListener listener = new MyClickListener();

            TextView tvContent;

            public ViewHolder(View view) {
                super(view);
                tvContent = view.findViewById(R.id.tv_recycler_item_content);
            }
        }

        class MyClickListener implements View.OnClickListener {

            public WeakReference<ViewHolder> wrf;
            public int position;

            public void setViewHolder(ViewHolder viewHolder) {
                wrf = new WeakReference<>(viewHolder);
            }

            @Override
            public void onClick(View v) {
                if (wrf == null || wrf.get() == null) {
                    return;
                }

                if (clickListener != null && position < subFenceBeans.size()) {
                    notifyDataSetChanged();
                    currClickIndex = position;
                    clickListener.onClick(subFenceBeans.get(position).getId());
                }
            }
        }

    }

}
