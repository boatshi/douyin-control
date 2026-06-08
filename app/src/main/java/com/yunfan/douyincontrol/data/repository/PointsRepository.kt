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

    suspend fun getRule(key: String): RuleEntity? = ruleDao.getByKey(key)

    fun getAllRules(): Flow<List<RuleEntity>> = ruleDao.getAll()

    suspend fun updateRule(key: String, value: Int) = ruleDao.updateValue(key, value)
}
