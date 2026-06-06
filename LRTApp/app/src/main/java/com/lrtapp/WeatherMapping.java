package com.lrtapp;

import java.util.HashMap;
import java.util.Map;

/**
 * HKO Weather Code → Local Emoji/Drawable Mapping
 * 
 * 天氣警告 (warnsum) 與天氣狀況 (PicCode) 對照表
 * Fallback: ☁️ 確保 Widget 唔會空白
 */
public final class WeatherMapping {

    private WeatherMapping() {}

    // ═══════════════════════════════════════════
    // 天氣警告代碼 → Emoji 映射 (warnsum)
    // 來源: https://www.weather.gov.hk/images/wxinfo/warning/
    // ═══════════════════════════════════════════
    private static final Map<String, String> WARNING_MAP = new HashMap<>();
    static {
        // 暴雨警告
        WARNING_MAP.put("WRAING",  "🌧️🟡");  // 黃色暴雨
        WARNING_MAP.put("WRAINR",  "🌧️🔴");  // 紅色暴雨
        WARNING_MAP.put("WRAINB",  "🌧️⚫");  // 黑色暴雨

        // 熱帶氣旋警告
        WARNING_MAP.put("WTC1",    "🌬️");    // 一號戒備
        WARNING_MAP.put("WTC3",    "🌀");     // 三號強風
        WARNING_MAP.put("WTC8NE",  "🌀🔴");   // 八號東北
        WARNING_MAP.put("WTC8NW",  "🌀🔴");   // 八號西北
        WARNING_MAP.put("WTC8SE",  "🌀🔴");   // 八號東南
        WARNING_MAP.put("WTC8SW",  "🌀🔴");   // 八號西南
        WARNING_MAP.put("WTC8",    "🌀🔴");   // 八號烈風
        WARNING_MAP.put("WTC9",    "🌀⚫");   // 九號烈風
        WARNING_MAP.put("WTC10",   "🌀⚫");   // 十號颶風

        // 其他警告
        WARNING_MAP.put("WMSGNL",  "💨");     // 強烈季候風
        WARNING_MAP.put("WL",      "🏔️");    // 山泥傾瀉
        WARNING_MAP.put("WF",      "🌊");     // 水浸
        WARNING_MAP.put("WTS",     "⛈️");    // 雷暴
        WARNING_MAP.put("WFNTSA",  "🔥");     // 酷熱
        WARNING_MAP.put("WCOOL",   "❄️");    // 寒冷
        WARNING_MAP.put("WFROST",  "🥶");     // 霜凍
        WARNING_MAP.put("WFOG",    "🌫️");    // 濃霧
        WARNING_MAP.put("WFS",     "🌊");     // 海嘯
        WARNING_MAP.put("WHS",     "🥵");     // 極端酷熱
    }

    // ═══════════════════════════════════════════
    // 天氣狀況 PicCode → Emoji 映射 (0-99)
    // 來源: https://www.weather.gov.hk/images/wxinfo/pic{PicCode}.png
    // ═══════════════════════════════════════════
    private static final int[] PICCODE_RANGES = {
        // [start, end, icon_type]
        // 晴天
        50, 53, 0,   // ☀️ 天晴/陽光
        54, 55, 1,   // 🌤️ 短暫陽光
        
        // 多雲
        60, 61, 2,   // ⛅ 幾陣微雨
        62, 63, 3,   // ☁️ 密雲
        64, 65, 4,   // 🌥️ 多雲
        
        // 雨
        70, 72, 5,   // 🌦️ 微雨
        73, 75, 6,   // 🌧️ 有雨
        76, 79, 7,   // 🌧️ 大雨/雷雨
        
        // 惡劣
        80, 83, 8,   // ⛈️ 雷暴
        84, 86, 9,   // 🌦️ 雨/雷
        87, 90, 10,  // 🌪️ 惡劣
        91, 99, 11,  // 🌫️ 煙霞/灰濛
    };

