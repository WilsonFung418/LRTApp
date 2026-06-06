package com.lrtapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import android.appwidget.AppWidgetManager;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver screenOnReceiver;
    private Spinner stationSelector;
    private RecyclerView arrivalsRecyclerView;
    private ArrivalsAdapter adapter;
    private TextView lastUpdateText;
    private TextView stationNameText;
    private TextView weatherText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View loadingView;
    private View emptyView;
    private View refreshIndicator;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LRTAPIService apiService;
    private Station currentStation;
    private List<Station> stations;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private boolean isRefreshing = false;
    private LocationManager locationManager;
    private boolean locationUpdatesStarted = false;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fineLocation) || Boolean.TRUE.equals(coarseLocation)) {
                    detectLocation();
                    // Try to request background location on API 30+
                    if (android.os.Build.VERSION.SDK_INT >= 30) {
                        requestBackgroundLocation();
                    }
                } else {
                    setDefaultStation();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Dynamically register SCREEN_ON receiver (manifest can't do this reliably)
        screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    // Screen just turned on - UnlockReceiver will handle USER_PRESENT
                    // This just wakes things up - actual refresh happens on unlock
                    android.util.Log.d("LRTApp", "💡 Screen turned on - ready for unlock");
                }
            }
        };
        IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOnReceiver, screenFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(screenOnReceiver, screenFilter);
        }
        try {
            setContentView(R.layout.activity_main);

            apiService = new LRTAPIService(this);
            currentStation = Station.getAllStations().get(0);
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            initViews();
            setupStationSelector();
            setupRecyclerView();
            checkLocationPermission();
            startAutoRefresh();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LRTApp", "Crash in onCreate", e);
            Toast.makeText(this, "Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        stationSelector = findViewById(R.id.stationSpinner);
        arrivalsRecyclerView = findViewById(R.id.arrivalsRecyclerView);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        stationNameText = findViewById(R.id.stationNameText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        loadingView = findViewById(R.id.loadingView);
        emptyView = findViewById(R.id.emptyView);
        weatherText = findViewById(R.id.weatherText);
        refreshIndicator = findViewById(R.id.refreshIndicator);

        swipeRefreshLayout.setOnRefreshListener(this::fetchArrivals);
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.primary),
                ContextCompat.getColor(this, R.color.primary_dark)
        );
    }

    private void setupStationSelector() {
        stations = Station.getAllStations();
        String[] stationNames = new String[stations.size()];
        for (int i = 0; i < stations.size(); i++) {
            stationNames[i] = stations.get(i).getDisplayName();
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, stationNames);
        stationSelector.setAdapter(spinnerAdapter);

        stationSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStation = stations.get(position);
                stationNameText.setText(currentStation.getDisplayName());
                fetchArrivals();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ArrivalsAdapter();
        arrivalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        arrivalsRecyclerView.setAdapter(adapter);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        } else {
            // On API 30+, request background separately
            String[] perms;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                perms = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            } else {
                perms = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
            }
            locationPermissionLauncher.launch(perms);
        }
    }

    private void detectLocation() {
        if (locationUpdatesStarted) return;

        String provider = LocationManager.GPS_PROVIDER;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                setDefaultStation();
                return;
            }
            provider = LocationManager.NETWORK_PROVIDER;
        }

        try {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                Station nearest = Station.findNearest(location.getLatitude(), location.getLongitude());
                selectStation(nearest);
            } else {
                setDefaultStation();
            }
            // Start periodic location updates
            startLocationUpdates();
        } catch (Exception e) {
            setDefaultStation();
        }
    }

    private void selectStation(Station station) {
        currentStation = station;
        runOnUiThread(() -> {
                        for (int i = 0; i < stations.size(); i++) {
                if (stations.get(i).id.equals(station.id)) {
                    stationSelector.setSelection(i);
                    break;
                }
            }
            stationNameText.setText(station.getDisplayName());
            fetchArrivals();
        });
    }

    private void setDefaultStation() {
        selectStation(Station.getAllStations().get(0));
    }

    private void fetchArrivals() {
        if (isRefreshing) return;
        isRefreshing = true;

        runOnUiThread(() -> {
            refreshIndicator.setVisibility(View.VISIBLE);
            if (adapter.getItemCount() == 0) {
                loadingView.setVisibility(View.VISIBLE);
            }
        });

        new Thread(() -> {
            try {
                if (currentStation == null || apiService == null) {
                    runOnUiThread(() -> {
                        loadingView.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        isRefreshing = false;
                        refreshIndicator.setVisibility(View.GONE);
                    });
                    return;
                }
                List<Arrival> arrivals = apiService.getArrivals(currentStation.id);
                // Also fetch weather
                try {
                    okhttp3.Request req = new okhttp3.Request.Builder()
                        .url("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=tc")
                        .build();
                    try (okhttp3.Response resp = httpClient.newCall(req).execute()) {
                        String body = resp.body().string();
                        com.google.gson.Gson g = new com.google.gson.Gson();
                        LRTWidgetProvider.HkoWeather hko = g.fromJson(body, LRTWidgetProvider.HkoWeather.class);
                        if (hko != null && hko.temperature != null && hko.temperature.data != null) {
                            for (LRTWidgetProvider.HkoWeather.TempItem t : hko.temperature.data) {
                                if ("屯門".equals(t.place)) {
                                    cachedWeather = t.value + "°C";
                                    cachedWeatherIcon = t.value >= 33 ? "☀️" : t.value >= 28 ? "⛅" : "🌧️";
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {}

                // Fetch HKO 9-day forecast for widget
                try {
                    okhttp3.Request frex = new okhttp3.Request.Builder()
                        .url("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=fnd&lang=tc")
                        .build();
                    try (okhttp3.Response fresp = httpClient.newCall(frex).execute()) {
                        String fbody = fresp.body().string();
                        org.json.JSONObject fjson = new org.json.JSONObject(fbody);
                        org.json.JSONArray fcast = fjson.optJSONArray("weatherForecast");
                        if (fcast != null) {
                            for (int fi = 0; fi < 3 && fi < fcast.length(); fi++) {
                                org.json.JSONObject day = fcast.optJSONObject(fi);
                                if (day != null) {
                                    String week = day.optString("week", "");
                                    int icon = day.optInt("ForecastIcon", 64);
                                    String emoji = com.lrtapp.WeatherMapping.picCodeToEmoji(icon);
                                    int maxT = day.optJSONObject("forecastMaxtemp") != null ?
                                        day.optJSONObject("forecastMaxtemp").optInt("value", 0) : 0;
                                    int minT = day.optJSONObject("forecastMintemp") != null ?
                                        day.optJSONObject("forecastMintemp").optInt("value", 0) : 0;
                                    String shortDay = weekToShort(week);
                                    cachedForecast[fi] = shortDay + " " + emoji + " " + minT + "-" + maxT + "°C";
                                }
                            }
                        }
                    }
                } catch (Exception e) {}

                runOnUiThread(() -> {
                    loadingView.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    isRefreshing = false;

                    pushWidgetUpdate(arrivals);
                    if (arrivals != null && !arrivals.isEmpty()) {
                        adapter.setArrivals(arrivals);
                        arrivalsRecyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else {
                        adapter.setArrivals(null);
                        arrivalsRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }

                    if (weatherText != null) {
                        weatherText.setText(cachedWeatherIcon + " " + cachedWeather);
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String time = sdf.format(new Date());
                    lastUpdateText.setText("最後更新: " + time);
                    refreshIndicator.setVisibility(View.GONE);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    loadingView.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    isRefreshing = false;
                    Toast.makeText(this, "無法載入資料: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    refreshIndicator.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private String cachedWeather = "--°C";
    private String cachedWeatherIcon = "☀️";
    private String[] cachedForecast = {"--:--", "--:--", "--:--"};

    private void fetchWeather() {
        new Thread(() -> {
            try {
                okhttp3.Request req = new okhttp3.Request.Builder()
                    .url("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=tc")
                    .build();
                try (okhttp3.Response resp = httpClient.newCall(req).execute()) {
                    String body = resp.body().string();
                    com.google.gson.Gson g = new com.google.gson.Gson();
                    LRTWidgetProvider.HkoWeather hko = g.fromJson(body, LRTWidgetProvider.HkoWeather.class);
                    if (hko != null && hko.temperature != null && hko.temperature.data != null) {
                        for (LRTWidgetProvider.HkoWeather.TempItem t : hko.temperature.data) {
                            if ("屯門".equals(t.place)) {
                                cachedWeather = t.value + "°C";
                                cachedWeatherIcon = t.value >= 33 ? "☀️" : t.value >= 28 ? "⛅" : "🌧️";
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {}
        }).start();
    }

    private void pushWidgetUpdate(java.util.List<Arrival> arrivals) {
        if (arrivals == null || currentStation == null) return;
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            int[] ids = mgr.getAppWidgetIds(new android.content.ComponentName(this, LRTWidgetProvider.class));
            if (ids.length == 0) return;
            String title = currentStation.getDisplayName() + " " + "\uD83D\uDEB4";

            // Save station preference for widget self-update
            for (int id : ids) {
                getSharedPreferences("lrt_widget_" + id, Context.MODE_PRIVATE)
                    .edit().putString("station_id", currentStation.id).apply();
            }

            // Build 4 rows x 5 columns [platform, route, dest, time, cars]
            String[][] cols = new String[4][5];
            java.util.Map<Integer, java.util.List<Arrival>> byPlat = new java.util.LinkedHashMap<>();
            int curP = -1;
            for (int i = 0; i < arrivals.size(); i++) {
                Arrival a = arrivals.get(i);
                if (a == null) continue;
                if (a.isPlatformHeader) {
                    curP = a.platformId;
                    byPlat.put(curP, new java.util.ArrayList<>());
                } else if (curP > 0) {
                    byPlat.get(curP).add(a);
                }
            }
            int row = 0;
            for (java.util.Map.Entry<Integer, java.util.List<Arrival>> e : byPlat.entrySet()) {
                int plat = e.getKey();
                java.util.List<Arrival> list = e.getValue();
                int perPlat = byPlat.size() >= 3 ? 1 : 2;
                for (int j = 0; j < Math.min(list.size(), perPlat) && row < 4; j++) {
                    Arrival a = list.get(j);
                    cols[row][0] = "P" + plat;
                    cols[row][1] = a.routeNo;
                    cols[row][2] = a.getDestinationTc();
                    cols[row][3] = a.getArrivalDisplay();
                    cols[row][4] = a.trainLength >= 2 ? "\uD83D\uDEF0\uD83D\uDEF0" : "\uD83D\uDEF0";
                    row++;
                }
            }

            // Weather data
            String weather = cachedWeather;
            String weatherIcon = cachedWeatherIcon;

            for (int id : ids) {
                LRTWidgetProvider.pushData(this, id, title, cols, weather, weatherIcon, cachedForecast, "");
            }
        } catch (Exception e) {}
    }

    private void requestBackgroundLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) return;
        try {
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                runOnUiThread(() -> {
                    try {
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                        builder.setTitle("背景定位權限")
                            .setMessage("允許背景定位可以讓Widget自動偵測最近車站，即使冇開App都會更新。")
                            .setPositiveButton("允許", (d, w) -> {
                                locationPermissionLauncher.launch(new String[]{
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                });
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    } catch (Exception e) {}
                });
            }, 3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {}
    }

    private void startAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!swipeRefreshLayout.isRefreshing()) {
                    fetchArrivals();
                }
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates (keep handler running for pending widget pushes)
        if (locationUpdatesStarted) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception e) {}
            locationUpdatesStarted = false;
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        if (locationUpdatesStarted) return;
        locationUpdatesStarted = true;
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 100, locationListener);
        } catch (Exception e) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 100, locationListener);
            } catch (Exception e2) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!locationUpdatesStarted) startLocationUpdates();
    }

    // New location listener field
    private final android.location.LocationListener locationListener = new android.location.LocationListener() {
        @Override
        public void onLocationChanged(@androidx.annotation.NonNull android.location.Location location) {
            Station nearest = Station.findNearest(location.getLatitude(), location.getLongitude());
            selectStation(nearest);
        }
        @Override public void onStatusChanged(String p, int s, android.os.Bundle b) {}
        @Override public void onProviderEnabled(String p) {}
        @Override public void onProviderDisabled(String p) {}
    };

    private static String weekToShort(String week) {
        if (week == null) return "";
        switch (week) {
            case "星期一": return "週一";
            case "星期二": return "週二";
            case "星期三": return "週三";
            case "星期四": return "週四";
            case "星期五": return "週五";
            case "星期六": return "週六";
            case "星期日": return "週日";
            default: return week.length() >= 2 ? week.substring(0, 2) : week;
        }
    }

    @Override
    protected void onDestroy() {
        if (screenOnReceiver != null) {
            try {
                unregisterReceiver(screenOnReceiver);
            } catch (Exception ignored) {}
        }
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}