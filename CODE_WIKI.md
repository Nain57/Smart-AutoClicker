# Klick'r - Smart AutoClicker Code Wiki

## 1. 项目概述

### 1.1 项目简介
Klick'r（原名Smart AutoClicker）是一个开源的Android自动化应用，提供基于图像检测的自动化点击和滑动功能。该应用通过AccessibilityService实现手势注入，支持两种模式：
- **智能模式（Smart Mode）**：基于屏幕图像检测触发自动化操作
- **普通模式（Dumb Mode）**：简单的按顺序执行的自动化操作

### 1.2 主要功能
- 点击和滑动操作自动化
- 图像检测触发（基于OpenCV）
- 高级自动化功能（计数器、Intent、流程控制）
- 多种触发条件（图像、计时器、计数器、广播）
- 快速设置Tile集成
- 应用内教程系统
- 备份和恢复功能
- 调试和分析工具

### 1.3 技术栈
- **语言**：Kotlin
- **最低SDK**：24 (Android 7.0)
- **编译SDK**：36
- **架构**：MVVM + Clean Architecture
- **依赖注入**：Hilt
- **数据库**：Room
- **图像处理**：OpenCV (C++)
- **UI框架**：Android View System + Material Design
- **构建工具**：Gradle (Kotlin DSL)
- **协程**：Kotlinx Coroutines

---

## 2. 项目架构

### 2.1 整体架构
项目采用模块化架构，清晰分离核心功能、特性和应用层：

```
├── build-logic/          # 自定义Gradle插件和构建逻辑
├── core/                 # 核心功能模块
│   ├── common/           # 通用功能组件
│   ├── dumb/             # 普通模式实现
│   └── smart/            # 智能模式实现
├── feature/              # 特性模块
├── smartautoclicker/     # 主应用模块
└── documentation/        # 项目文档
```

