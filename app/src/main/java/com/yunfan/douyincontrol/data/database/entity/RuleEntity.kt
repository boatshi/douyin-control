package com.yunfan.douyincontrol.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleKey: String,
    val ruleName: String,
    val value: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
