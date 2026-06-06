package com.lrtapp;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.List;

public class LRTWidgetUpdateService extends IntentService {
    private static final String TAG = "LRTWidgetUpdate";
    private static final String PREFS_NAME = "LRTWidgetPrefs";

    public LRTWidgetUpdateService() {
        super("LRTWidgetUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Widget update triggered by unlock");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(this, LRTWidgetProvider.class));

        if (appWidgetIds == null || appWidgetIds.length == 0) {
            Log.d(TAG, "No widgets to update");
            return;
        }

        // Get saved station ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String stationId = prefs.getString("widget_station_id_" + appWidgetIds[0], "220");
        String stationName = prefs.getString("widget_station_name_" + appWidgetIds[0], "大興(南)");

        // Fetch arrivals and update all widgets
        try {
            LRTAPIService apiService = new LRTAPIService(this);
            List<Arrival> arrivals = apiService.getArrivals(stationId);

            for (int widgetId : appWidgetIds) {
                String[][] cols = buildColumns(arrivals);
                LRTWidgetProvider.pushData(this, widgetId, stationName, cols, "", "⏱", new String[]{}, "");
            }
            Log.d(TAG, "Widgets updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to update widgets: " + e.getMessage());
        }
    }

    private String[][] buildColumns(List<Arrival> arrivals) {
        String[][] cols = new String[4][5];
        int row = 0;
        for (Arrival a : arrivals) {
            if (row >= 4) break;
            if (a.isPlatformHeader) {
                cols[row][0] = a.routeNo;           // Platform
                cols[row][1] = "";                   // Route
                cols[row][2] = "";                   // Destination
                cols[row][3] = "";                   // Time
                cols[row][4] = "";                   // Car count
                row++;
            } else {
                cols[row][0] = a.routeNo;
                cols[row][1] = a.destinationTc;
                cols[row][2] = a.arrivalTc;
                cols[row][3] = String.valueOf(a.trainLength);
                cols[row][4] = "";
                row++;
            }
        }
        return cols;
    }
}
