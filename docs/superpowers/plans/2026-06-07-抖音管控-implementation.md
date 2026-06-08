# 抖音管控App 实施计划

> **For agentic workers:** 使用 subagent-driven-development 或 executing-plans 技能逐步执行。步骤使用 `- [ ]` 标记追踪。

**目标：** 开发原生 Android App，孩子做题赚积分 → 积分兑换抖音观看时间，AI 实时出题去重。

**架构：** 单 Activity + Jetpack Compose 导航，Room SQLite 本地存储，WebView 内嵌 douyin.com，DeepSeek API AI 出题。

**技术栈：** Kotlin, Jetpack Compose, Room, OkHttp, Navigation Compose, DataStore

---

## 环境确认

在执行前，确保已安装：

- Android Studio（Ladybug+）
- JDK 17+
- Android SDK（API 24-34）
- Gradle 8.x

当前环境 **未检测到这些工具**，需先安装。

---

## Phase 0：项目搭建与环境配置

### Task 0.1：安装 Android Studio 及 SDK

- [ ] **Step 1: 下载安装 Android Studio**

从 https://developer.android.com/studio 下载 Ladybug 版本并安装。

- [ ] **Step 2: 安装 SDK**

启动 Android Studio → SDK Manager → 安装：
- Android SDK Platform 34
- Android SDK Build-Tools 34
- Android Emulator（可选）

- [ ] **Step 3: 确认环境变量**

```
JAVA_HOME = C:\Program Files\Android\Android Studio\jbr
ANDROID_HOME = C:\Users\Administrator\AppData\Local\Android\Sdk
```

### Task 0.2：创建 Android 项目

- [ ] **Step 1: 用 Android Studio 创建新项目**

- 模板：Empty Views Activity（或 Compose Empty Activity）
- 包名：`com.yunfan.douyincontrol`
- 语言：Kotlin
- Minimum SDK：API 24
- 项目路径：`D:\xiangmu\抖音管控App`

- [ ] **Step 2: 配置 build.gradle.kts（项目级）**

```kotlin
// D:\xiangmu\抖音管控App\build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
```

- [ ] **Step 3: 配置 build.gradle.kts（模块级）**

```kotlin
// D:\xiangmu\抖音管控App\app\build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.yunfan.douyincontrol"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yunfan.douyincontrol"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
```

- [ ] **Step 4: 配置 settings.gradle.kts**

```kotlin
// D:\xiangmu\抖音管控App\settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "抖音管控App"
include(":app")
```

- [ ] **Step 5: 配置 gradle.properties**

```
# D:\xiangmu\抖音管控App\gradle.properties
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m
```

- [ ] **Step 6: 同步 Gradle**

在 Android Studio 中点击 "Sync Now"，确认项目编译通过。

---

## Phase 1：数据库层 & 数据模型

### Task 1.1：创建 Entity 类

- **文件路径：** `app/src/main/java/com/yunfan/douyincontrol/data/database/entity/`

- [ ] **Step 1: 创建 QuestionEntity.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/entity/QuestionEntity.kt
package com.yunfan.douyincontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,      // math / chinese
    val grade: String,        // bigclass / grade1 ~ grade6
    val question: String,     // 题目内容，用于去重
    val options: String,      // JSON 数组 ["A", "B", "C", "D"]
    val answer: String,       // 正确答案
    val deleted: Int = 0,     // 0=正常, 1=软删除
    val source: String = "ai", // seed / ai / manual
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: 创建 RuleEntity.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/entity/RuleEntity.kt
package com.yunfan.douyincontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleKey: String,      // score_per_question, cost_per_minute, etc.
    val ruleName: String,
    val value: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: 创建 PointsLogEntity.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/entity/PointsLogEntity.kt
package com.yunfan.douyincontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "points_log")
data class PointsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,          // earn / spend
    val amount: Int,
    val source: String,        // question_correct / question_wrong_redo / video_cost / parent_adjust
    val sourceDetail: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: 创建 StudyLogEntity.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/entity/StudyLogEntity.kt
package com.yunfan.douyincontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,            // question / video
    val subject: String? = null, // math / chinese（做题时）
    val questionId: Long? = null,
    val result: String,          // correct / wrong / rework_correct
    val detail: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Task 1.2：创建 DAO 接口

- **文件路径：** `app/src/main/java/com/yunfan/douyincontrol/data/database/dao/`

- [ ] **Step 1: 创建 QuestionDao.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/dao/QuestionDao.kt
package com.yunfan.douyincontrol.data.database.dao

import androidx.room.*
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE deleted = 0 AND subject = :subject AND grade = :grade ORDER BY RANDOM()")
    fun getQuestionsBySubjectAndGrade(subject: String, grade: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE deleted = 0 AND subject = :subject AND grade = :grade LIMIT :limit")
    suspend fun getQuestionsBySubjectAndGradeLimit(subject: String, grade: String, limit: Int): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE deleted = 0 AND subject = :subject AND grade = :grade")
    suspend fun countBySubjectAndGrade(subject: String, grade: String): Int

    @Query("SELECT * FROM questions WHERE question = :questionText LIMIT 1")
    suspend fun findByQuestionText(questionText: String): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(questions: List<QuestionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(question: QuestionEntity): Long

    @Query("UPDATE questions SET deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM questions WHERE deleted = 0 AND subject = :subject ORDER BY id DESC")
    fun getAllBySubject(subject: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE deleted = 0 ORDER BY id DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>
}
```

- [ ] **Step 2: 创建 RuleDao.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/dao/RuleDao.kt
package com.yunfan.douyincontrol.data.database.dao

import androidx.room.*
import com.yunfan.douyincontrol.data.database.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules WHERE ruleKey = :key LIMIT 1")
    suspend fun getByKey(key: String): RuleEntity?

    @Query("SELECT * FROM rules")
    fun getAll(): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RuleEntity)

    @Query("UPDATE rules SET value = :value, updatedAt = :updatedAt WHERE ruleKey = :key")
    suspend fun updateValue(key: String, value: Int, updatedAt: Long = System.currentTimeMillis())
}
```

- [ ] **Step 3: 创建 PointsLogDao.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/dao/PointsLogDao.kt
package com.yunfan.douyincontrol.data.database.dao

import androidx.room.*
import com.yunfan.douyincontrol.data.database.entity.PointsLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PointsLogDao {
    @Insert
    suspend fun insert(log: PointsLogEntity)

    @Query("SELECT * FROM points_log ORDER BY id DESC")
    fun getAll(): Flow<List<PointsLogEntity>>

    @Query("SELECT * FROM points_log ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<PointsLogEntity>

    @Query("SELECT COUNT(*) FROM points_log")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'earn' THEN amount ELSE 0 END), 0) - COALESCE(SUM(CASE WHEN type = 'spend' THEN amount ELSE 0 END), 0) FROM points_log")
    suspend fun getBalance(): Int

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'earn' THEN amount ELSE 0 END), 0) FROM points_log WHERE createdAt >= :since")
    suspend fun getEarnedToday(since: Long): Int
}
```

- [ ] **Step 4: 创建 StudyLogDao.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/dao/StudyLogDao.kt
package com.yunfan.douyincontrol.data.database.dao

import androidx.room.*
import com.yunfan.douyincontrol.data.database.entity.StudyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Insert
    suspend fun insert(log: StudyLogEntity)

    @Query("SELECT * FROM study_logs ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<StudyLogEntity>

    @Query("SELECT COUNT(*) FROM study_logs")
    suspend fun count(): Int

    // 错题查询（未重新做对的错题）
    @Query("""
        SELECT q.* FROM questions q 
        INNER JOIN study_logs s ON q.id = s.questionId 
        WHERE s.result = 'wrong' AND q.id NOT IN (
            SELECT questionId FROM study_logs WHERE result = 'rework_correct' AND questionId IS NOT NULL
        ) AND q.deleted = 0 AND q.subject = :subject
        GROUP BY q.id ORDER BY s.id DESC
    """)
    fun getWrongQuestionsBySubject(subject: String): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM study_logs WHERE type = 'question' AND createdAt >= :since")
    suspend fun getQuestionCountToday(since: Long): Int

    @Query("SELECT COUNT(*) FROM study_logs WHERE type = 'question' AND result = 'correct' AND createdAt >= :since")
    suspend fun getCorrectCountToday(since: Long): Int

    // 检查某道题是否在错题本中（有 wrong 记录但无 rework_correct 记录）
    @Query("""
        SELECT COUNT(*) FROM study_logs 
        WHERE questionId = :questionId AND result = 'wrong' 
        AND questionId NOT IN (SELECT questionId FROM study_logs WHERE result = 'rework_correct' AND questionId = :questionId)
    """)
    suspend fun isInWrongBook(questionId: Long): Int
}
```

