package com.yunfan.douyincontrol.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.dataStore
import com.yunfan.douyincontrol.ui.theme.*
import kotlinx.coroutines.launch

val grades = listOf(
    "bigclass" to "幼儿园大班",
    "grade1" to "小学一年级",
    "grade2" to "小学二年级",
    "grade3" to "小学三年级",
    "grade4" to "小学四年级",
    "grade5" to "小学五年级",
    "grade6" to "小学六年级"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeScreen(navController: NavController, app: App) {
    val scope = rememberCoroutineScope()
    val currentGrade = remember { mutableStateOf("grade1") }

    LaunchedEffect(Unit) {
        app.dataStore.data.collect { prefs ->
            currentGrade.value = prefs[stringPreferencesKey("current_grade")] ?: "grade1"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("年级设置") },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("选择当前年级", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            grades.forEach { (key, name) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentGrade.value == key,
                        onClick = {
                            currentGrade.value = key
                            scope.launch {
                                app.dataStore.edit { prefs ->
                                    prefs[stringPreferencesKey("current_grade")] = key
                                }
                            }
                        }
                    )
                    Text(name, modifier = Modifier.padding(start = 8.dp), fontSize = 18.sp, color = TextPrimary)
                }
            }
        }
    }
}
