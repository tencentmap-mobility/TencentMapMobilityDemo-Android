package com.tencent.mobility.util;

public class RouteUtils {

    public static int getTrafficColorNoSelector(int type) {
        int color = 0xFFFFFFFF;

        switch (type) {
            case 0:
                // 路况标签-畅通
                // 绿色
                color = 0xff6cbe89;
                break;

            case 1:
                // 路况标签-缓慢
                // 黄色
                color = 0xffedc263;
                break;
            case 2:
                // 路况标签-拥堵
                // 红色
                color = 0xffd96b63;
                break;

            case 3:
                // 路况标签-无路况
                color = 0xff69a1ea;
                break;

            case 4:
                // 路况标签-特别拥堵（猪肝红）
                color = 0xffab4448;
                break;

        }
        return color;
    }

    public static int getTrafficColorByCode(int type) {
        int color = 0xFFFFFFFF;

        switch (type) {
            case 0:
                // 路况标签-畅通
                // 绿色
                color = 0xff3EBA79;
                break;

            case 1:
                // 路况标签-缓慢
                // 黄色
                color = 0xffF4BB45;
                break;

            case 2:
                // 路况标签-拥堵
                // 红色
                color = 0xffE85854;
                break;

            case 3:
                // 路况标签-无路况
                color = 0xff4F96EE;
                break;

            case 4:
                // 路况标签-特别拥堵（猪肝红）
                color = 0xffAF333D;
                break;

        }
        return color;
    }
}
