package com.lrtapp;

import java.util.List;

public class Station {
    public String id;
    public String nameTc;
    public String nameEn;
    public double latitude;
    public double longitude;
    public int displayOrder;

    public Station(String id, String nameTc, String nameEn, double latitude, double longitude, int displayOrder) {
        this.id = id;
        this.nameTc = nameTc;
        this.nameEn = nameEn;
        this.latitude = latitude;
        this.longitude = longitude;
        this.displayOrder = displayOrder;
    }

    public String getDisplayName() {
        return nameTc;
    }

    public static List<Station> getAllStations() {
        return List.of(
            new Station("220", "大興(南)", "Tai Hing (South)", 22.4028, 113.9720, 1),
            new Station("100", "兆康", "Siu Hong", 22.4073, 113.9768, 2),
            new Station("295", "屯門", "Tuen Mun", 22.3947, 113.9766, 3),
            new Station("280", "市中心", "Town Centre", 22.3915, 113.9766, 4),
            new Station("270", "安定", "On Ting", 22.3892, 113.9728, 5),
            new Station("560", "水邊圍", "Shui Pin Wai", 22.3856, 113.9726, 6),
            new Station("570", "豐年路", "Fung Nin Road", 22.3832, 113.9692, 7),
            new Station("580", "康樂路", "Hong Lok Road", 22.3811, 113.9659, 8),
            new Station("590", "大棠路", "Tai Tong Road", 22.3787, 113.9623, 9),
            new Station("600", "元朗", "Yuen Long", 22.3757, 113.9588, 10),
            new Station("430", "天水圍", "Tin Shui Wai", 22.4608, 113.9612, 11),
            new Station("460", "天瑞", "Tin Shui", 22.4665, 113.9654, 12),
            new Station("480", "天富", "Tin Fu", 22.4713, 113.9686, 13),
            new Station("468", "頌富", "Chung Fu", 22.4657, 113.9705, 14),
            new Station("070", "河田", "Ho Tin", 22.4079, 113.9786, 15),
            new Station("080", "澤豐", "Zefeng", 22.4034, 113.9749, 16)
        );
    }

    public static Station findNearest(double lat, double lng) {
        Station nearest = getAllStations().get(0);
        double minDist = Double.MAX_VALUE;
        for (Station s : getAllStations()) {
            double d = Math.sqrt(Math.pow(s.latitude - lat, 2) + Math.pow(s.longitude - lng, 2));
            if (d < minDist) {
                minDist = d;
                nearest = s;
            }
        }
        return nearest;
    }
}