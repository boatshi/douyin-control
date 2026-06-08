package com.yunfan.douyincontrol

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.yunfan.douyincontrol.data.database.AppDatabase
import com.yunfan.douyincontrol.data.database.entity.RuleEntity
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.QuestionRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import com.yunfan.douyincontrol.data.seed.SeedQuestions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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
