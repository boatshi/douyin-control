package com.yunfan.douyincontrol.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.database.entity.StudyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Insert
    suspend fun insert(log: StudyLogEntity)

    @Query("SELECT * FROM study_logs ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<StudyLogEntity>

    @Query("SELECT COUNT(*) FROM study_logs")
    suspend fun count(): Int

    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN study_logs s ON q.id = s.questionId
        WHERE s.result = 'wrong' AND q.id NOT IN (
            SELECT questionId FROM study_logs WHERE result = 'rework_correct' AND questionId IS NOT NULL
        ) AND q.deleted = 0 AND q.subject = :subject
        GROUP BY q.id ORDER BY s.id DESC
    """)
    fun getWrongQuestionsBySubject(subject: String): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM study_logs WHERE type = 'question' AND createdAt >= :since")
    suspend fun getQuestionCountToday(since: Long): Int

    @Query("SELECT COUNT(*) FROM study_logs WHERE type = 'question' AND result = 'correct' AND createdAt >= :since")
    suspend fun getCorrectCountToday(since: Long): Int

    @Query("""
        SELECT COUNT(*) FROM study_logs
        WHERE questionId = :questionId AND result = 'wrong'
        AND questionId NOT IN (SELECT questionId FROM study_logs WHERE result = 'rework_correct' AND questionId = :questionId)
    """)
    suspend fun isInWrongBook(questionId: Long): Int
}
