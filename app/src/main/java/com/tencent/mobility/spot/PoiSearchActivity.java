package com.tencent.mobility.spot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.tencent.map.navi.agent.TencentSearchManager;
import com.tencent.map.navi.agent.data.POIInfo;
import com.tencent.map.navi.agent.data.SearchLatLng;
import com.tencent.map.navi.agent.sug.SugOptions;
import com.tencent.map.navi.agent.sug.beans.SugRsp;
import com.tencent.map.navi.agent.sug.interfaces.SugListener;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import com.tencent.mobility.R;

/**
 * 搜索Poi，封装了UI
 */
public class PoiSearchActivity extends Activity {

    public static final int REQUEST_CODE = 90;
    /**
     * 默认为起点
     */
    private PoiType poiType = PoiType.START;

    /**
     * 搜索View
     */
    private EditText editText;

    private ListView listView;

    private ImageView backView;

    private LatLng latLng;

    private String region = "";

    private SearchAdapter mSearchAdapter;

    private int requestCode;

    /**
     * @param activity
     * @param latLng   起点的坐标
     * @param region   起点的城市名
     */
    public static void searchPoiStart(int requestCode, Activity activity, LatLng latLng, String region) {
        searchPoi(requestCode, activity, PoiType.START, latLng, region);
    }

    /**
     * @param activity
     */
    public static void searchPoiDes(int requestCode, Activity activity, String region) {
        searchPoi(requestCode, activity, PoiType.DES, null, region);
    }

    /**
     * @param activity
     * @param poiType
     * @param latLng
     * @param region
     */
    private static void searchPoi(int requestCode, Activity activity, PoiType poiType, LatLng latLng, String region) {
        Intent intent = new Intent(activity, PoiSearchActivity.class);
        intent.putExtra("type", poiType);
        intent.putExtra("latlng", latLng);
        intent.putExtra("region", region);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poi_search_layout);
        init();
        initUI();
    }

    private void init() {
        poiType = (PoiType) getIntent().getSerializableExtra("type");
        latLng = getIntent().getParcelableExtra("latlng");
        region = getIntent().getStringExtra("region");
    }

    private void initUI() {
        editText = findViewById(R.id.search);
        listView = findViewById(R.id.list);
        mSearchAdapter = new SearchAdapter(getApplication());
        listView.setAdapter(mSearchAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                POIInfo poiBean = mSearchAdapter.getItem(position);
                Intent intent = new Intent();
                intent.putExtra("result_poi", getSerializablePoi(poiBean));
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        backView = findViewById(R.id.back);

        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                search(s.toString());
            }
        });
    }

    private POIBean getSerializablePoi(POIInfo info) {
        POIBean bean = new POIBean();
        bean.id = info.id;
        bean.lat = info.location.getLat();
        bean.lng = info.getLocation().getLng();
        return bean;
    }

    private void search(String keywords) {
        TencentSearchManager dataManager = new TencentSearchManager(getApplicationContext());
        dataManager.setSugListener(new SugListener() {
            @Override
            public void onSuccess(SugRsp sugRsp) {
                Log.d("mainactivity", "sug请求成功");
                onSug(sugRsp);
            }

            @Override
            public void onError(int i, String s) {
                Log.d("mainactivity", "message");
            }
        });

        SugOptions sugOptions = new SugOptions();
        SearchLatLng locationBean = new SearchLatLng();
        if (latLng != null) {
            locationBean.setLat(40.034852);
            locationBean.setLng(116.319820);
        }
        sugOptions.setPolicy(poiType == PoiType.START ? "1" : "2")
                .setRegion(region)
                .setKeyword(keywords)
                .setLocation(locationBean);
        dataManager.getSug(sugOptions);
    }

    private void onSug(SugRsp sugBean) {
        mSearchAdapter.refreshData(sugBean);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public enum PoiType {
        START, DES
    }

}
