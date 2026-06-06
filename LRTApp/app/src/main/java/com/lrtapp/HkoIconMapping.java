package com.lrtapp;

/**
 * HKO 天氣圖標映射表 (Local Drawable)
 * 
 * HKO 官方圖片 CDN 已改變(404)，改用自定義顏色 Drawable (XML) 代替。
 * Widget 繼續使用 Emoji (RemoteViews 相容性更好)
 * MainActivity 可使用此映射顯示彩色標籤。
 * 
 * Fallback: pic_64
 */
public final class HkoIconMapping {

    private HkoIconMapping() {}

    /** 將 HKO warning code 對應到本地 drawable ID */
    public static int warningCodeToDrawable(String code) {
        if (code == null) return R.drawable.pic_64;
        switch (code) {
            case "WRAING":  return R.drawable.warn_raina;
            case "WRAINR":  return R.drawable.warn_rainr;
            case "WRAINB":  return R.drawable.warn_rainb;
            case "WTC1":    return R.drawable.warn_tc1;
            case "WTC3":    return R.drawable.warn_tc3;
            case "WTC8NE":
            case "WTC8NW":
            case "WTC8SE":
            case "WTC8SW":
            case "WTC8":    return R.drawable.warn_tc8;
            case "WTC9":    return R.drawable.warn_tc9;
            case "WTC10":   return R.drawable.warn_tc10;
            case "WMSGNL":  return R.drawable.warn_msgnl;
            case "WL":      return R.drawable.warn_ls;
            case "WF":      return R.drawable.warn_flood;
            case "WTS":     return R.drawable.warn_thunder;
            case "WFNTSA":  return R.drawable.warn_hot;
            case "WCOOL":   return R.drawable.warn_cold;
            case "WFROST":  return R.drawable.warn_frost;
            case "WFOG":    return R.drawable.warn_fog;
            case "WFIRE":   return R.drawable.warn_fire;
            default:        return R.drawable.pic_64;
        }
    }

    /** 將 HKO PicCode (50-99) 對應到本地 drawable ID */
    public static int picCodeToDrawable(int code) {
        switch (code) {
            case 50: case 51: return R.drawable.pic_50;
            case 52: case 53: return R.drawable.pic_52;
            case 54: case 55: return R.drawable.pic_54;
            case 60: case 61: return R.drawable.pic_60;
            case 62: case 63: return R.drawable.pic_62;
            case 64: case 65: return R.drawable.pic_64;
            case 70: case 71: return R.drawable.pic_70;
            case 72: case 73: return R.drawable.pic_72;
            case 74: case 75: return R.drawable.pic_74;
            case 76: case 77: return R.drawable.pic_76;
            case 78: case 79: return R.drawable.pic_78;
            case 80: case 81: return R.drawable.pic_80;
            case 82: case 83: return R.drawable.pic_82;
            case 84: case 85: return R.drawable.pic_84;
            default:          return R.drawable.pic_64;
        }
    }

    /** 取得 warning 代碼對應的中文名稱 */
    public static String getWarningLabel(String code) {
        if (code == null) return "";
        switch (code) {
            case "WRAING":  return "黃色暴雨警告";
            case "WRAINR":  return "紅色暴雨警告";
            case "WRAINB":  return "黑色暴雨警告";
            case "WTC1":    return "一號戒備信號";
            case "WTC3":    return "三號強風信號";
            case "WTC8NE":
            case "WTC8NW":
            case "WTC8SE":
            case "WTC8SW":
            case "WTC8":    return "八號烈風信號";
            case "WTC9":    return "九號烈風信號";
            case "WTC10":   return "十號颶風信號";
            case "WMSGNL":  return "強烈季候風信號";
            case "WL":      return "山泥傾瀉警告";
            case "WF":      return "水浸警告";
            case "WTS":     return "雷暴警告";
            case "WFNTSA":  return "酷熱天氣警告";
            case "WCOOL":   return "寒冷天氣警告";
            case "WFROST":  return "霜凍警告";
            case "WFOG":    return "濃霧警告";
            default:        return "";
        }
    }
}
