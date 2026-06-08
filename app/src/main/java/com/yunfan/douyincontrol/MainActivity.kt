package com.yunfan.douyincontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yunfan.douyincontrol.ui.navigation.NavGraph
import com.yunfan.douyincontrol.ui.theme.BackgroundLight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundLight
            ) {
                val navController = rememberNavController()
                NavGraph(navController = navController, app = app)
            }
        }
    }
}
