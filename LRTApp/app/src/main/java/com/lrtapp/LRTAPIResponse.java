package com.lrtapp;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LRTAPIResponse {
    @SerializedName("platform_list")
    public List<Platform> platformList;
    
    public static class Platform {
        @SerializedName("platform_id")
        public int platformId;
        
        @SerializedName("route_list")
        public List<RouteData> routeList;
    }
    
    public static class RouteData {
        @SerializedName("route_no")
        public String routeNo;
        
        @SerializedName("dest_en")
        public String destEn;
        
        @SerializedName("dest_ch")
        public String destCh;
        
        @SerializedName("time_en")
        public String timeEn;
        
        @SerializedName("time_ch")
        public String timeCh;
        
        @SerializedName("train_length")
        public int trainLength;
        
        @SerializedName("stop")
        public int stop;
    }
}
