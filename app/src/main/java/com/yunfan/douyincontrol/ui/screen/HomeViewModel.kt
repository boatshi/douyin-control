package com.yunfan.douyincontrol.ui.screen

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunfan.douyincontrol.data.repository.PointsRepository
import com.yunfan.douyincontrol.data.repository.StudyRepository
import com.yunfan.douyincontrol.util.PasswordUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(
    private val pointsRepository: PointsRepository,
    private val studyRepository: StudyRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    private val _parentPasswordHash = MutableStateFlow("")
    val parentPasswordHash: StateFlow<String> = _parentPasswordHash.asStateFlow()

    private val _currentGrade = MutableStateFlow("bigclass")
    val currentGrade: StateFlow<String> = _currentGrade.asStateFlow()

    private val _passwordErrorCount = MutableStateFlow(0)
    val passwordErrorCount: StateFlow<Int> = _passwordErrorCount.asStateFlow()

    private val _lockedUntil = MutableStateFlow(0L)
    val lockedUntil: StateFlow<Long> = _lockedUntil.asStateFlow()

    companion object {
        private val PASSWORD_KEY = stringPreferencesKey("parent_password_hash")
        private val GRADE_KEY = stringPreferencesKey("current_grade")
        private val ERROR_COUNT_KEY = intPreferencesKey("password_error_count")
        private val LOCKED_UNTIL_KEY = longPreferencesKey("locked_until")
    }

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _parentPasswordHash.value = prefs[PASSWORD_KEY] ?: PasswordUtil.hash("1234")
                _currentGrade.value = prefs[GRADE_KEY] ?: "grade1"
                _passwordErrorCount.value = prefs[ERROR_COUNT_KEY] ?: 0
                _lockedUntil.value = prefs[LOCKED_UNTIL_KEY] ?: 0L
            }
        }
        refreshBalance()
    }

    fun refreshBalance() {
        viewModelScope.launch {
            _balance.value = pointsRepository.getBalance()
        }
    }

    fun verifyPassword(input: String): Boolean {
        val now = System.currentTimeMillis()
        if (now < _lockedUntil.value) return false

        val isValid = PasswordUtil.verify(input, _parentPasswordHash.value)
        if (!isValid) {
            val newCount = _passwordErrorCount.value + 1
            viewModelScope.launch {
                dataStore.edit { prefs ->
                    prefs[ERROR_COUNT_KEY] = newCount
                    if (newCount >= 3) {
                        prefs[LOCKED_UNTIL_KEY] = now + 30_000
                    }
                }
            }
        } else {
            viewModelScope.launch {
                dataStore.edit { prefs ->
                    prefs[ERROR_COUNT_KEY] = 0
                    prefs[LOCKED_UNTIL_KEY] = 0L
                }
            }
        }
        return isValid
    }
}
