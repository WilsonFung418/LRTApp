# LRTApp - Light Rail Transit Widget

Android Home Screen Widget for MTR Light Rail (輕鐵) real-time arrivals.

## Features

* **Real-time arrivals** - Shows next train times for Light Rail stations
* **Weather display** - Current temperature and weather warnings
* **Multiple routes** - Supports 610, 507, 614, 615, 706, 751 and more
* **Auto-refresh** - Updates every few minutes automatically

## Station Support

Default station: 大興南 (Tai Hing (South)) - Station ID: 220

Supports all MTR Light Rail stations via official HKGov API.

## Tech Stack

* **Language**: Java
* **Build**: Gradle
* **API**: MTR Light Rail API via HKGov data.gov.hk
* **Weather**: Hong Kong Observatory API

## API Used

Light Rail schedule API:
```
https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=220
```

## Build

```bash
./gradlew assembleDebug
```

APK will be in `app/build/outputs/apk/debug/`

## Permissions

* `INTERNET` - For API calls
* `ACCESS_NETWORK_STATE` - Check connectivity
* `RECEIVE_BOOT_COMPLETED` - Auto-start after reboot
* `SCHEDULE_EXACT_ALARM` - Precise widget updates

## Author

Wilson Fung