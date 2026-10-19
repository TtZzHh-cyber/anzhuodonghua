# 抖音过渡动画独立演示 APK（Kotlin）

这是一个可在 Android Studio 中打开和构建的独立演示项目，用于复现“竖向信息流 + 松手后弹簧式切页”的交互概念。

## 重要区别

- 原始 `抖音过渡动画.dylib` 是 iOS ARM64e Mach-O 动态库，字符串显示它会拦截 `AWEFeedTableViewController` 的 `scrollViewWillEndDragging:withVelocity:targetContentOffset:`，并使用 UIKit 的弹簧动画 API。
- 本项目不是对 `.dylib` 的二进制转换，也不包含其原始代码；它是依据可见符号和行为线索编写的 Kotlin 演示实现。
- 本 APK 只在自己的演示界面生效，不会修改或注入官方安卓抖音。普通、未 Root 的 APK 不能随意进入另一个应用进程修改其内部方法。
- 原文件引用了 `Ceshi.dylib`，若该依赖承担部分逻辑，仅凭当前 dylib 不能保证完全复原原效果。

## 功能

- 五个可上下滑动的演示页面。
- 根据拖动距离和释放速度决定是否切换页面。
- 页面边界有阻尼效果。
- 切页时使用自定义的阻尼/回弹插值模拟弹簧感。
- 仅依赖 Android SDK；无需第三方库。

## 环境要求

- Android Studio（建议较新版本）
- JDK 17
- Android SDK Platform 35
- Gradle 由 Android Studio 按项目配置自动下载/使用

## 构建 APK

1. 解压项目 ZIP。
2. Android Studio 选择 **Open**，打开 `DouyinTransitionDemo` 文件夹。
3. 等待 Gradle Sync 完成；如果提示安装 Android SDK Platform 35，请按提示安装。
4. 连接安卓手机并开启开发者选项和 USB 调试，或启动模拟器。
5. 点击 **Run**，运行 `app` 配置。

生成 Debug APK：
- 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- APK 通常位于 `app/build/outputs/apk/debug/app-debug.apk`

也可在项目根目录执行：
```bash
./gradlew assembleDebug
```
如果项目中没有 Gradle Wrapper 文件，可直接使用 Android Studio 的 Gradle 工具窗口构建，或在本机安装 Gradle 后执行：
```bash
gradle assembleDebug
```

## 操作说明

- 向上滑动：切换到下一页。
- 向下滑动：切换到上一页。
- 快速甩动或长距离拖动会更容易触发切页。
- 在首尾页面继续向外拖动时有阻尼。

## 参数调节

主要参数都在 `MainActivity.kt` 的 `settleAfterRelease()` / `animateTo()` 中：

- `pageHeight * 0.22f`：触发翻页的拖动距离阈值。
- `abs(velocityY) > 0.65f`：快速甩动阈值，单位约为 px/ms。
- `duration` 的 240–380 ms 范围：动画时长。
- `damping`、`frequency`：自定义弹簧曲线参数。

这些是演示调参值，并非从原插件反汇编得到的真实参数。

## 当前限制 / 下一步

1. 这是文字页面演示，没有视频解码、播放状态同步或抖音 UI。
2. 不能在普通无 Root 环境下直接更改官方抖音的滚动行为。
3. 如果希望进一步逼近原效果，需要取得 `Ceshi.dylib`，并对原动态库做完整反汇编/伪代码分析；再通过安卓端合法可用的集成方式实现对应行为。
