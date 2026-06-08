package com.yunfan.douyincontrol.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.QuestionRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import com.yunfan.douyincontrol.engine.QuizEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel(
    private val questionRepository: QuestionRepository,
    private val pointsRepository: PointsRepository,
    private val studyRepository: StudyRepository,
    private val quizEngine: QuizEngine
) : ViewModel() {

    private val _currentQuestion = MutableStateFlow<QuestionEntity?>(null)
    val currentQuestion: StateFlow<QuestionEntity?> = _currentQuestion.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _totalQuestions = MutableStateFlow(0)
    val totalQuestions: StateFlow<Int> = _totalQuestions.asStateFlow()

    private val _selectedAnswer = MutableStateFlow<String?>(null)
    val selectedAnswer: StateFlow<String?> = _selectedAnswer.asStateFlow()

    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult.asStateFlow()

    private val _isCorrect = MutableStateFlow<Boolean?>(null)
    val isCorrect: StateFlow<Boolean?> = _isCorrect.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _earningThisSession = MutableStateFlow(0)
    val earningThisSession: StateFlow<Int> = _earningThisSession.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private var questions: List<QuestionEntity> = emptyList()

    fun startStudy(subject: String, grade: String, questionsList: List<QuestionEntity>? = null) {
        viewModelScope.launch {
            questions = questionsList ?: quizEngine.getQuestionsForStudy(subject, grade)
            if (questions.isEmpty()) {
                _message.value = "题库暂时没有题目，请先在家长设置中生成题目"
                _finished.value = true
                return@launch
            }
            _totalQuestions.value = questions.size
            _currentIndex.value = 0
            _score.value = 0
            _earningThisSession.value = 0
            _finished.value = false
            _selectedAnswer.value = null
            _showResult.value = false
            _isCorrect.value = null
            _message.value = ""
            showQuestion(0)
        }
    }

    private fun showQuestion(index: Int) {
        if (index >= questions.size) {
            _finished.value = true
            _message.value = "全部完成！共获得 $_earningThisSession 积分"
            return
        }
        _currentQuestion.value = questions[index]
        _currentIndex.value = index
        _selectedAnswer.value = null
        _showResult.value = false
        _isCorrect.value = null
    }

    fun selectAnswer(answer: String) {
        if (_showResult.value) return
        val question = _currentQuestion.value ?: return

        _selectedAnswer.value = answer
        _showResult.value = true

        viewModelScope.launch {
            val correct = answer == question.answer
            _isCorrect.value = correct

            if (correct) {
                val rule = pointsRepository.getRule("score_per_question")
                val points = rule?.value ?: 10

                val inWrongBook = studyRepository.isInWrongBook(question.id)
                val pointsToAdd = if (inWrongBook) {
                    val reworkRule = pointsRepository.getRule("score_per_rework")
                    pointsRepository.addPoints(reworkRule?.value ?: 10, "question_wrong_redo", "错题重做对：${question.question}")
                    studyRepository.logQuestion(question.id, question.subject, "rework_correct")
                    reworkRule?.value ?: 10
                } else {
                    pointsRepository.addPoints(points, "question_correct", "做对${if (question.subject == "math") "数学" else "语文"}题：${question.question}")
                    studyRepository.logQuestion(question.id, question.subject, "correct")
                    points
                }
                _earningThisSession.value += pointsToAdd
                _score.value += 1
            } else {
                studyRepository.logQuestion(question.id, question.subject, "wrong")
            }
        }
    }

    fun nextQuestion() {
        showQuestion(_currentIndex.value + 1)
    }

    fun reset() {
        _finished.value = false
        _currentIndex.value = 0
        questions = emptyList()
    }
}