### Task 1.3：创建 Room Database

- [ ] **Step 1: 创建 AppDatabase.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/database/AppDatabase.kt
package com.yunfan.douyincontrol.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yunfan.douyincontrol.data.database.dao.*
import com.yunfan.douyincontrol.data.database.entity.*

@Database(
    entities = [
        QuestionEntity::class,
        RuleEntity::class,
        PointsLogEntity::class,
        StudyLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun ruleDao(): RuleDao
    abstract fun pointsLogDao(): PointsLogDao
    abstract fun studyLogDao(): StudyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "douyin_control.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### Task 1.4：创建 Repository 层

- **文件路径：** `app/src/main/java/com/yunfan/douyincontrol/data/repository/`

- [ ] **Step 1: 创建 QuestionRepository.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/repository/QuestionRepository.kt
package com.yunfan.douyincontrol.data.repository

import com.yunfan.douyincontrol.data.database.dao.QuestionDao
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

class QuestionRepository(private val questionDao: QuestionDao) {

    fun getQuestionsBySubject(subject: String): Flow<List<QuestionEntity>> =
        questionDao.getAllBySubject(subject)

    fun getAllQuestions(): Flow<List<QuestionEntity>> =
        questionDao.getAllQuestions()

    suspend fun getQuestionsForStudy(subject: String, grade: String, limit: Int = 20): List<QuestionEntity> =
        questionDao.getQuestionsBySubjectAndGradeLimit(subject, grade, limit)

    suspend fun countBySubjectAndGrade(subject: String, grade: String): Int =
        questionDao.countBySubjectAndGrade(subject, grade)

    suspend fun findByQuestionText(text: String): QuestionEntity? =
        questionDao.findByQuestionText(text)

    suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long> =
        questionDao.insertAll(questions)

    suspend fun insertQuestion(question: QuestionEntity): Long =
        questionDao.insert(question)

    suspend fun softDelete(id: Long) =
        questionDao.softDelete(id)
}
```

- [ ] **Step 2: 创建 PointsRepository.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/repository/PointsRepository.kt
package com.yunfan.douyincontrol.data.repository

import com.yunfan.douyincontrol.data.database.dao.PointsLogDao
import com.yunfan.douyincontrol.data.database.dao.RuleDao
import com.yunfan.douyincontrol.data.database.entity.PointsLogEntity
import com.yunfan.douyincontrol.data.database.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

class PointsRepository(
    private val pointsLogDao: PointsLogDao,
    private val ruleDao: RuleDao
) {
    suspend fun getBalance(): Int = pointsLogDao.getBalance()

    suspend fun getEarnedToday(since: Long): Int = pointsLogDao.getEarnedToday(since)

    suspend fun addPoints(amount: Int, source: String, detail: String) {
        pointsLogDao.insert(
            PointsLogEntity(type = "earn", amount = amount, source = source, sourceDetail = detail)
        )
    }

    suspend fun spendPoints(amount: Int, source: String, detail: String) {
        pointsLogDao.insert(
            PointsLogEntity(type = "spend", amount = amount, source = source, sourceDetail = detail)
        )
    }

    fun getAllLogs(): Flow<List<PointsLogEntity>> = pointsLogDao.getAll()

    suspend fun getLogsPage(limit: Int, offset: Int): List<PointsLogEntity> =
        pointsLogDao.getPage(limit, offset)

    suspend fun getLogsCount(): Int = pointsLogDao.count()

    // 积分规则
    suspend fun getRule(key: String): RuleEntity? = ruleDao.getByKey(key)

    fun getAllRules(): Flow<List<RuleEntity>> = ruleDao.getAll()

    suspend fun updateRule(key: String, value: Int) = ruleDao.updateValue(key, value)
}
```

- [ ] **Step 3: 创建 StudyRepository.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/repository/StudyRepository.kt
package com.yunfan.douyincontrol.data.repository

import com.yunfan.douyincontrol.data.database.dao.StudyLogDao
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.database.entity.StudyLogEntity
import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyLogDao: StudyLogDao) {

    suspend fun logQuestion(questionId: Long, subject: String, result: String) {
        studyLogDao.insert(
            StudyLogEntity(type = "question", subject = subject, questionId = questionId, result = result)
        )
    }

    suspend fun logVideo(durationMinutes: Int) {
        studyLogDao.insert(
            StudyLogEntity(type = "video", result = "watched", detail = durationMinutes.toString())
        )
    }

    fun getWrongQuestionsBySubject(subject: String): Flow<List<QuestionEntity>> =
        studyLogDao.getWrongQuestionsBySubject(subject)

    suspend fun getQuestionCountToday(since: Long): Int =
        studyLogDao.getQuestionCountToday(since)

    suspend fun getCorrectCountToday(since: Long): Int =
        studyLogDao.getCorrectCountToday(since)

    suspend fun isInWrongBook(questionId: Long): Boolean =
        studyLogDao.isInWrongBook(questionId) > 0
}
```

### Task 1.5：创建 Application 类并初始化数据库

- [ ] **Step 1: 创建 App.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/App.kt
package com.yunfan.douyincontrol

import android.app.Application
import com.yunfan.douyincontrol.data.database.AppDatabase
import com.yunfan.douyincontrol.data.database.entity.RuleEntity
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.QuestionRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val questionRepository by lazy { QuestionRepository(database.questionDao()) }
    val pointsRepository by lazy { PointsRepository(database.pointsLogDao(), database.ruleDao()) }
    val studyRepository by lazy { StudyRepository(database.studyLogDao()) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedRules()
        }
    }

    private suspend fun seedRules() {
        val rules = listOf(
            RuleEntity(ruleKey = "score_per_question", ruleName = "做对一题得分", value = 10),
            RuleEntity(ruleKey = "score_per_rework", ruleName = "错题重做对得分", value = 10),
            RuleEntity(ruleKey = "cost_per_minute", ruleName = "看抖音每分钟扣分", value = 10),
            RuleEntity(ruleKey = "min_cost_threshold", ruleName = "看抖音最低积分阈值", value = 10),
        )
        rules.forEach { rule ->
            if (database.ruleDao().getByKey(rule.ruleKey) == null) {
                database.ruleDao().upsert(rule)
            }
        }
    }
}
```

- [ ] **Step 2: 注册 Application 到 AndroidManifest.xml**

```xml
<!-- app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".App"
        android:allowBackup="true"
        android:label="云帆乐园"
        android:supportsRtl="true"
        android:theme="@style/Theme.DouyinControl"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.DouyinControl">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## Phase 2：主题、导航 & 工具类

### Task 2.1：创建主题和配色

- [ ] **Step 1: 创建 Theme.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/theme/Theme.kt
package com.yunfan.douyincontrol.ui.theme

import androidx.compose.ui.graphics.Color

// 蓝紫渐变配色（云帆乐园风格）
val PurpleStart = Color(0xFF667EEA)
val PurpleEnd = Color(0xFF764BA2)
val LightPurple = Color(0xFFE8E0F0)
val CardBackground = Color(0xFFFFF5F5)
val CorrectGreen = Color(0xFF4CAF50)
val WrongRed = Color(0xFFE53935)
val Gold = Color(0xFFFFD700)
val BackgroundLight = Color(0xFFF5F3FF)
val TextPrimary = Color(0xFF2D3436)
val TextSecondary = Color(0xFF636E72)
```

- [ ] **Step 2: 修改 colors.xml 和 themes.xml**

```xml
<!-- app/src/main/res/values/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_start">#667EEA</color>
    <color name="purple_end">#764BA2</color>
    <color name="white">#FFFFFF</color>
</resources>
```

```xml
<!-- app/src/main/res/values/themes.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.DouyinControl" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">@color/purple_start</item>
        <item name="android:navigationBarColor">@color/purple_start</item>
    </style>
</resources>
```

### Task 2.2：创建工具类

- [ ] **Step 1: 创建 PasswordUtil.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/util/PasswordUtil.kt
package com.yunfan.douyincontrol.util

import java.security.MessageDigest

object PasswordUtil {
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(input: String, storedHash: String): Boolean {
        return hash(input) == storedHash
    }
}
```

- [ ] **Step 2: 创建 SimilarityUtil.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/util/SimilarityUtil.kt
package com.yunfan.douyincontrol.util

object SimilarityUtil {
    // Levenshtein 距离计算
    fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    // 标准化相似度（0~1），值越小越相似
    fun isSimilar(a: String, b: String, threshold: Int = 3): Boolean {
        // 清理：去空格、去标点
        val cleanA = a.replace(Regex("[\\s,，。？！、；：]"), "")
        val cleanB = b.replace(Regex("[\\s,，。？！、；：]"), "")
        return levenshteinDistance(cleanA, cleanB) <= threshold
    }
}
```

### Task 2.3：创建导航图

- [ ] **Step 1: 创建 Screen 路由定义**

```kotlin
// 在 NavGraph.kt 中，或者单独的文件
// app/src/main/java/com/yunfan/douyincontrol/ui/navigation/Screen.kt
package com.yunfan.douyincontrol.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Study : Screen("study/{subject}") {
        fun createRoute(subject: String) = "study/$subject"
    }
    object Douyin : Screen("douyin")
    object WrongBook : Screen("wrong_book")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object Rules : Screen("settings/rules")
    object QuestionManage : Screen("settings/questions")
    object Grade : Screen("settings/grade")
    object Data : Screen("settings/data")
    object ChangePassword : Screen("settings/password")
}
```

- [ ] **Step 2: 创建 NavGraph.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/navigation/NavGraph.kt
package com.yunfan.douyincontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.screen.*

@Composable
fun NavGraph(navController: NavHostController, app: App) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(navController = navController, app = app)
        }

        composable(
            route = Screen.Study.route,
            arguments = listOf(navArgument("subject") { type = NavType.StringType })
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject") ?: "math"
            StudyScreen(navController = navController, app = app, subject = subject)
        }

        composable(Screen.Douyin.route) {
            DouyinScreen(navController = navController, app = app)
        }

        composable(Screen.WrongBook.route) {
            WrongBookScreen(navController = navController, app = app)
        }

        composable(Screen.Stats.route) {
            StatsScreen(navController = navController, app = app)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController, app = app)
        }

        composable(Screen.Rules.route) {
            RulesScreen(navController = navController, app = app)
        }

        composable(Screen.QuestionManage.route) {
            QuestionManageScreen(navController = navController, app = app)
        }

        composable(Screen.Grade.route) {
            GradeScreen(navController = navController, app = app)
        }

        composable(Screen.Data.route) {
            DataScreen(navController = navController, app = app)
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(navController = navController, app = app)
        }
    }
}
```

### Task 2.4：创建主 Activity

- [ ] **Step 1: 创建 MainActivity.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/MainActivity.kt
package com.yunfan.douyincontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yunfan.douyincontrol.ui.navigation.NavGraph
import com.yunfan.douyincontrol.ui.theme.BackgroundLight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundLight
            ) {
                val navController = rememberNavController()
                NavGraph(navController = navController, app = app)
            }
        }
    }
}
```

