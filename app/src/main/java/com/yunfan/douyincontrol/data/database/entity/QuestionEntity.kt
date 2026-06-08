package com.yunfan.douyincontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val grade: String,
    val question: String,
    val options: String,
    val answer: String,
    val deleted: Int = 0,
    val source: String = "ai",
    val createdAt: Long = System.currentTimeMillis()
)
