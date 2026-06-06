package com.lrtapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import android.icu.util.ChineseCalendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LRTWidgetProvider extends AppWidgetProvider {

    private static final String[] LUNAR_MONTHS = {"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"};
    private static final String[] LUNAR_DAYS = {"初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"};

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lrt_widget);
            views.setTextViewText(R.id.widget_station, "輕鐵到站");
            views.setTextViewText(R.id.widget_r0, "載入中...");
            updateDate(views);
            setClickIntent(context, views);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static void pushData(Context context, int appWidgetId, String stationName,
                                 String[][] cols, String weather, String weatherIcon,
                                 String[] forecast, String warningText) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lrt_widget);

            // Clear all cells
            int[] allCellIds = {
                R.id.widget_r0, R.id.widget_d0, R.id.widget_t0, R.id.widget_c0,
                R.id.widget_r1, R.id.widget_d1, R.id.widget_t1, R.id.widget_c1,
                R.id.widget_r2, R.id.widget_d2, R.id.widget_t2, R.id.widget_c2,
                R.id.widget_r3, R.id.widget_d3, R.id.widget_t3, R.id.widget_c3
            };
            for (int id : allCellIds) views.setTextViewText(id, "");

            // Station + time
            views.setTextViewText(R.id.widget_station, stationName != null ? stationName : "");
            java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            views.setTextViewText(R.id.widget_updated, "更新 " + tf.format(new java.util.Date()));

            boolean hasData = false;
            if (cols != null && cols.length > 0) {
                int[] rIds = {R.id.widget_r0, R.id.widget_r1, R.id.widget_r2, R.id.widget_r3};
                int[] dIds = {R.id.widget_d0, R.id.widget_d1, R.id.widget_d2, R.id.widget_d3};
                int[] tIds = {R.id.widget_t0, R.id.widget_t1, R.id.widget_t2, R.id.widget_t3};
                int[] cIds = {R.id.widget_c0, R.id.widget_c1, R.id.widget_c2, R.id.widget_c3};

                for (int i = 0; i < 4 && i < cols.length; i++) {
                    if (cols[i] != null && cols[i].length > 0 && cols[i][0] != null) {
                        String routeNo = safeStr(cols[i][1]);
                        int routeColor = com.lrtapp.RouteColors.getColor(routeNo);

                        views.setTextViewText(rIds[i], routeNo);
                        views.setInt(rIds[i], "setBackgroundColor", routeColor);
                        views.setTextColor(rIds[i], "751".equals(routeNo) ? 0xFF000000 : 0xFFFFFFFF);

                        // Merge platform + destination: e.g. "P1 田景"
                        String platform = safeStr(cols[i][0]);
                        String dest = safeStr(cols[i][2]);
                        String merged = platform.isEmpty() ? dest : platform + " " + dest;
                        views.setTextViewText(dIds[i], merged);

                        views.setTextViewText(tIds[i], safeStr(cols[i][3]));
                        String timeStr = safeStr(cols[i][3]);
                        if ("-".equals(timeStr)) {
                            views.setTextColor(tIds[i], 0xFF999999);
                        } else {
                            views.setTextColor(tIds[i], routeColor);
                        }

                        views.setTextViewText(cIds[i], safeStr(cols[i][4]));
                        hasData = true;
                    }
                }
            }

            if (!hasData) {
                views.setTextViewText(R.id.widget_station, "暫無數據");
            }

            // Weather
            views.setTextViewText(R.id.widget_weather_temp, weather != null ? weather : "");
            views.setTextViewText(R.id.widget_weather_icon_text, weatherIcon != null ? weatherIcon : "");

            // Forecast (3 days)
            if (forecast != null) {
                for (int fi = 0; fi < 3 && fi < forecast.length; fi++) {
                    int fid = fi == 0 ? R.id.widget_forecast_0 : (fi == 1 ? R.id.widget_forecast_1 : R.id.widget_forecast_2);
                    views.setTextViewText(fid, forecast[fi] != null ? forecast[fi] : "");
                }
            }

            // Date + Lunar
            updateDate(views);
            setClickIntent(context, views);
            mgr.updateAppWidget(appWidgetId, views);
        } catch (Exception e) { /* ignore widget update errors */ }
    }

    private static String safeStr(String s) {
        return s != null ? s : "";
    }

    private static void updateDate(RemoteViews views) {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd\nEEE", Locale.CHINESE);
        views.setTextViewText(R.id.widget_date, sdf.format(now));

        try {
            ChineseCalendar cc = new ChineseCalendar();
            cc.setTimeInMillis(now.getTime());
            int lunarMonth = cc.get(ChineseCalendar.MONTH) + 1;
            int lunarDay = cc.get(ChineseCalendar.DAY_OF_MONTH) - 1;
            String monthStr = lunarMonth == 1 ? "正月" : LUNAR_MONTHS[lunarMonth - 1];
            String dayStr = lunarDay >= 0 && lunarDay < LUNAR_DAYS.length ? LUNAR_DAYS[lunarDay] : lunarDay + "日";
            views.setTextViewText(R.id.widget_lunar, "農曆 " + monthStr + dayStr);
        } catch (Exception e) {
            views.setTextViewText(R.id.widget_lunar, "");
        }
    }

    private static void setClickIntent(Context context, RemoteViews views) {
        try {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (android.os.Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_IMMUTABLE;

            Intent refreshIntent = new Intent(context, LRTWidgetUpdateService.class);
            PendingIntent refreshPi = PendingIntent.getService(context, 0, refreshIntent, flags);

            Intent appIntent = new Intent(context, MainActivity.class);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent appPi = PendingIntent.getActivity(context, 1, appIntent, flags);

            views.setOnClickPendingIntent(R.id.widget_weather_icon_text, refreshPi);
            views.setOnClickPendingIntent(R.id.widget_container, refreshPi);
            views.setOnClickPendingIntent(R.id.widget_station, appPi);
            views.setOnClickPendingIntent(R.id.platform_card_0, appPi);
            views.setOnClickPendingIntent(R.id.platform_card_1, appPi);
            views.setOnClickPendingIntent(R.id.platform_card_2, appPi);
            views.setOnClickPendingIntent(R.id.platform_card_3, appPi);
        } catch (Exception e) { /* ignore */ }
    }

    public static class HkoWeather {
        public TemperatureData temperature;
        public static class TemperatureData {
            public java.util.List<TempItem> data;
        }
        public static class TempItem {
            public String place;
            public int value;
            public String unit;
        }
    }
}
