# ZeroDroid — Battery Optimization

How ZeroDroid stays at zero idle battery.
For the project overview, screenshots, and setup, see the [main README](../README.md).

[← Back to README](../README.md)

---

| Optimization | Before | After | Impact |
|-------------|--------|-------|--------|
| Home screen | SensorScreen (6 sensors at 60Hz) | Dashboard (static Build info) | ~360 events/sec → 0 |
| Sensor polling | `SENSOR_DELAY_UI` (60Hz) | `SENSOR_DELAY_NORMAL` (5Hz) | 12x fewer events |
| BLE scan mode | `SCAN_MODE_LOW_LATENCY` | `SCAN_MODE_LOW_POWER` | ~10x less battery |
| Auto-start | Sensors + WiFi start on screen load | Manual start button required | No scanning unless you ask |
| Auto-stop | Never (runs forever) | Sensors: 60s, WiFi: 30s, BLE: 30s | Forgotten scans can't drain battery |
| Service init | 25+ services created at app launch | All `by lazy` (created on first use) | Startup cost → near zero |
| Navigation | `saveState=true` (keeps disposed screens alive) | No saveState (dispose fires on navigate away) | Scanners stop when you leave |
