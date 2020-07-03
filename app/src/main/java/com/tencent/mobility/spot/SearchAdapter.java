package com.tencent.mobility.spot;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tencent.map.navi.agent.data.POIInfo;
import com.tencent.map.navi.agent.sug.beans.SugRsp;

import com.tencent.mobility.R;

public class SearchAdapter extends BaseAdapter {


    private Context mContext;

    private SugRsp sugBean;

    public SearchAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        if (sugBean == null || sugBean.getPoiInfoList() == null)
            return 0;
        return sugBean.getPoiInfoList().size();
    }

    @Override
    public POIInfo getItem(int position) {
        return sugBean.getPoiInfoList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        POIInfo poiBean = getItem(position);
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.search_item_layout, null);
            ViewHolder vh = new ViewHolder();
            vh.name = convertView.findViewById(R.id.name);
            vh.des = convertView.findViewById(R.id.des);
            convertView.setTag(vh);
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.name.setText(poiBean.title);
        viewHolder.des.setText(poiBean.address);
        return convertView;
    }

    public void refreshData(SugRsp sugBean) {
        this.sugBean = sugBean;
        notifyDataSetChanged();
    }

    public static class ViewHolder {
        public TextView name;
        public TextView des;
    }
}
