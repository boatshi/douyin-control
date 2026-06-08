package com.yunfan.douyincontrol.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE deleted = 0 AND subject = :subject AND grade = :grade ORDER BY RANDOM()")
    fun getQuestionsBySubjectAndGrade(subject: String, grade: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE deleted = 0 AND subject = :subject AND grade = :grade LIMIT :limit")
    suspend fun getQuestionsBySubjectAndGradeLimit(subject: String, grade: String, limit: Int): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions WHERE deleted = 0 AND subject = :subject AND grade = :grade
        AND id NOT IN (SELECT questionId FROM study_logs WHERE questionId IS NOT NULL)
        LIMIT :limit
    """)
    suspend fun getUnansweredQuestions(subject: String, grade: String, limit: Int): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE deleted = 0 AND subject = :subject AND grade = :grade")
    suspend fun countBySubjectAndGrade(subject: String, grade: String): Int

    @Query("SELECT * FROM questions WHERE question = :questionText LIMIT 1")
    suspend fun findByQuestionText(questionText: String): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(questions: List<QuestionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(question: QuestionEntity): Long

    @Query("UPDATE questions SET deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM questions WHERE deleted = 0 AND subject = :subject ORDER BY id DESC")
    fun getAllBySubject(subject: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE deleted = 0 ORDER BY id DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>
}
