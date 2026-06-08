package com.yunfan.douyincontrol.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunfan.douyincontrol.data.database.entity.QuestionEntity
import com.yunfan.douyincontrol.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WrongBookViewModel(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _selectedSubject = MutableStateFlow("math")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _wrongQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val wrongQuestions: StateFlow<List<QuestionEntity>> = _wrongQuestions.asStateFlow()

    init {
        loadWrongQuestions()
    }

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
        loadWrongQuestions()
    }

    private fun loadWrongQuestions() {
        viewModelScope.launch {
            studyRepository.getWrongQuestionsBySubject(_selectedSubject.value)
                .collect { questions ->
                    _wrongQuestions.value = questions
                }
        }
    }
}
