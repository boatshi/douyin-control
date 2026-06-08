package com.yunfan.douyincontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val subject: String? = null,
    val questionId: Long? = null,
    val result: String,
    val detail: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
