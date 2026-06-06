package com.lrtapp;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

public class LRTWidgetUpdateJobService extends JobService {
    private static final String TAG = "LRTWidgetUpdateJob";
    private static final String PREFS_NAME = "LRTWidgetPrefs";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "📱 Widget update job started (unlock trigger)");
        new WidgetUpdateTask().execute(params);
        return true; // Keep the job alive
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Widget update job stopped");
        return false; // Don't reschedule
    }

    private class WidgetUpdateTask extends AsyncTask<JobParameters, Void, JobParameters> {
        @Override
        protected JobParameters doInBackground(JobParameters... params) {
            JobParameters jobParams = params[0];
            updateWidgets();
            return jobParams;
        }

        @Override
        protected void onPostExecute(JobParameters jobParams) {
            jobFinished(jobParams, false);
        }

        private void updateWidgets() {
            try {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(LRTWidgetUpdateJobService.this);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                        new ComponentName(LRTWidgetUpdateJobService.this, LRTWidgetProvider.class));

                if (appWidgetIds == null || appWidgetIds.length == 0) {
                    Log.d(TAG, "No widgets to update");
                    return;
                }

                // Get saved station ID
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String stationId = prefs.getString("widget_station_id_" + appWidgetIds[0], "220");
                String stationName = prefs.getString("widget_station_name_" + appWidgetIds[0], "大興(南)");

                // Fetch LRT data
                LRTAPIService apiService = new LRTAPIService(LRTWidgetUpdateJobService.this);
                List<Arrival> arrivals = apiService.getArrivals(stationId);

                // Update each widget
                for (int widgetId : appWidgetIds) {
                    String[][] cols = buildColumns(arrivals);
                    LRTWidgetProvider.pushData(LRTWidgetUpdateJobService.this, widgetId, stationName, cols, "", "⏱", new String[]{}, "");
                }

                Log.d(TAG, "✅ Widgets refreshed after unlock");
            } catch (Exception e) {
                Log.e(TAG, "❌ Widget update failed: " + e.getMessage());
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
