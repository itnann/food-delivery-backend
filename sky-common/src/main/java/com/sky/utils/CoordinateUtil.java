package com.sky.utils;

/**
 * 经纬度处理工具类 格式为：纬度,经度
 */
public class CoordinateUtil {

    public static String processCoordinate(String lat, String lng) {
        String[] splitLat = lat.split("\\.");
        String[] splitLng = lng.split("\\.");



        lat = splitLat[0]+"."+splitLat[1].substring(0, 6);
        lng = splitLng[0]+"."+splitLng[1].substring(0, 6);
        return lat + "," + lng;
    }
}
