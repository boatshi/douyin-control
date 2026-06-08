# 📱 抖音管控 App — 云帆乐园

> 为孩子（大班→六年级）定制的 Android 平板学习激励工具
> **做题赚积分 → 积分兑换抖音观看时间**

---

## 🎯 项目背景

孩子每天想看抖音短视频，但需要建立"先学习后娱乐"的习惯。通过原生 Android App 内嵌浏览器内核加载抖音，配合做题系统，实现：

- ✅ **做题赚积分** — 答对给分，答错不给分
- ✅ **积分换时间** — 用积分兑换抖音观看时长
- ✅ **错题复习** — 自动收录错题，重新做对也给积分
- ✅ **AI 出题** — 实时生成不重复题目，覆盖大班到六年级
- ✅ **自动管控** — 时间到自动锁定，按实际观看时长扣分

---

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| 🏠 **首页** | 积分显示 + 4个功能入口 + 家长设置（点击弹出密码） |
| 📚 **做题系统** | 选择题模式，答对加分，答错收错题本，支持数学/语文 |
| 🤖 **AI 出题** | 对接 DeepSeek API，自动去重不重复，按家长设置的年级出题 |
| ❌ **错题本** | 按科目分组，翻页展示，重新做对也给积分 |
| 🎬 **看抖音** | GeckoView (Firefox) 内核加载抖音，首屏预加载秒开，时间到自动锁定 |
| 🏆 **学习统计** | 今日概况 + 积分流水翻页 |
| ⚙️ **家长设置** | 积分规则、年级设置、题目管理、AI 出题、API 配置、修改密码、数据导出/重置 |
| 📱 **华为平板适配** | GeckoView 内核替代系统 WebView，提升加载速度 |

---

## 🏗️ 技术架构

| 层面 | 技术 |
|------|------|
| 语言 | **Kotlin** |
| UI | **Jetpack Compose**（声明式 UI） |
| 数据库 | **Room** (SQLite) |
| 浏览器内核 | **GeckoView** (Firefox) |
| AI 接口 | **DeepSeek API**（兼容 OpenAI 格式） |
| 导航 | **Navigation Compose** |
| 配置存储 | **DataStore** |
| 网络 | **OkHttp** |
| 开发工具 | **Android Studio** |
| 最低版本 | **Android 7.0 (API 24)** |

---

## 📦 项目结构

```
抖音管控App/
├── app/
│   ├── src/main/
│   │   ├── java/com/yunfan/douyincontrol/
│   │   │   ├── MainActivity.kt              # 主入口
│   │   │   ├── App.kt                       # Application 类 + GeckoRuntime
│   │   │   ├── api/
│   │   │   │   ├── AiService.kt             # DeepSeek API 调用
│   │   │   │   └── AiRepository.kt          # AI 出题仓库
│   │   │   ├── data/
│   │   │   │   ├── database/                # Room 数据库 (Entity/DAO)
│   │   │   │   ├── repository/              # 数据仓库
│   │   │   │   └── seed/                    # 种子题库
│   │   │   ├── engine/
│   │   │   │   ├── QuizEngine.kt            # 出题引擎（去重 + AI 补充）
│   │   │   │   └── TimerEngine.kt           # 计时引擎
│   │   │   ├── ui/
│   │   │   │   ├── screen/                  # 所有界面
│   │   │   │   │   ├── HomeScreen.kt        # 首页
│   │   │   │   │   ├── StudyScreen.kt       # 做题
│   │   │   │   │   ├── DouyinScreen.kt      # 抖音 (GeckoView)
│   │   │   │   │   ├── WrongBookScreen.kt   # 错题本
│   │   │   │   │   ├── StatsScreen.kt       # 学习统计
│   │   │   │   │   └── settings/            # 家长设置各页面
│   │   │   │   ├── component/               # 可复用组件
│   │   │   │   ├── navigation/              # 导航图
│   │   │   │   └── theme/                   # 主题/配色
│   │   │   └── util/                        # 工具类
│   │   ├── assets/
│   │   │   └── seed_questions.json           # 种子题库（33道预置题）
│   │   └── res/
│   │       └── drawable/                    # 图标资源
│   └── build.gradle.kts
├── docs/superpowers/                        # 设计文档和实施计划
└── icon_preview.html                        # 图标预览
```

---

## 🚀 快速开始

### 环境要求

- **Android Studio** Ladybug+
- **JDK** 17+
- **Android SDK** API 35
- **Gradle** 8.7+

### 编译安装

```bash
# 克隆项目
git clone https://github.com/boatshi/yunfan-paradise.git
cd yunfan-paradise

# 切换到抖音管控分支
git checkout douyin-control

# 编译安装到设备
./gradlew installDebug
```

或者直接在 **Android Studio** 中打开项目，点击 Run ▶️

### 首次使用

1. 打开 App → **长按底部进入家长设置**
2. 点 **API配置** → 填入 DeepSeek API 地址和 Key
3. 点 **AI出题** → 选择年级和科目 → 生成题目
4. 回到首页 → **去做题** 赚积分
5. 积分够了 → **看抖音** 消费积分

---

## 📊 积分规则

| 项目 | 默认值 | 可调范围 |
|------|:------:|:--------:|
| 做对一道题 | +10 积分 | 1-100 |
| 错题重做对 | +10 积分 | 1-100 |
| 看抖音每分钟 | -10 积分 | 1-100 |
| 最低消费门槛 | 10 积分 | 1-1000 |

---

## 🎨 App 图标

图标采用紫蓝渐变背景，包含以下元素：

| 元素 | 寓意 |
|------|------|
| ☁️ 云朵 | 云帆 |
| ⭐ 绿星 | 快乐学习 |
| ⛵ 帆船 | 扬帆起航 |
| 🌊 波纹 | 乘风破浪 |

---

## 🔧 技术要点

### GeckoView 浏览器内核

华为平板无法使用 X5 内核（腾讯已停更），且无法注册 Chrome WebView（鸿蒙限制），因此使用 **Mozilla GeckoView**（Firefox 内核）作为页面渲染引擎：

- 开源、可打包进 APK、鸿蒙兼容
- 桌面版 User-Agent 加载 douyin.com
- 首页预加载，点进去秒开
- 退出页面自动关闭 Session 停掉声音

### AI 出题去重

- 每次 AI 出题前检查数据库是否已有相同题目（精确匹配 + Levenshtein 相似度）
- 重复的题目自动丢弃，避免题库膨胀
- 孩子做完所有题目后才触发 AI 补充

---

## 📝 许可证

本项目为个人开发的教育工具，供学习交流使用。

---

## 分支说明

| 分支 | 内容 |
|------|------|
| `douyin-control` | 📱 抖音管控 App（当前项目） |