### 2.2 模块依赖关系
- **smartautoclicker**（主应用）依赖于所有core和feature模块
- **core/common/*** 被其他core模块和feature模块依赖
- **core/smart/*** 模块之间相互协作
- **feature/*** 模块依赖于core模块，提供用户界面和特定功能

---

## 3. 核心模块详解

### 3.1 Core Common 模块

#### 3.1.1 core:common:actions
提供Android手势执行功能，包括点击、滑动等操作的注入。

#### 3.1.2 core:common:android
Android平台相关的基础功能。

#### 3.1.3 core:common:base
基础组件和工具类：
- `AndroidExecutor.kt`: Android线程执行器
- `DatabaseListUpdater.kt`: 数据库列表更新工具
- `PreferencesDataStore.kt`: DataStore偏好存储
- `SafeBroadcastReceiver.kt`: 安全广播接收器
- `ScenarioStats.kt`: 场景统计

#### 3.1.4 core:common:bitmaps
位图管理功能：
- `BitmapRepository.kt`: 位图仓库接口
- `BitmapRepositoryImpl.kt`: 位图仓库实现
- `BitmapLRUCache.kt`: 位图LRU缓存
- `ConditionBitmapsDataSource.kt`: 条件位图数据源

#### 3.1.5 core:common:display
显示相关功能：
- 屏幕录制
- 显示配置管理

#### 3.1.6 core:common:overlays
悬浮窗UI管理：
- 菜单系统
- 位置选择器
- 视图层管理

#### 3.1.7 core:common:permissions
权限请求和管理功能。

#### 3.1.8 core:common:quality
应用质量监控功能。

#### 3.1.9 core:common:settings
应用设置管理：
- `SettingsRepository.kt`: 设置仓库接口
- `SettingsRepositoryImpl.kt`: 设置仓库实现

#### 3.1.10 core:common:ui
通用UI组件和资源：
- 动画资源
- 颜色主题
- 通用布局组件
- Material Design组件

### 3.2 Core Dumb 模块 (core:dumb)
实现普通模式（Regular Mode）功能：
- `DumbDatabase.kt`: 普通模式数据库
- `DumbRepository.kt`: 数据仓库
- `DumbEngine.kt`: 执行引擎
- `DumbActionExecutor.kt`: 动作执行器

### 3.3 Core Smart 模块

#### 3.3.1 core:smart:database
智能模式数据库层，基于Room：
- `ClickDatabase.kt`: 主数据库（版本18）
- `TutorialDatabase.kt`: 教程数据库
- **实体类**：
  - `ScenarioEntity.kt`: 场景实体
  - `EventEntity.kt`: 事件实体
  - `ActionEntity.kt`: 动作实体
  - `ConditionEntity.kt`: 条件实体
  - `IntentExtraEntity.kt`: Intent额外数据
  - `EventToggleEntity.kt`: 事件开关
  - `ScenarioStatsEntity.kt`: 场景统计
- **DAO**：各实体对应的数据库访问对象
- **数据库迁移**：包含从版本1到18的完整迁移逻辑
- **序列化**：支持场景数据的导入导出

#### 3.3.2 core:smart:debugging
调试功能实现：
- 使用Protocol Buffers定义调试消息格式
- 提供调试报告和时间线功能

#### 3.3.3 core:smart:detection
图像检测核心模块：
- **Native层（C++）**：
  - `smartautoclicker.cpp`: JNI入口
  - `detector/`: 图像检测实现
    - `images/`: 图像处理
    - `matching/`: 模板匹配（基于OpenCV）
  - `jni/`: JNI绑定
- **Kotlin层**：
  - `ImageDetector.kt`: 图像检测器接口
  - `NativeDetector.kt`: 本地检测器实现
  - `DetectionResult.kt`: 检测结果

关键特性：
- 使用OpenCV的模板匹配算法
- 支持检测区域限制
- 可配置检测阈值
- 最低检测质量：400ms

#### 3.3.4 core:smart:domain
领域模型和仓库：
- `IRepository.kt`: 仓库接口
- `Repository.kt`: 仓库实现

#### 3.3.5 core:smart:processing
智能模式处理引擎，负责：
- 场景处理逻辑
- 条件检测
- 动作执行调度

---

## 4. Feature 模块详解

### 4.1 feature:backup
场景备份和恢复功能：
- 支持导入/导出场景
- 提供备份UI

### 4.2 feature:dumb-config
普通模式配置UI：
- 场景配置界面
- 动作编辑（点击、滑动、暂停）
- 重复次数设置

### 4.3 feature:notifications
服务通知管理：
- 前台服务通知
- 通知控制（暂停、继续、停止等）

### 4.4 feature:quick-settings-tile
快速设置Tile集成：
- 从通知栏快速启动/停止场景

### 4.5 feature:revenue
收入相关功能（Play Store版本）：
- Pro版本购买
- 广告加载和显示
- 用户同意管理

### 4.6 feature:review
应用评分和评价功能。

### 4.7 feature:smart-config
智能模式配置UI，提供完整的场景编辑功能：
- 场景管理
- 事件配置
- 动作编辑（点击、滑动、Intent、通知等）
- 条件设置（图像、计时器、计数器、广播）
- 高级设置

### 4.8 feature:smart-debugging
智能模式调试工具：
- 调试报告
- 事件时间线
- 检测结果分析

### 4.9 feature:tutorial
交互式教程系统：
- 教程游戏
- 引导式学习
- 进度跟踪

---

## 5. 主应用模块 (smartautoclicker)

### 5.1 核心服务

#### SmartAutoClickerService
继承自AccessibilityService，是应用的核心服务：

**主要职责**：
- 作为AccessibilityService提供手势注入能力
- 管理LocalService的生命周期
- 处理音量键事件（用于停止场景）
- 前台服务管理
- 显示配置监控
- 与Quick Settings Tile集成

**关键方法**：
- `onServiceConnected()`: 服务连接时初始化
- `onLocalServiceStarted()`: 本地服务启动回调
- `onLocalServiceStopped()`: 本地服务停止回调
- `onKeyEvent()`: 处理按键事件
- `dump()`: 支持通过adb dumpsys查看服务状态

### 5.2 主要界面

#### ScenarioActivity
场景列表和管理界面：
- 显示所有场景（智能和普通）
- 场景排序和过滤
- 创建、编辑、删除场景
- 备份和恢复

#### SettingsActivity
应用设置界面。

### 5.3 本地服务 (LocalService)
提供应用内部的服务API，管理场景的启动和停止：
- 智能场景启动/停止
- 普通场景启动/停止
- 悬浮窗管理

---

## 6. 关键技术实现

### 6.1 图像检测流程
1. 通过MediaProjection录制屏幕
2. 将屏幕帧传递给NativeDetector
3. 使用OpenCV进行模板匹配
4. 检查检测结果是否满足阈值要求
5. 触发相应的事件和动作

### 6.2 场景处理流程
1. 场景启动 → 初始化处理引擎
2. 持续获取屏幕帧 → 检测条件
3. 条件满足 → 执行关联动作
4. 动作执行 → 注入手势到系统
5. 循环直到场景停止

### 6.3 依赖注入 (Hilt)
项目使用Hilt进行依赖注入：
- 所有核心组件通过Hilt管理
- 支持单例模式
- 模块化的依赖配置

### 6.4 数据库设计
使用Room数据库，主要表结构：
- **scenarios**: 场景表
- **events**: 事件表（关联到场景）
- **actions**: 动作表（关联到事件）
- **conditions**: 条件表（关联到事件）
- **intent_extras**: Intent额外数据表
- **event_toggles**: 事件开关表
- **scenario_stats**: 场景统计表

### 6.5 构建变体
项目支持多种构建变体：
- **Flavors**:
  - `playStore`: Google Play版本，包含广告和付费功能
  - `fDroid`: F-Droid版本，完全开源无商业功能
- **Build Types**:
  - `debug`: 调试版本
  - `release`: 发布版本

---

## 7. 依赖关系

### 7.1 主要依赖
```kotlin
// AndroidX
androidx.appcompat
androidx.core.ktx
androidx.datastore
androidx.recyclerview
androidx.fragment.ktx
androidx.lifecycle.*
androidx.room.*

// 第三方
kotlinx.coroutines
google.dagger.hilt
google.protobuf
airbnb.lottie
google.material
opencv (C++)

// Play Store 专用
android.billingClient
google.firebase.*
google.gms.ads
google.play.review
```

### 7.2 Gradle插件
- 自定义插件（build-logic）
- Android Application/Library
- Hilt
- Room
- Protobuf
- Kotlin Serialization
- Navigation Safe Args

---

## 8. 项目运行方式

### 8.1 环境要求
- Android Studio Hedgehog或更高版本
- JDK 21
- Android SDK 36
- NDK 28.2.13676358（用于OpenCV编译）

### 8.2 构建步骤
1. 克隆项目
2. 使用Android Studio打开
3. 同步Gradle
4. 选择构建变体（fDroidDebug推荐用于开发）
5. 连接Android设备或启动模拟器
6. 点击运行按钮

### 8.3 权限要求
应用需要以下权限：
- **ACCESSIBILITY_SERVICE**: 核心功能必需
- **BIND_ACCESSIBILITY_SERVICE**: 同上
- **SYSTEM_ALERT_WINDOW**: 悬浮窗显示
- **FOREGROUND_SERVICE**: 前台服务
- **POST_NOTIFICATIONS**: 显示通知
- **WRITE_EXTERNAL_STORAGE** (API < 33): 备份功能

---

## 9. 开发指南

### 9.1 代码风格
- 遵循Kotlin官方编码规范
- 使用ktlint进行格式化
- 所有模块有统一的代码风格配置

### 9.2 架构原则
- **Clean Architecture**: 分离关注点
- **MVVM**: UI层使用ViewModel
- **Repository Pattern**: 数据层抽象
- **Dependency Injection**: 使用Hilt

### 9.3 测试
- 单元测试使用JUnit和MockK
- 本地测试（Local Unit Tests）
- 插桩测试（Instrumented Tests）

### 9.4 调试技巧
- 使用`adb shell dumpsys activity service com.buzbuz.smartautoclicker`查看服务状态
- 启用调试模式查看检测过程
- 使用智能调试功能分析场景执行

---

## 10. 版本历史

当前版本：3.5.1 (versionCode: 85)

主要版本更新：
- 数据库版本18：最新的数据库结构
- 支持两种自动化模式
- 完整的教程系统
- 调试和分析工具
- 多语言支持（10+语言）

---

## 11. 参考资料

- [项目README](file:///workspace/README.md)
- [项目GitHub Wiki](https://github.com/Nain57/Smart-AutoClicker/wiki)
- [OpenCV文档](https://docs.opencv.org/)
- [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)

---

*本文档最后更新时间：2025年5月30日*
