package com.yunfan.douyincontrol

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.yunfan.douyincontrol.data.database.AppDatabase
import com.yunfan.douyincontrol.data.database.entity.RuleEntity
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.QuestionRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import com.yunfan.douyincontrol.data.seed.SeedQuestions
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import kotlinx.coroutines.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class App : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val questionRepository by lazy { QuestionRepository(database.questionDao()) }
    val pointsRepository by lazy { PointsRepository(database.pointsLogDao(), database.ruleDao()) }
    val studyRepository by lazy { StudyRepository(database.studyLogDao()) }

    // GeckoView 运行时（全局单例）
    val geckoRuntime by lazy {
        Log.i("GeckoView", "GeckoRuntime 正在初始化...")
        GeckoRuntime.create(this).also {
            Log.i("GeckoView", "GeckoRuntime 初始化完成 ✅")
        }
    }

    // 预加载的 GeckoSession
    private var _preloadedSession: GeckoSession? = null
    var isSessionPreloaded = false
        private set

    // 在首页时调用，后台预加载抖音
    @Synchronized
    fun preloadDouyin() {
        if (_preloadedSession != null) return
        val session = GeckoSession()
        session.open(geckoRuntime)
        session.settings.userAgentOverride = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStop(p0: GeckoSession, p1: Boolean) {}
            override fun onPageStart(p0: GeckoSession, p1: String) {}
            override fun onProgressChange(p0: GeckoSession, p1: Int) {}
            override fun onSecurityChange(p0: GeckoSession, p1: GeckoSession.ProgressDelegate.SecurityInformation) {}
            override fun onSessionStateChange(p0: GeckoSession, p1: GeckoSession.SessionState) {}
        }
        session.loadUri("https://www.douyin.com")
        _preloadedSession = session
        isSessionPreloaded = true
        Log.i("GeckoView", "🔄 后台预加载抖音中...")
    }

    // 取出预加载的 Session（取一次后销毁，只给 DouyinScreen 用）
    @Synchronized
    fun takePreloadedSession(): GeckoSession? {
        val session = _preloadedSession
        _preloadedSession = null
        isSessionPreloaded = false
        return session
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedRules()
            SeedQuestions(this@App, database).seedIfNeeded()
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
