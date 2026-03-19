# TrafficSIM

TrafficSIM 是一个基于 LSPosed 的双卡数据卡自动切换模块，可按 Wi-Fi `SSID` / `BSSID` 规则在 `SIM1` 和 `SIM2` 之间自动切换默认数据卡。

> 项目不申请 `INTERNET` 权限，也不包含联网更新逻辑。
> 主仓库：<https://github.com/L-aros/TrafficSIM>

## 重要说明

- `v1.0.4` 起更换了签名证书，旧版本无法覆盖安装，升级前请先卸载旧版。
- 当前使用 libxposed API `101.0.0` 编译。
- 焦点通知白名单绕过现已改为高级页可配置开关，默认开启。
- 当前推荐作用域里不再包含 `android` 和 `miui.systemui.plugin`。

## 功能特性

- 按 `SSID` / `BSSID` 匹配规则自动切换数据卡
- 支持优先级、冷却时间、离开 Wi-Fi 回切
- 支持无 Wi-Fi 目标卡与立即切换
- 支持省电模式 / 常驻模式切换，默认建议使用常驻模式
- 支持焦点通知结果展示与调试通知
- 高级页支持“移除焦点通知白名单校验”开关
- 支持自检、权限说明、日志导出和 JSON 高级配置

## 构建

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

Debug APK 默认输出在：

- `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`
- `app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk`

## 推荐作用域

- `com.android.systemui`
- `com.android.phone`
- `com.xiaomi.phone`
- `com.qti.phone`
- `com.qualcomm.qti.telephonyservice`
- `com.android.services.telephony`
- `com.android.telephony`

## 首次使用

1. 安装 APK，并在 LSPosed 中启用模块。
2. 勾选上面的推荐作用域后重启系统界面或重启设备。
3. 打开 TrafficSIM，授予定位、附近 Wi-Fi、电话状态、通知等权限。
4. 在“设置 > 高级配置”确认运行模式，默认建议使用常驻模式。
5. 如需发送焦点通知，在“设置 > 高级配置”保持“移除焦点通知白名单校验”为开启。
6. 在首页或规则页新增 Wi-Fi 规则后启动自动切卡。

## 配置示例

```json
{
  "enabled": true,
  "powerSaveMode": false,
  "removeFocusWhitelistCheck": true,
  "hideBackgroundTask": false,
  "screenOnIntervalSec": 20,
  "screenOffIntervalSec": 90,
  "cooldownSec": 90,
  "leaveDelaySec": 180,
  "leaveMissThreshold": 3,
  "revertOnLeave": true,
  "fixedLeaveSlot": null,
  "noWifiSlot": 0,
  "noWifiImmediate": true,
  "logRetentionDays": 7,
  "logMaxMb": 10,
  "rules": [
    {
      "id": "home_wifi_to_sim1",
      "priority": 100,
      "ssid": "MyHomeWiFi",
      "bssid": null,
      "targetSlot": 0
    }
  ]
}
```

## 排查建议

- 检查模块是否已在 LSPosed 中启用且作用域完整。
- 检查系统定位总开关、通知权限、电话状态权限是否已授予。
- 通过“设置 > 自检”确认 Wi-Fi、数据卡、权限和运行模式状态。
- 焦点通知异常时，优先检查“移除焦点通知白名单校验”是否开启，以及系统是否仍有限制项未移除。

## 致谢

- HyperCeiler：<https://github.com/ReChronoRain/HyperCeiler>

## License

MIT，详见 [LICENSE](LICENSE)
