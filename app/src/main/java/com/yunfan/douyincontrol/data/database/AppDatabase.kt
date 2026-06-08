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
