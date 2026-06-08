package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.data.database.entity.RuleEntity
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RulesViewModel(private val app: App) : ViewModel() {
    private val _rules = MutableStateFlow<List<RuleEntity>>(emptyList())
    val rules: StateFlow<List<RuleEntity>> = _rules.asStateFlow()

    init {
        viewModelScope.launch {
            app.database.ruleDao().getAll().collect { ruleList ->
                _rules.value = ruleList
            }
        }
    }

    fun updateRule(key: String, value: Int) {
        viewModelScope.launch {
            app.database.ruleDao().updateValue(key, value)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(navController: NavController, app: App) {
    val viewModel: RulesViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RulesViewModel(app) as T
            }
        }
    )
    val rules by viewModel.rules.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("积分规则") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("← 返回", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurpleStart,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rules.forEach { rule ->
                var value by remember(rule) { mutableStateOf(rule.value.toString()) }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(rule.ruleName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { value = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.width(120.dp),
                                singleLine = true
                            )
                            Button(onClick = {
                                val intVal = value.toIntOrNull() ?: return@Button
                                viewModel.updateRule(rule.ruleKey, intVal)
                            }) { Text("保存") }
                        }
                    }
                }
            }
        }
    }
}
