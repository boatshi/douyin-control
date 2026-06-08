package com.yunfan.douyincontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "points_log")
data class PointsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val amount: Int,
    val source: String,
    val sourceDetail: String,
    val createdAt: Long = System.currentTimeMillis()
)
