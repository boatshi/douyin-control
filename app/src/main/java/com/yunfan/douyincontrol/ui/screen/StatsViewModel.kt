package com.yunfan.douyincontrol.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunfan.douyincontrol.data.database.entity.PointsLogEntity
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class StatsViewModel(
    private val pointsRepository: PointsRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    private val _todayCorrect = MutableStateFlow(0)
    val todayCorrect: StateFlow<Int> = _todayCorrect.asStateFlow()

    private val _todayTotal = MutableStateFlow(0)
    val todayTotal: StateFlow<Int> = _todayTotal.asStateFlow()

    private val _logs = MutableStateFlow<List<PointsLogEntity>>(emptyList())
    val logs: StateFlow<List<PointsLogEntity>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            _balance.value = pointsRepository.getBalance()
            val todayStart = getTodayStart()
            _todayTotal.value = studyRepository.getQuestionCountToday(todayStart)
            _todayCorrect.value = studyRepository.getCorrectCountToday(todayStart)

            pointsRepository.getAllLogs().collect { allLogs ->
                _logs.value = allLogs
            }
        }
    }

    private fun getTodayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
