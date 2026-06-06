package com.lrtapp;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

public class UnlockReceiver extends BroadcastReceiver {
    private static final String TAG = "UnlockReceiver";
    private static final String PREFS_NAME = "LRTWidgetPrefs";
    private static final String PREF_LAST_UPDATE = "last_unlock_update_time";
    private static final long DEBOUNCE_MS = 15000; // 15 seconds

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : "";
        Log.d(TAG, "📡 Received broadcast: " + action);

        // Only trigger on full unlock (USER_PRESENT)
        if (!Intent.ACTION_USER_PRESENT.equals(action)) {
            return;
        }

        // Debounce: skip if last update was < 15 seconds ago
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdate = prefs.getLong(PREF_LAST_UPDATE, 0);
        long now = System.currentTimeMillis();

        if (now - lastUpdate < DEBOUNCE_MS) {
            Log.d(TAG, "⏳ Debounce: last update was " + (now - lastUpdate) + "ms ago, skipping");
            return;
        }

        Log.d(TAG, "🔓 Unlock detected - refreshing widget data");
        prefs.edit().putLong(PREF_LAST_UPDATE, now).apply();

        // Trigger async widget refresh
        new WidgetRefreshTask(context).execute();
    }

    private static class WidgetRefreshTask extends AsyncTask<Void, Void, Void> {
        private final Context context;

        WidgetRefreshTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            refreshWidgets();
            return null;
        }

        private void refreshWidgets() {
            try {
                AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                int[] ids = mgr.getAppWidgetIds(
                        new ComponentName(context, LRTWidgetProvider.class));

                if (ids == null || ids.length == 0) {
                    Log.d(TAG, "No widgets installed");
                    return;
                }

                // Get saved station config
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String stationId = prefs.getString("widget_station_id_" + ids[0], "220");
                String stationName = prefs.getString("widget_station_name_" + ids[0], "大興(南)");

                // Fetch LRT arrivals
                LRTAPIService api = new LRTAPIService(context);
                List<Arrival> arrivals = api.getArrivals(stationId);

                // Build data columns
                String[][] cols = buildColumns(arrivals);

                // Update all widgets
                for (int id : ids) {
                    LRTWidgetProvider.pushData(context, id, stationName,
                            cols, "", "⏱", new String[]{}, "");
                }

                Log.d(TAG, "✅ " + ids.length + " widget(s) refreshed after unlock");
            } catch (Exception e) {
                Log.e(TAG, "❌ Widget refresh failed: " + e.getMessage());
            }
        }

        private String[][] buildColumns(List<Arrival> arrivals) {
            String[][] cols = new String[4][5];
            int row = 0;
            for (Arrival a : arrivals) {
                if (row >= 4) break;
                if (a.isPlatformHeader) {
                    cols[row][0] = a.routeNo;
                    cols[row][1] = "";
                    cols[row][2] = "";
                    cols[row][3] = "";
                    cols[row][4] = "";
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
}
