# TrafficSIM

TrafficSIM 是一个 LSPosed 模块，用于根据 Wi-Fi `SSID` / `BSSID` 规则自动切换默认数据卡（SIM1 / SIM2）。

> 本项目不申请 `INTERNET` 权限，也不包含联网逻辑。
>
> 如需更新，请优先前往 [主仓库](https://github.com/L-aros/TrafficSIM)。

## 重要提示

- `v1.0.4` 起更换签名证书，旧版本无法覆盖更新，请先卸载再安装新版本。
- 当前版本仅保留 `LSPosed` 单链路切卡，不再提供 Root 兜底切卡。
- 当前主要验证环境为小米 HyperOS 3，其他 ROM 可能存在兼容差异。

## 功能特性

- 支持按 `SSID` / `BSSID` 匹配规则
- 支持规则优先级、冷却时间、离开 Wi-Fi 回切
- 支持无 Wi-Fi 目标卡与立即切换策略
- 支持快速学习当前 Wi-Fi 生成规则
- 支持自检页、权限说明页、焦点通知调试
- 支持省电模式 / 常驻模式切换
- 支持切卡事件通知与日志导出

## 环境要求

- Android 10+
- 已安装 LSPosed（建议 Zygisk）
- 建议双卡设备
- 不需要 Root

## 构建

1. 将 LSPosed API jar 放到 `app/libs/api-82.jar`
2. 构建 Debug 包：

```powershell
.\gradlew.bat :app:assembleDebug
```

Debug 输出为按 ABI 拆分的 APK：

- `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`
- `app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk`

## 安装与启用

1. 如果你当前安装的是 `v1.0.3` 或更早版本，先卸载旧版。
2. 安装与设备 ABI 匹配的 APK。
3. 在 LSPosed 中启用模块。
4. 勾选推荐作用域：
   - `com.android.phone`
   - `com.xiaomi.phone`
   - `com.qti.phone`
   - `com.qualcomm.qti.telephonyservice`
   - `com.android.services.telephony`
   - `com.android.telephony`
5. 如仍不生效，可额外尝试勾选 `android` 作用域。
6. 重启设备。

## 首次使用

1. 打开 TrafficSIM，授予定位、附近 Wi-Fi、电话状态、通知等权限。
2. 确认系统“定位总开关”处于开启状态。
3. 进入“设置 > 自检”，确认 Wi-Fi、数据卡、权限和运行模式正常。
4. 在控制台使用“学习 -> SIM1 / SIM2”快速生成规则，或在“规则”页手动新增。
5. 选择运行模式：
   - 常驻模式：更及时、更稳定，推荐
   - 省电模式：更省电，但可能延迟或漏触发
6. 点击“启动自动切卡”。

## 配置示例

```json
{
  "enabled": true,
  "powerSaveMode": false,
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

## 常见排查

- 检查定位权限、附近 Wi-Fi 权限、电话状态权限、通知权限是否已授予
- 检查系统定位总开关是否开启
- 检查 LSPosed 模块是否启用、作用域是否完整
- 使用“设置 > 自检”查看当前 Wi-Fi、数据卡、权限与最近切卡结果
- 使用“设置 > 更多 > 权限说明”查看每个权限的用途
- 日志可在“关于”页导出；若用 ADB 查看，可执行：

```powershell
adb shell run-as com.laros.lsp.traffics ls files/logs
```

## 开源协议

MIT，详见 [LICENSE](LICENSE)
