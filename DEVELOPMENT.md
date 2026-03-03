# DEVELOPMENT

本文档用于说明本仓库的开发环境、构建方式与常用调试手段。

## 1. 项目概览
- 项目名称：TrafficManager
- 技术栈：Kotlin + Android + Gradle Kotlin DSL
- 目标：在 Root + LSPosed 环境下，根据 Wi-Fi 规则自动切换默认数据卡（SIM1/SIM2）
- 关键目录：
  - `app/src/main/java/com/laros/lsp/traffics`：核心业务代码
  - `app/src/main/res`：界面与资源
  - `docs/`：文档

## 2. 开发环境
- JDK：17（推荐）
- Android Studio：稳定版
- Android SDK：按 `app/build.gradle.kts` 要求安装
- 设备：建议双卡设备（主要验证环境：Xiaomi HyperOS 3）

## 3. 构建
1. 将 LSPosed API jar 放到：`app/libs/api-82.jar`
2. 构建 Debug：

```powershell
.\gradlew.bat :app:assembleDebug
```

输出 APK：`app/build/outputs/apk/debug/app-debug.apk`

## 4. 安装与运行
- 安装 Debug：

```powershell
.\gradlew.bat :app:installDebug
```

- 启动 App：

```powershell
adb shell am start -n com.laros.lsp.traffics/.MainActivity
```

## 5. LSPosed 作用域建议
- 必选：`com.android.phone`
- 可选：根据 ROM 兼容性追加 `android` 作用域

## 6. 调试建议
- App 内导出日志或查看目录：`Android/data/com.laros.lsp.traffics/files/logs/`
- 验证 Root 可用：`adb shell su -c id`

## 7. 分支与提交
- 禁止直接向 `main` 提交
- 使用 `feature/*`、`fix/*`、`docs/*` 分支
- 提交信息遵循 Conventional Commits：`type(scope): summary`