---

## Phase 3：通用 UI 组件

### Task 3.1：创建可复用组件

- **文件路径：** `app/src/main/java/com/yunfan/douyincontrol/ui/component/`

- [ ] **Step 1: 创建 PaginationBar.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/component/PaginationBar.kt
package com.yunfan.douyincontrol.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunfan.douyincontrol.ui.theme.PurpleStart

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    pageSize: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "显示 ${currentPage * pageSize + 1}-${minOf((currentPage + 1) * pageSize, totalItems)} / 共 $totalItems 条",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = { if (currentPage > 0) onPageChange(currentPage - 1) },
                enabled = currentPage > 0
            ) {
                Text("← 上一页")
            }

            val startPage = maxOf(0, currentPage - 2)
            val endPage = minOf(totalPages - 1, startPage + 4)
            for (i in startPage..endPage) {
                if (i == currentPage) {
                    TextButton(onClick = {}) {
                        Text(
                            "${i + 1}",
                            fontWeight = FontWeight.Bold,
                            color = PurpleStart
                        )
                    }
                } else {
                    TextButton(onClick = { onPageChange(i) }) {
                        Text("${i + 1}")
                    }
                }
            }

            TextButton(
                onClick = { if (currentPage < totalPages - 1) onPageChange(currentPage + 1) },
                enabled = currentPage < totalPages - 1
            ) {
                Text("下一页 →")
            }
        }
    }
}
```

- [ ] **Step 2: 创建 QuestionCard.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/component/QuestionCard.kt
package com.yunfan.douyincontrol.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunfan.douyincontrol.ui.theme.*

@Composable
fun QuestionCard(
    question: String,
    options: List<String>,
    selectedAnswer: String?,
    correctAnswer: String?,
    showResult: Boolean,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 题目
            Text(
                text = question,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 选项
            options.forEach { option ->
                val isSelected = selectedAnswer == option
                val isCorrect = correctAnswer == option
                val bgColor = when {
                    showResult && isCorrect -> CorrectGreen.copy(alpha = 0.15f)
                    showResult && isSelected && !isCorrect -> WrongRed.copy(alpha = 0.15f)
                    isSelected -> PurpleStart.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = when {
                    showResult && isCorrect -> CorrectGreen
                    showResult && isSelected && !isCorrect -> WrongRed
                    isSelected -> PurpleStart
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }

                OutlinedButton(
                    onClick = { if (!showResult) onOptionSelected(option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(borderColor)
                    )
                ) {
                    Text(
                        text = option,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
```

- [ ] **Step 3: 创建 TimerView.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/component/TimerView.kt
package com.yunfan.douyincontrol.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunfan.douyincontrol.ui.theme.Gold
import com.yunfan.douyincontrol.ui.theme.PurpleStart
import com.yunfan.douyincontrol.ui.theme.TextPrimary

