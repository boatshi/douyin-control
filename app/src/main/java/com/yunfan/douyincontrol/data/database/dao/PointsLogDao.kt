package com.yunfan.douyincontrol.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
