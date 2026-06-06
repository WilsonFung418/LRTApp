package com.lrtapp;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

public final class RouteColors {

    private static final int[] COLORS = {
        0xFF7CB342, // 505 草綠
        0xFF8E44AD, // 507 紫
        0xFFD35400, // 610 深橙
        0xFFEC407A, // 614 粉紅
        0xFF29B6F6, // 614P 淺藍
        0xFF0D47A1, // 615 深藍
        0xFF81C784, // 615P 淺綠
        0xFFBCAAA4, // 705 淺啡
        0xFF795548, // 706 啡
        0xFFF1C40F, // 751 黃
        0xFFB7950B, // 751P 土黃
        0xFFC0392B, // 761P 紅
    };

    private static final String[] ROUTES = {
        "505", "507", "610", "614", "614P", "615", "615P", "705", "706", "751", "751P", "761P"
    };

    private static int indexOf(String routeNo) {
        if (routeNo == null) return -1;
        String r = routeNo.trim();
        for (int i = 0; i < ROUTES.length; i++) {
            if (ROUTES[i].equals(r)) return i;
        }
        return -1;
    }

    public static int getColor(String routeNo) {
        int idx = indexOf(routeNo);
        if (idx >= 0) return COLORS[idx];
        return 0xFF1A73E8; // Fallback 藍
    }

    public static void applyBadge(TextView badgeText, String routeNo) {
        int color = getColor(routeNo);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(24f);
        bg.setColor(color);
        badgeText.setBackground(bg);
        boolean isYellow = routeNo != null && routeNo.trim().equals("751");
        badgeText.setTextColor(isYellow ? 0xFF000000 : 0xFFFFFFFF);
    }

    public static int getTimeColor(String routeNo) {
        return getColor(routeNo);
    }
}