    private static final String[] PICCODE_EMOJIS = {
        "☀️", "🌤️", "⛅", "☁️", "🌥️",
        "🌦️", "🌧️", "🌧️", "⛈️", "🌦️",
        "🌪️", "🌫️"
    };

    private static final String[] PICCODE_LABELS = {
        "天晴", "短暫陽光", "微雨", "密雲", "多雲",
        "微雨", "有雨", "大雨", "雷暴", "雨",
        "惡劣", "煙霞"
    };

    /**
     * 將 HKO warning code 轉換為顯示用 Emoji + 文字
     * @param code HKO 警告代碼 (如 "WRAING")
     * @return 顯示用字串 (如 "🌧️🟡 黃雨")
     */
    public static String getWarningDisplay(String code) {
        if (code == null) return "";
        String emoji = WARNING_MAP.get(code);
        if (emoji != null) {
            String label = getWarningLabel(code);
            return emoji + " " + label;
        }
        return ""; // Unknown code → don't show
    }

    /**
     * 取得警告代碼的中文名稱
     */
    public static String getWarningLabel(String code) {
        if (code == null) return "";
        switch (code) {
            case "WRAING": return "黃雨";
            case "WRAINR": return "紅雨";
            case "WRAINB": return "黑雨";
            case "WTC1":   return "一號波";
            case "WTC3":   return "三號波";
            case "WTC8":   return "八號波";
            case "WTC9":   return "九號波";
            case "WTC10":  return "十號波";
            case "WMSGNL": return "季候風";
            case "WL":     return "山泥";
            case "WF":     return "水浸";
            case "WTS":    return "雷暴";
            case "WFNTSA": return "酷熱";
            case "WCOOL":  return "寒冷";
            case "WFROST": return "霜凍";
            case "WFOG":   return "濃霧";
            case "WHS":    return "極端酷熱";
            default:       return "";
        }
    }

    /**
     * 將 HKO icon code (PicCode) 轉換為 Emoji
     * @param picCode 0-99 的天氣 icon code
     * @return Emoji 字串 (Fallback: ☁️)
     */
    public static String picCodeToEmoji(int picCode) {
        for (int i = 0; i < PICCODE_RANGES.length; i += 3) {
            if (picCode >= PICCODE_RANGES[i] && picCode <= PICCODE_RANGES[i+1]) {
                return PICCODE_EMOJIS[PICCODE_RANGES[i+2]];
            }
        }
        return "☁️"; // Fallback
    }

    /**
     * 將 HKO icon code 轉換為中文描述
     * @param picCode 0-99 的天氣 icon code
     * @return 中文描述 (Fallback: "多雲")
     */
    public static String picCodeToLabel(int picCode) {
        for (int i = 0; i < PICCODE_RANGES.length; i += 3) {
            if (picCode >= PICCODE_RANGES[i] && picCode <= PICCODE_RANGES[i+1]) {
                return PICCODE_LABELS[PICCODE_RANGES[i+2]];
            }
        }
        return "多雲"; // Fallback
    }

    /**
     * 將 WMO weather code (Open-Meteo) 轉換為 Emoji
     * WMO Code 參考: https://open-meteo.com/en/docs
     */
    public static String wmoCodeToEmoji(int code) {
        if (code == 0) return "☀️";        // Clear sky
        if (code <= 3) return "⛅";        // Mainly clear/partly cloudy
        if (code <= 10) return "🌤️";      // Haze
        if (code <= 19) return "🌫️";      // Fog
        if (code <= 29) return "⛈️";      // Thunderstorm
        if (code <= 39) return "🌧️";      // Dust/sand storm
        if (code <= 49) return "🌫️";      // Fog
        if (code <= 55) return "🌦️";      // Drizzle
        if (code <= 65) return "🌧️";      // Rain
        if (code <= 79) return "🌨️";      // Snow
        if (code <= 82) return "🌧️";      // Rain showers
        if (code <= 86) return "🌨️";      // Snow showers
        if (code <= 99) return "⛈️";      // Thunderstorm
        return "☁️";                       // Fallback
    }
}
