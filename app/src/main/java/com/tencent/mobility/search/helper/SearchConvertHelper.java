package com.tencent.mobility.search.helper;

import com.tencent.map.navi.agent.data.SearchLatLng;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.ArrayList;

public class SearchConvertHelper {

    public static ArrayList<LatLng> convertLatLng(ArrayList<SearchLatLng> latLngs) {
        if (latLngs == null)
            return null;
        ArrayList<LatLng> tls = new ArrayList<>();
        for (SearchLatLng latlng : latLngs) {
            tls.add(new LatLng(latlng.getLat(), latlng.getLng()));
        }
        return tls;
    }

}
