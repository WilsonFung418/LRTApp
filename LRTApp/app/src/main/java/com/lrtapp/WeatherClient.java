package com.lrtapp;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WeatherClient {

    private static final String HKO_API_URL = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=tc";
    private final OkHttpClient client;
    private final Gson gson;

    public WeatherClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    public WeatherData getCurrentWeather() throws IOException {
        Request request = new Request.Builder()
                .url(HKO_API_URL)
                .header("User-Agent", "LRTApp/1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HKO API error: " + response.code());
            String body = response.body().string();
            return gson.fromJson(body, WeatherData.class);
        }
    }

    // Station ID -> HKO location name
    public static String getDistrict(String stationId) {
        switch (stationId) {
            case "220": case "100": case "295": case "280":
            case "270": case "070": case "080":
                return "屯門";
            case "560": case "570": case "580": case "590": case "600":
                return "元朗";
            case "430": case "460": case "480": case "468":
                return "屯門";
            default:
                return "屯門";
        }
    }

    public static class WeatherData {
        public String[] icon;
        public String[] warningMessage;
        public TemperatureData[] temperature;
    }

    public static class TemperatureData {
        public String place;
        public String value;
        public String unit;
    }
}