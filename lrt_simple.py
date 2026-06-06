#!/usr/bin/env python3
import json
import urllib.request
import sys

# 站名到 ID 的映射
station_map = {
    "大興南": "220",
    "市中心": "295",
    "天瑞": "460",
    "頌富": "468",
    "天耀": "445",
    "樂湖": "448",
    "坑尾村": "425",
    "洪水橋": "190",
    "鍾屋村": "380",
    "泥圍": "370",
    "藍地": "360",
    "兆康": "110",
    "屯門醫院": "100",
    "澤豐": "90",
    "銀圍": "230",
}

def get_arrival_time(station_id, route_no, dest_ch):
    try:
        url = f"https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id={station_id}"
        with urllib.request.urlopen(url, timeout=5) as response:
            data = json.loads(response.read().decode('utf-8'))

        for platform in data.get('platform_list', []):
            for route in platform.get('route_list', []):
                if route['route_no'] == route_no and route['dest_ch'] == dest_ch:
                    time_ch = route.get('time_ch', '')
                    if time_ch == '-':
                        return '已經有車'
                    elif time_ch == '':
                        return '無班次'
                    return time_ch
        return '無班次'
    except Exception as e:
        return f'查詢失敗: {str(e)}'

def main():
    if len(sys.argv) < 2:
        print("用法: python3 lrt_simple.py <站名>")
        print("範例: python3 lrt_simple.py 大興南")
        sys.exit(1)

    station_name = sys.argv[1]
    station_id = station_map.get(station_name)

    if not station_id:
        print(f"❌ 找不到站點：{station_name}")
        print(f"可用站點：{', '.join(station_map.keys())}")
        sys.exit(1)

    print(f"🚃 {station_name} 輕鐵實時到站")
    print("=" * 40)

    # 查詢常用路線
    routes = [
        ("610", "元朗"),
        ("507", "屯門碼頭"),
    ]

    for route_no, dest in routes:
        time = get_arrival_time(station_id, route_no, dest)
        print(f"{route_no} → {dest}: {time}")

if __name__ == "__main__":
    main()
