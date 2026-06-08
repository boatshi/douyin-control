package com.yunfan.douyincontrol.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