@Composable
fun TimerView(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⏱️ ", fontSize = 20.sp)
            Text(
                text = "剩余 %02d:%02d".format(minutes, seconds),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (remainingSeconds < 60) MaterialTheme.colorScheme.error else TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = Gold,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
```

---

## Phase 4：首页

### Task 4.1：创建首页 ViewModel

- [ ] **Step 1: 创建 HomeViewModel.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/HomeViewModel.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import com.yunfan.douyincontrol.util.PasswordUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(
    private val pointsRepository: PointsRepository,
    private val studyRepository: StudyRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    // 当前积分
    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    // 今日赚积分
    private val _todayEarned = MutableStateFlow(0)
    val todayEarned: StateFlow<Int> = _todayEarned.asStateFlow()

    // 家长密码（哈希值）
    private val _parentPasswordHash = MutableStateFlow("")
    val parentPasswordHash: StateFlow<String> = _parentPasswordHash.asStateFlow()

    // 当前年级
    private val _currentGrade = MutableStateFlow("bigclass")
    val currentGrade: StateFlow<String> = _currentGrade.asStateFlow()

    // 密码错误次数
    private val _passwordErrorCount = MutableStateFlow(0)
    val passwordErrorCount: StateFlow<Int> = _passwordErrorCount.asStateFlow()

    // 锁定直到时间戳
    private val _lockedUntil = MutableStateFlow(0L)
    val lockedUntil: StateFlow<Long> = _lockedUntil.asStateFlow()

    companion object {
        private val PASSWORD_KEY = stringPreferencesKey("parent_password_hash")
        private val GRADE_KEY = stringPreferencesKey("current_grade")
        private val ERROR_COUNT_KEY = intPreferencesKey("password_error_count")
        private val LOCKED_UNTIL_KEY = longPreferencesKey("locked_until")
    }

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _parentPasswordHash.value = prefs[PASSWORD_KEY] ?: PasswordUtil.hash("1234")
                _currentGrade.value = prefs[GRADE_KEY] ?: "grade1"
                _passwordErrorCount.value = prefs[ERROR_COUNT_KEY] ?: 0
                _lockedUntil.value = prefs[LOCKED_UNTIL_KEY] ?: 0L
            }
        }
        refreshBalance()
    }

    fun refreshBalance() {
        viewModelScope.launch {
            _balance.value = pointsRepository.getBalance()
            val todayStart = getTodayStart()
            _todayEarned.value = pointsRepository.getEarnedToday(todayStart)
        }
    }

    fun verifyPassword(input: String): Boolean {
        val now = System.currentTimeMillis()
        if (now < _lockedUntil.value) return false

        val isValid = PasswordUtil.verify(input, _parentPasswordHash.value)
        if (!isValid) {
            val newCount = _passwordErrorCount.value + 1
            viewModelScope.launch {
                dataStore.edit { prefs ->
                    prefs[ERROR_COUNT_KEY] = newCount
                    if (newCount >= 3) {
                        prefs[LOCKED_UNTIL_KEY] = now + 30_000 // 锁定30秒
                    }
                }
            }
        } else {
            viewModelScope.launch {
                dataStore.edit { prefs ->
                    prefs[ERROR_COUNT_KEY] = 0
                    prefs[LOCKED_UNTIL_KEY] = 0L
                }
            }
        }
        return isValid
    }

    private fun getTodayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
```

### Task 4.2：创建首页界面

- [ ] **Step 1: 创建 HomeScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/HomeScreen.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.navigation.Screen
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, app: App) {
    val viewModel = remember {
        HomeViewModel(app.pointsRepository, app.studyRepository, app.dataStore)
    }
    val balance by viewModel.balance.collectAsState()
    val todayEarned by viewModel.todayEarned.collectAsState()

    // 家长入口长按检测
    var isLongPressing by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var longPressProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
    }

    // 长按进度
    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            longPressProgress = 0f
            while (longPressProgress < 1f) {
                delay(100)
                longPressProgress += 0.02f
            }
            showPasswordDialog = true
            isLongPressing = false
            longPressProgress = 0f
        } else {
            longPressProgress = 0f
        }
    }

    // 密码弹窗
    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }
        val lockedUntil by viewModel.lockedUntil.collectAsState()

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; password = "" },
            title = { Text("家长验证", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (System.currentTimeMillis() < lockedUntil) {
                        Text("密码错误次数过多，请等待30秒", color = WrongRed)
                    } else {
                        TextField(
                            value = password,
                            onValueChange = {
                                if (it.length <= 4) {
                                    password = it
                                    errorMsg = ""
                                }
                            },
                            label = { Text("请输入4位数字密码") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = WrongRed, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (System.currentTimeMillis() < lockedUntil) {
                    TextButton(onClick = {}) { Text("等待中...") }
                } else {
                    TextButton(onClick = {
                        if (viewModel.verifyPassword(password)) {
                            showPasswordDialog = false
                            password = ""
                            navController.navigate(Screen.Settings.route)
                        } else {
                            errorMsg = "密码错误"
                        }
                    }) { Text("确认") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; password = "" }) { Text("取消") }
            }
        )
    }

    // 主界面
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PurpleStart, PurpleEnd)
                )
            )
    ) {
        // 顶部积分区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🧸",
                fontSize = 48.sp
            )
            Text(
                text = "欢迎回来，宝贝！",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐ 积分：$balance", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold)
                }
            }
        }

        // 功能网格
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundLight)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // 第一行：去做题 + 看抖音
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeButton(
                        emoji = "📚",
                        label = "去做题",
                        onClick = { navController.navigate(Screen.Study.createRoute("math")) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeButton(
                        emoji = "📱",
                        label = "看抖音",
                        onClick = { navController.navigate(Screen.Douyin.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 第二行：错题本 + 学习统计
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeButton(
                        emoji = "❌",
                        label = "错题本",
                        onClick = { navController.navigate(Screen.WrongBook.route) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeButton(
                        emoji = "🏆",
                        label = "学习统计",
                        onClick = { navController.navigate(Screen.Stats.route) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 底部长按提示
                Text(
                    text = "[ 长按进入家长设置 ]",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { isLongPressing = true },
                                onPress = {
                                    try { awaitRelease() } finally { isLongPressing = false }
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
fun HomeButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}
```

---

## Phase 5：做题系统

### Task 5.1：创建出题引擎

- [ ] **Step 1: 创建 QuizEngine.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/engine/QuizEngine.kt
package com.yunfan.douyincontrol.engine

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.repository.QuestionRepository
import com.yunfan.douyincontrol.util.SimilarityUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizEngine(private val questionRepository: QuestionRepository) {

    companion object {
        private const val MIN_QUESTIONS_THRESHOLD = 20
    }

    suspend fun getQuestionsForStudy(subject: String, grade: String, count: Int = 10): List<QuestionEntity> {
        val questions = questionRepository.getQuestionsForStudy(subject, grade, count)
        val aiCount = questions.size
        if (aiCount < count) {
            // 题目不够，需要触发AI补充
            // 这里不直接调AI，而是返回现有题目，由上层决定是否触发AI
        }
        return questions.shuffled().take(count)
    }

    suspend fun deduplicateAndSave(
        newQuestions: List<QuestionEntity>
    ): List<QuestionEntity> {
        val saved = mutableListOf<QuestionEntity>()
        for (q in newQuestions) {
            val existing = questionRepository.findByQuestionText(q.question)
            if (existing != null) continue

            // 相似度检查
            val allExisting = questionRepository.getQuestionsForStudy(q.subject, q.grade, 1000)
            val isDuplicate = allExisting.any { SimilarityUtil.isSimilar(it.question, q.question) }
            if (isDuplicate) continue

            val id = questionRepository.insertQuestion(q)
            if (id > 0) {
                saved.add(q.copy(id = id))
            }
        }
        return saved
    }
}
```

### Task 5.2：创建做题 ViewModel

- [ ] **Step 1: 创建 StudyViewModel.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/StudyViewModel.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.QuestionRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import com.yunfan.douyincontrol.engine.QuizEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel(
    private val questionRepository: QuestionRepository,
    private val pointsRepository: PointsRepository,
    private val studyRepository: StudyRepository,
    private val quizEngine: QuizEngine
) : ViewModel() {

    private val _currentQuestion = MutableStateFlow<QuestionEntity?>(null)
    val currentQuestion: StateFlow<QuestionEntity?> = _currentQuestion.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _totalQuestions = MutableStateFlow(0)
    val totalQuestions: StateFlow<Int> = _totalQuestions.asStateFlow()

    private val _selectedAnswer = MutableStateFlow<String?>(null)
    val selectedAnswer: StateFlow<String?> = _selectedAnswer.asStateFlow()

    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult.asStateFlow()

    private val _isCorrect = MutableStateFlow<Boolean?>(null)
    val isCorrect: StateFlow<Boolean?> = _isCorrect.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _earningThisSession = MutableStateFlow(0)
    val earningThisSession: StateFlow<Int> = _earningThisSession.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private var questions: List<QuestionEntity> = emptyList()

    fun startStudy(subject: String, grade: String, questionsList: List<QuestionEntity>? = null) {
        viewModelScope.launch {
            questions = questionsList ?: quizEngine.getQuestionsForStudy(subject, grade)
            if (questions.isEmpty()) {
                _message.value = "题库暂时没有题目，请先在家长设置中生成题目"
                _finished.value = true
                return@launch
            }
            _totalQuestions.value = questions.size
            _currentIndex.value = 0
            _score.value = 0
            _earningThisSession.value = 0
            _finished.value = false
            _selectedAnswer.value = null
            _showResult.value = false
            _isCorrect.value = null
            _message.value = ""
            showQuestion(0)
        }
    }

    private fun showQuestion(index: Int) {
        if (index >= questions.size) {
            _finished.value = true
            _message.value = "全部完成！共获得 $_earningThisSession 积分"
            return
        }
        _currentQuestion.value = questions[index]
        _currentIndex.value = index
        _selectedAnswer.value = null
        _showResult.value = false
        _isCorrect.value = null
    }

    fun selectAnswer(answer: String) {
        if (_showResult.value) return
        val question = _currentQuestion.value ?: return

        _selectedAnswer.value = answer
        _showResult.value = true

        viewModelScope.launch {
            val correct = answer == question.answer
            _isCorrect.value = correct

            if (correct) {
                val rule = pointsRepository.getRule("score_per_question")
                val points = rule?.value ?: 10

                // 检查是否在错题库中
                val inWrongBook = studyRepository.isInWrongBook(question.id)
                val pointsToAdd = if (inWrongBook) {
                    val reworkRule = pointsRepository.getRule("score_per_rework")
                    pointsRepository.addPoints(reworkRule?.value ?: 10, "question_wrong_redo", "错题重做对：${question.question}")
                    studyRepository.logQuestion(question.id, question.subject, "rework_correct")
                    reworkRule?.value ?: 10
                } else {
                    pointsRepository.addPoints(points, "question_correct", "做对${if (question.subject == "math") "数学" else "语文"}题：${question.question}")
                    studyRepository.logQuestion(question.id, question.subject, "correct")
                    points
                }
                _earningThisSession.value += pointsToAdd
                _score.value += 1
            } else {
                studyRepository.logQuestion(question.id, question.subject, "wrong")
            }
        }
    }

    fun nextQuestion() {
        showQuestion(_currentIndex.value + 1)
    }

    fun reset() {
        _finished.value = false
        _currentIndex.value = 0
        questions = emptyList()
    }
}
```

### Task 5.3：创建做题界面

- [ ] **Step 1: 创建 StudyScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/StudyScreen.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.engine.QuizEngine
import com.yunfan.douyincontrol.ui.component.QuestionCard
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(navController: NavController, app: App, subject: String) {
    val viewModel = remember {
        StudyViewModel(app.questionRepository, app.pointsRepository, app.studyRepository, QuizEngine(app.questionRepository))
    }
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val totalQuestions by viewModel.totalQuestions.collectAsState()
    val selectedAnswer by viewModel.selectedAnswer.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val score by viewModel.score.collectAsState()
    val earning by viewModel.earningThisSession.collectAsState()
    val finished by viewModel.finished.collectAsState()
    val message by viewModel.message.collectAsState()

    val subjectName = if (subject == "math") "数学" else "语文"
    val gson = remember { Gson() }
    val coroutineScope = rememberCoroutineScope()

    // 自动下一题
    LaunchedEffect(showResult) {
        if (showResult) {
            delay(1500)
            viewModel.nextQuestion()
        }
    }

    // 初始化
    LaunchedEffect(subject) {
        val appViewModel = androidx.lifecycle.viewmodel.compose.viewModel<HomeViewModel>()
        // 简化处理：从App获取当前年级
        viewModel.startStudy(subject, "grade1")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${subjectName}挑战") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 退出", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            if (finished) {
                // 完成界面
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("全部完成！", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "答对 $score / $totalQuestions 题",
                        fontSize = 20.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "获得 $earning 积分",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("返回首页", fontSize = 18.sp)
                    }
                }
            } else if (currentQuestion != null) {
                val question = currentQuestion!!
                val options = try {
                    gson.fromJson(question.options, object : TypeToken<List<String>>() {}.type) as? List<String>
                        ?: listOf("A", "B", "C", "D")
                } catch (e: Exception) {
                    listOf("A", "B", "C", "D")
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 进度
                    Text(
                        "第 ${currentIndex + 1}/$totalQuestions 题",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / totalQuestions },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = PurpleStart,
                        trackColor = Color.LightGray,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // 题目卡片
                    QuestionCard(
                        question = question.question,
                        options = options,
                        selectedAnswer = selectedAnswer,
                        correctAnswer = if (showResult) question.answer else null,
                        showResult = showResult,
                        onOptionSelected = { viewModel.selectAnswer(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 答题结果反馈
                    if (showResult) {
                        val (emoji, msg, color) = if (isCorrect == true) {
                            Triple("🎉", "答对了！+${if (/* 错题重做 */ false) "错题加分" else "积分"}", CorrectGreen)
                        } else {
                            Triple("😊", "正确答案：${question.answer}", WrongRed)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.background(
                                color.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ).padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("$emoji  $msg", fontSize = 18.sp, color = color, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 本次获得积分
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.2f))
                    ) {
                        Text(
                            "本局获得 $earning 积分",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                    }
                }
            } else {
                // 加载中
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("加载题目中...", fontSize = 18.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
```

---

## Phase 6：AI 出题与去重

### Task 6.1：创建 AI API 层

- [ ] **Step 1: 创建 AiService.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/api/AiService.kt
package com.yunfan.douyincontrol.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class AiRequest(
    val model: String = "deepseek-chat",
    val messages: List<AiMessage>,
    val temperature: Double = 0.8
)

data class AiMessage(
    val role: String,
    val content: String
)

data class AiResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: AiMessage?
)

data class AiQuestion(
    val question: String,
    val options: List<String>,
    val answer: String
)

class AiService(
    private val apiUrl: String = "https://api.your-proxy.com/v1/chat/completions",
    private val apiKey: String = ""
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateQuestions(
        subject: String,
        grade: String,
        count: Int = 5
    ): List<AiQuestion> = withContext(Dispatchers.IO) {
        val gradeNames = mapOf(
            "bigclass" to "幼儿园大班",
            "grade1" to "小学一年级",
            "grade2" to "小学二年级",
            "grade3" to "小学三年级",
            "grade4" to "小学四年级",
            "grade5" to "小学五年级",
            "grade6" to "小学六年级"
        )
        val subjectNames = mapOf("math" to "数学", "chinese" to "语文")
        val gradeName = gradeNames[grade] ?: "小学一年级"
        val subjectName = subjectNames[subject] ?: "数学"

        val prompt = """
你是一位小学教育专家，请为$gradeName学生出$count道${subjectName}选择题。
每道题4个选项，仅一个正确答案，选项用A、B、C、D标记。
以JSON格式返回，不要返回其他内容：
[
  {
    "question": "题目内容",
    "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
    "answer": "A. 选项1"
  }
]
        """.trimIndent()

        val requestBody = AiRequest(
            messages = listOf(
                AiMessage(role = "user", content = prompt)
            )
        )

        val jsonBody = gson.toJson(requestBody)
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")

        val aiResponse = gson.fromJson(responseBody, AiResponse::class.java)
        val content = aiResponse.choices?.firstOrNull()?.message?.content
            ?: throw IOException("No content in response")

        // 提取 JSON（可能被 markdown 包裹）
        val jsonStr = content
            .replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^```\\s*", RegexOption.MULTILINE), "")
            .trim()

        val type = object : TypeToken<List<AiQuestion>>() {}.type
        gson.fromJson(jsonStr, type) ?: emptyList()
    }
}
```

- [ ] **Step 2: 创建 AiRepository.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/api/AiRepository.kt
package com.yunfan.douyincontrol.api

import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.engine.QuizEngine

class AiRepository(
    private val aiService: AiService,
    private val quizEngine: QuizEngine
) {
    suspend fun generateAndSaveQuestions(
        subject: String,
        grade: String,
        count: Int = 5
    ): List<QuestionEntity> {
        val aiQuestions = aiService.generateQuestions(subject, grade, count)
        val entities = aiQuestions.map { aiq ->
            QuestionEntity(
                subject = subject,
                grade = grade,
                question = aiq.question,
                options = com.google.gson.Gson().toJson(aiq.options),
                answer = aiq.answer,
                source = "ai"
            )
        }
        return quizEngine.deduplicateAndSave(entities)
    }
}
```

---

## Phase 7：错题本

### Task 7.1：创建错题本 ViewModel

- [ ] **Step 1: 创建 WrongBookViewModel.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/WrongBookViewModel.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WrongBookViewModel(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _selectedSubject = MutableStateFlow("math")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _wrongQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val wrongQuestions: StateFlow<List<QuestionEntity>> = _wrongQuestions.asStateFlow()

    init {
        loadWrongQuestions()
    }

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
        loadWrongQuestions()
    }

    private fun loadWrongQuestions() {
        viewModelScope.launch {
            studyRepository.getWrongQuestionsBySubject(_selectedSubject.value)
                .collect { questions ->
                    _wrongQuestions.value = questions
                }
        }
    }
}
```

### Task 7.2：创建错题本界面

- [ ] **Step 1: 创建 WrongBookScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/WrongBookScreen.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.component.PaginationBar
import com.yunfan.douyincontrol.ui.navigation.Screen
import com.yunfan.douyincontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongBookScreen(navController: NavController, app: App) {
    val viewModel = remember { WrongBookViewModel(app.studyRepository) }
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val wrongQuestions by viewModel.wrongQuestions.collectAsState()
    val gson = remember { Gson() }

    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 10
    val totalItems = wrongQuestions.size
    val totalPages = (totalItems + pageSize - 1) / pageSize
    val pagedQuestions = wrongQuestions.drop(currentPage * pageSize).take(pageSize)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("❌ 错题本") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // 科目切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedSubject == "math",
                    onClick = { viewModel.selectSubject("math"); currentPage = 0 },
                    label = { Text("数学", fontSize = 16.sp) }
                )
                FilterChip(
                    selected = selectedSubject == "chinese",
                    onClick = { viewModel.selectSubject("chinese"); currentPage = 0 },
                    label = { Text("语文", fontSize = 16.sp) }
                )
            }

            if (wrongQuestions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无错题，继续保持！🎉", fontSize = 18.sp, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pagedQuestions) { index, question ->
                        val options = try {
                            gson.fromJson(question.options, object : TypeToken<List<String>>() {}.type) as? List<String>
                                ?: emptyList()
                        } catch (e: Exception) { emptyList() }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "${currentPage * pageSize + index + 1}. ${question.question}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "正确答案：${question.answer}",
                                    fontSize = 14.sp,
                                    color = CorrectGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        navController.navigate(Screen.Study.createRoute(question.subject))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleStart)
                                ) {
                                    Text("重新挑战", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                if (totalPages > 1) {
                    PaginationBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        totalItems = totalItems,
                        pageSize = pageSize,
                        onPageChange = { currentPage = it },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
```

---

## Phase 8：抖音 WebView

### Task 8.1：创建抖音界面

- [ ] **Step 1: 创建 DouyinScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/DouyinScreen.kt
package com.yunfan.douyincontrol.ui.screen

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.engine.TimerEngine
import com.yunfan.douyincontrol.ui.component.TimerView
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DouyinScreen(navController: NavController, app: App) {
    val pointsRepository = app.pointsRepository
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var totalSeconds by remember { mutableIntStateOf(600) } // 默认10分钟
    var showTimeUpDialog by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // 检查积分并初始化
    LaunchedEffect(Unit) {
        val balance = pointsRepository.getBalance()
        val rule = pointsRepository.getRule("cost_per_minute")
        val threshold = pointsRepository.getRule("min_cost_threshold")
        val costPerMinute = rule?.value ?: 10
        val minThreshold = threshold?.value ?: 10

        if (balance < minThreshold) {
            // 积分不足
            showTimeUpDialog = true
            return@LaunchedEffect
        }

        val maxMinutes = balance / costPerMinute
        // 最多30分钟
        val minutes = minOf(maxMinutes, 30)
        totalSeconds = minutes * 60
        remainingSeconds = totalSeconds

        // 扣除积分
        pointsRepository.spendPoints(
            amount = minutes * costPerMinute,
            source = "video_cost",
            detail = "看抖音${minutes}分钟"
        )

        // 记录
        app.studyRepository.logVideo(minutes)
    }

    // 倒计时
    LaunchedEffect(remainingSeconds) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        if (remainingSeconds <= 0) {
            showTimeUpDialog = true
        }
    }

    // 时间到/积分不足弹窗
    if (showTimeUpDialog) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text("时间到！") },
            text = { Text("去做题赚积分吧！") },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("去做题")
                }
            },
            dismissButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("返回首页")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📱 抖音") },
                navigationIcon = {
                    TextButton(onClick = {
                        webView?.destroy()
                        navController.popBackStack()
                    }) {
                        Text("← 退出", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // 倒计时
            TimerView(
                remainingSeconds = remainingSeconds,
                totalSeconds = totalSeconds,
                modifier = Modifier.padding(16.dp)
            )

            // WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = false
                            displayZoomControls = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return if (url.startsWith("http")) {
                                    false
                                } else {
                                    true
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl("https://www.douyin.com")
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

---

## Phase 9：学习统计

### Task 9.1：创建统计界面

- [ ] **Step 1: 创建 StatsScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/StatsScreen.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.component.PaginationBar
import com.yunfan.douyincontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController, app: App) {
    val viewModel: StatsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return StatsViewModel(app.pointsRepository, app.studyRepository) as T
            }
        }
    )

    val balance by viewModel.balance.collectAsState()
    val todayCorrect by viewModel.todayCorrect.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 20
    val totalItems = logs.size
    val totalPages = (totalItems + pageSize - 1) / pageSize
    val pagedLogs = logs.drop(currentPage * pageSize).take(pageSize)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 学习统计") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 概览卡片
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("⭐ 积分", "$balance")
                        StatItem("📝 今日做题", "$todayTotal")
                        StatItem("✅ 正确率", if (todayTotal > 0) "${(todayCorrect * 100 / todayTotal)}%" else "0%")
                    }
                }
            }

            // 积分流水标题
            item {
                Text(
                    "积分流水",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // 流水列表
            if (pagedLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无记录", fontSize = 16.sp, color = TextSecondary)
                    }
                }
            } else {
                itemsIndexed(pagedLogs) { _, log ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.sourceDetail, fontSize = 14.sp, color = TextPrimary)
                                // 显示时间
                            val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.CHINA)
                            Text(
                                sdf.format(java.util.Date(log.createdAt)),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            }
                            Text(
                                "${if (log.type == "earn") "+" else "-"}${log.amount}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.type == "earn") CorrectGreen else WrongRed
                            )
                        }
                    }
                }
            }

            // 翻页
            if (totalPages > 1) {
                item {
                    PaginationBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        totalItems = totalItems,
                        pageSize = pageSize,
                        onPageChange = { currentPage = it }
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PurpleStart)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 14.sp, color = TextSecondary)
    }
}
```

- [ ] **Step 2: 创建 StatsViewModel.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/StatsViewModel.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunfan.douyincontrol.data.database.entity.PointsLogEntity
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class StatsViewModel(
    private val pointsRepository: PointsRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    private val _todayCorrect = MutableStateFlow(0)
    val todayCorrect: StateFlow<Int> = _todayCorrect.asStateFlow()

    private val _todayTotal = MutableStateFlow(0)
    val todayTotal: StateFlow<Int> = _todayTotal.asStateFlow()

    private val _logs = MutableStateFlow<List<PointsLogEntity>>(emptyList())
    val logs: StateFlow<List<PointsLogEntity>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            _balance.value = pointsRepository.getBalance()
            val todayStart = getTodayStart()
            _todayTotal.value = studyRepository.getQuestionCountToday(todayStart)
            _todayCorrect.value = studyRepository.getCorrectCountToday(todayStart)

            pointsRepository.getAllLogs().collect { allLogs ->
                _logs.value = allLogs
            }
        }
    }

    private fun getTodayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
```

---

## Phase 10：家长设置

### Task 10.1：创建设置主界面

- [ ] **Step 1: 创建 SettingsScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/settings/SettingsScreen.kt
package com.yunfan.douyincontrol.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yunfan.douyincontrol.ui.navigation.Screen
import com.yunfan.douyincontrol.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, app: App) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ 家长设置") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsItem("积分规则", "调整做题得分、抖音消耗积分") {
                navController.navigate(Screen.Rules.route)
            }
            SettingsItem("年级设置", "当前：一年级") {
                navController.navigate(Screen.Grade.route)
            }
            SettingsItem("题目管理", "查看/删除题目，AI出题") {
                navController.navigate(Screen.QuestionManage.route)
            }
            SettingsItem("学习数据", "查看学习统计") {
                navController.navigate(Screen.Stats.route)
            }
            SettingsItem("修改密码", "修改家长入口密码") {
                navController.navigate(Screen.ChangePassword.route)
            }
            SettingsItem("数据管理", "导出/重置数据") {
                navController.navigate(Screen.Data.route)
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, desc: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(desc, fontSize = 14.sp, color = TextSecondary)
            }
            Text("→", fontSize = 20.sp, color = TextSecondary)
        }
    }
}
```

### Task 10.2：创建各设置子页面

- [ ] **Step 1: 创建 RulesScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/settings/RulesScreen.kt
package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.theme.*

// RulesViewModel 在同一个文件中定义
class RulesViewModel(app: App) : androidx.lifecycle.ViewModel() {
    private val _rules = mutableStateOf<List<com.yunfan.douyincontrol.data.database.entity.RuleEntity>>(emptyList())
    val rules: State<List<com.yunfan.douyincontrol.data.database.entity.RuleEntity>> = _rules

    init {
        androidx.lifecycle.viewModelScope.launch {
            app.database.ruleDao().getAll().collect { ruleList ->
                _rules.value = ruleList
            }
        }
    }

    fun updateRule(key: String, value: Int) {
        androidx.lifecycle.viewModelScope.launch {
            app.database.ruleDao().updateValue(key, value)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(navController: NavController, app: App) {
    val viewModel: RulesViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RulesViewModel(app) as T
            }
        }
    )
    val rules by viewModel.rules

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("积分规则") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rules.forEach { rule ->
                var value by remember(rule) { mutableStateOf(rule.value.toString()) }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(rule.ruleName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = {
                                    value = it.filter { c -> c.isDigit() }
                                },
                                modifier = Modifier.width(120.dp),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val intVal = value.toIntOrNull() ?: return@Button
                                    viewModel.updateRule(rule.ruleKey, intVal)
                                }
                            ) {
                                Text("保存")
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: 创建 GradeScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/settings/GradeScreen.kt
package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.launch

val grades = listOf(
    "bigclass" to "幼儿园大班",
    "grade1" to "小学一年级",
    "grade2" to "小学二年级",
    "grade3" to "小学三年级",
    "grade4" to "小学四年级",
    "grade5" to "小学五年级",
    "grade6" to "小学六年级"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeScreen(navController: NavController, app: App) {
    val scope = rememberCoroutineScope()
    val currentGrade = remember { mutableStateOf("grade1") }

    // 读取当前年级
    LaunchedEffect(Unit) {
        app.dataStore.data.collect { prefs ->
            currentGrade.value = prefs[stringPreferencesKey("current_grade")] ?: "grade1"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("年级设置") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("选择当前年级", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            grades.forEach { (key, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = currentGrade.value == key,
                        onClick = {
                            currentGrade.value = key
                            scope.launch {
                                app.dataStore.edit { prefs ->
                                    prefs[stringPreferencesKey("current_grade")] = key
                                }
                            }
                        }
                    )
                    Text(
                        name,
                        modifier = Modifier.padding(start = 8.dp),
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: 创建 ChangePasswordScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/settings/ChangePasswordScreen.kt
package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.theme.*
import com.yunfan.douyincontrol.util.PasswordUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController, app: App) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改密码") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("旧密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("新密码（4位数字）") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认新密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (newPassword.length != 4 || newPassword.any { !it.isDigit() }) {
                        message = "新密码必须为4位数字"
                        isError = true
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        message = "两次密码不一致"
                        isError = true
                        return@Button
                    }
                    scope.launch {
                        val storedHash = app.dataStore.data.first()[stringPreferencesKey("parent_password_hash")]
                        if (storedHash != null && !PasswordUtil.verify(oldPassword, storedHash)) {
                            message = "旧密码错误"
                            isError = true
                            return@launch
                        }
                        app.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey("parent_password_hash")] = PasswordUtil.hash(newPassword)
                        }
                        message = "密码修改成功！"
                        isError = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确认修改")
            }
            if (message.isNotEmpty()) {
                Text(
                    message,
                    color = if (isError) WrongRed else CorrectGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
```

- [ ] **Step 4: 创建 QuestionManageScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/settings/QuestionManageScreen.kt
package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.ui.component.PaginationBar
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.launch

class QuestionManageViewModel(app: App) : androidx.lifecycle.ViewModel() {
    private val questionRepo = app.questionRepository
    private val aiRepository = com.yunfan.douyincontrol.api.AiRepository(
        com.yunfan.douyincontrol.api.AiService(),
        com.yunfan.douyincontrol.engine.QuizEngine(questionRepo)
    )

    private val _questions = mutableStateOf<List<QuestionEntity>>(emptyList())
    val questions: State<List<QuestionEntity>> = _questions

    private val _isGenerating = mutableStateOf(false)
    val isGenerating: State<Boolean> = _isGenerating

    private val _message = mutableStateOf("")
    val message: State<String> = _message

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        androidx.lifecycle.viewModelScope.launch {
            questionRepo.getAllQuestions().collect { list ->
                _questions.value = list
            }
        }
    }

    fun deleteQuestion(id: Long) {
        androidx.lifecycle.viewModelScope.launch {
            questionRepo.softDelete(id)
        }
    }

    fun generateQuestions(subject: String, grade: String, count: Int = 5) {
        androidx.lifecycle.viewModelScope.launch {
            _isGenerating.value = true
            _message.value = "正在生成题目..."
            try {
                val saved = aiRepository.generateAndSaveQuestions(subject, grade, count)
                _message.value = "成功生成 ${saved.size} 道新题目"
            } catch (e: Exception) {
                _message.value = "生成失败：${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionManageScreen(navController: NavController, app: App) {
    val viewModel: QuestionManageViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return QuestionManageViewModel(app) as T
            }
        }
    )
    val questions by viewModel.questions
    val isGenerating by viewModel.isGenerating
    val message by viewModel.message

    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 20
    val totalItems = questions.size
    val totalPages = (totalItems + pageSize - 1) / pageSize
    val pagedQuestions = questions.drop(currentPage * pageSize).take(pageSize)

    // AI 出题状态
    var showGenerateDialog by remember { mutableStateOf(false) }
    var generateSubject by remember { mutableStateOf("math") }
    var generateGrade by remember { mutableStateOf("grade1") }
    var generateCount by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 题目管理") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { showGenerateDialog = true }) {
                        Text("AI出题", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (message.isNotEmpty()) {
                Text(message, fontSize = 14.sp, color = if (message.contains("失败")) WrongRed else CorrectGreen)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (pagedQuestions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无题目，点击右上角 AI出题 生成", fontSize = 16.sp, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pagedQuestions) { index, question ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${currentPage * pageSize + index + 1}. ${question.question}",
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${if (question.subject == "math") "数学" else "语文"} | ${question.grade}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteQuestion(question.id) }) {
                                    Text("🗑️")
                                }
                            }
                        }
                    }
                }

                if (totalPages > 1) {
                    PaginationBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        totalItems = totalItems,
                        pageSize = pageSize,
                        onPageChange = { currentPage = it },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // AI出题对话框
        if (showGenerateDialog) {
            AlertDialog(
                onDismissRequest = { showGenerateDialog = false },
                title = { Text("AI 出题") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("科目：")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = generateSubject == "math",
                                onClick = { generateSubject = "math" },
                                label = { Text("数学") }
                            )
                            FilterChip(
                                selected = generateSubject == "chinese",
                                onClick = { generateSubject = "chinese" },
                                label = { Text("语文") }
                            )
                        }
                        Text("年级：")
                        // 简单的下拉选择
                        grades.forEach { (key, name) ->
                            Row {
                                RadioButton(
                                    selected = generateGrade == key,
                                    onClick = { generateGrade = key }
                                )
                                Text(name, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                        OutlinedTextField(
                            value = generateCount,
                            onValueChange = { generateCount = it.filter { c -> c.isDigit() } },
                            label = { Text("生成数量（1-10）") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showGenerateDialog = false
                        val count = generateCount.toIntOrNull() ?: 5
                        viewModel.generateQuestions(generateSubject, generateGrade, count.coerceIn(1, 10))
                    }) {
                        Text("开始生成")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGenerateDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
```

- [ ] **Step 5: 创建 DataScreen.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/ui/screen/settings/DataScreen.kt
package com.yunfan.douyincontrol.ui.screen.settings

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(navController: NavController, app: App) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据管理") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = {
                    scope.launch {
                        exportData(context, app)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📤 导出学习记录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("导出为CSV格式", fontSize = 14.sp, color = TextSecondary)
                }
            }

            Card(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🗑️ 重置所有数据", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WrongRed)
                    Text("清除所有数据，此操作不可恢复", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("确认重置？") },
                text = { Text("清除所有数据包括题目、积分、学习记录。此操作不可恢复！") },
                confirmButton = {
                    TextButton(onClick = {
                        showResetDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                context.deleteDatabase("douyin_control.db")
                            }
                            Toast.makeText(context, "数据已重置", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("确认重置", color = WrongRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

private suspend fun exportData(context: Context, app: App) = withContext(Dispatchers.IO) {
    try {
        val logs = app.database.studyLogDao().getPage(1000, 0)
        val file = File(context.getExternalFilesDir(null), "study_logs_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.write("ID,类型,科目,结果,详情,时间\n")
            logs.forEach { log ->
                writer.write("${log.id},${log.type},${log.subject},${log.result},${log.detail ?: ""},${log.createdAt}\n")
            }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "已导出到：${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

---

## Phase 11：种子题库 & 初始化

### Task 11.1：创建种子题库

- [ ] **Step 1: 创建 seed_questions.json**

在 `app/src/main/assets/seed_questions.json` 中存放预置种子题库：

```json
[
  {
    "subject": "math",
    "grade": "bigclass",
    "question": "3 + 5 = ?",
    "options": ["A. 6", "B. 7", "C. 8", "D. 9"],
    "answer": "C. 8"
  },
  {
    "subject": "math",
    "grade": "bigclass",
    "question": "10 - 4 = ?",
    "options": ["A. 4", "B. 5", "C. 6", "D. 7"],
    "answer": "C. 6"
  },
  {
    "subject": "math",
    "grade": "grade1",
    "question": "15 + 8 = ?",
    "options": ["A. 21", "B. 22", "C. 23", "D. 24"],
    "answer": "C. 23"
  },
  {
    "subject": "chinese",
    "grade": "bigclass",
    "question": "苹果的'苹'字读音是？",
    "options": ["A. pín", "B. píng", "C. pǐng", "D. pìng"],
    "answer": "B. píng"
  },
  {
    "subject": "chinese",
    "grade": "grade1",
    "question": "下列哪个字是上下结构？",
    "options": ["A. 明", "B. 花", "C. 妈", "D. 林"],
    "answer": "B. 花"
  }
]
```

*（此处仅示例5题，实际需准备至少100道题，覆盖大班到二年级各科）*

- [ ] **Step 2: 创建 SeedQuestions.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/data/seed/SeedQuestions.kt
package com.yunfan.douyincontrol.data.seed

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yunfan.douyincontrol.data.database.AppDatabase
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class SeedQuestions(private val context: Context, private val database: AppDatabase) {

    data class SeedQuestion(
        val subject: String,
        val grade: String,
        val question: String,
        val options: List<String>,
        val answer: String
    )

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val count = database.questionDao().countBySubjectAndGrade("math", "bigclass")
        if (count > 0) return@withContext // 已有数据，不再导入

        try {
            val inputStream = context.assets.open("seed_questions.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val type = object : TypeToken<List<SeedQuestion>>() {}.type
            val seeds: List<SeedQuestion> = gson.fromJson(reader, type)

            val entities = seeds.map { seed ->
                QuestionEntity(
                    subject = seed.subject,
                    grade = seed.grade,
                    question = seed.question,
                    options = gson.toJson(seed.options),
                    answer = seed.answer,
                    source = "seed"
                )
            }
            database.questionDao().insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
```

- [ ] **Step 3: 在 App.kt 中调用种子导入**

```kotlin
// 在 App.kt 的 onCreate 中增加：
override fun onCreate() {
    super.onCreate()
    applicationScope.launch {
        seedRules()
        SeedQuestions(this@App, database).seedIfNeeded()
    }
}
```

---

## Phase 12：计时引擎

### Task 12.1：创建计时引擎

- [ ] **Step 1: 创建 TimerEngine.kt**

```kotlin
// app/src/main/java/com/yunfan/douyincontrol/engine/TimerEngine.kt
package com.yunfan.douyincontrol.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerEngine {

    private var job: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start(totalSeconds: Int, scope: CoroutineScope, onTimeUp: () -> Unit) {
        stop()
        _remainingSeconds.value = totalSeconds
        _isRunning.value = true

        job = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            _isRunning.value = false
            onTimeUp()
        }
    }

    fun stop() {
        job?.cancel()
        _isRunning.value = false
    }

    fun pause() {
        // 倒计时暂停（当前方案是匀速减少，不需要暂停）
    }

    fun resume() {
        // 继续倒计时
    }

    fun getElapsedSeconds(totalSeconds: Int): Int {
        return totalSeconds - _remainingSeconds.value
    }
}
```

---

## Phase 13：打包与发布

### Task 13.1：生成 APK

- [ ] **Step 1: 配置签名**

在 `app/build.gradle.kts` 中添加签名配置。

- [ ] **Step 2: 生成 APK**

```bash
# 在 Android Studio 中
Build → Build Bundle(s) / APK(s) → Build APK(s)

# 或命令行
./gradlew assembleDebug
```

APK 生成路径：`app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: 侧载安装到华为平板**

1. 将 APK 通过 USB 或 QQ/微信发送到平板
2. 在平板上点击安装
3. 如果提示"未知来源应用"，在设置中允许安装
4. 在华为平板"儿童乐园"中设置只允许此 App 运行

---

## 实施路线汇总

| Phase | 内容 | 预计工作量 |
|-------|------|:---------:|
| Phase 0 | 环境搭建 | 1 天 |
| Phase 1 | 数据库层（Entity+DAO+Repository） | 1 天 |
| Phase 2 | 主题+导航+工具类+主Activity | 0.5 天 |
| Phase 3 | UI 通用组件 | 0.5 天 |
| Phase 4 | 首页 | 1 天 |
| Phase 5 | 做题系统 | 1-2 天 |
| Phase 6 | AI 出题与去重 | 1 天 |
| Phase 7 | 错题本 | 0.5 天 |
| Phase 8 | 抖音 WebView | 1 天 |
| Phase 9 | 学习统计 | 0.5 天 |
| Phase 10 | 家长设置 | 1-2 天 |
| Phase 11 | 种子题库 | 0.5 天 |
| Phase 12 | 计时引擎 | 0.5 天 |
| Phase 13 | 打包发布 | 0.5 天 |
| **总计** | | **~10-13 天** |

---

## 范围检查

设计文档覆盖了以下需求，本计划均包含对应任务：

| 需求 | 对应任务 |
|------|---------|
| 做题赚积分 | Task 5.2, 5.3 |
| AI 实时出题+去重 | Task 6.1 |
| 本地 SQLite 题库 | Task 1.1, 1.2, 1.3 |
| 积分制/自定义兑换规则 | Task 1.5, 10.2 (RulesScreen) |
| 错题本（错题不给积分） | Task 7.1, 7.2 |
| 错题重做对给积分 | Task 5.2 |
| 抖音 WebView 寄生 | Task 8.1 |
| 倒计时/时长管理 | Task 3.1, 12.1 |
| 学习统计 | Task 9.1 |
| 家长设置（密码保护） | Task 4.2, 10.1, 10.2 |
| 翻页组件 | Task 3.1 |
| 种子题库 | Task 11.1 |
| 年级设置（大班~六年级） | Task 10.2 |
