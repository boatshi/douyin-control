package com.yunfan.douyincontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yunfan.douyincontrol.App
import com.yunfan.douyincontrol.ui.screen.*
import com.yunfan.douyincontrol.ui.screen.settings.*

@Composable
fun NavGraph(navController: NavHostController, app: App) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(navController = navController, app = app)
        }

        composable(
            route = Screen.Study.route,
            arguments = listOf(navArgument("subject") { type = NavType.StringType })
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject") ?: "math"
            StudyScreen(navController = navController, app = app, subject = subject)
        }

        composable(Screen.Douyin.route) {
            DouyinScreen(navController = navController, app = app)
        }

        composable(Screen.WrongBook.route) {
            WrongBookScreen(navController = navController, app = app)
        }

        composable(Screen.Stats.route) {
            StatsScreen(navController = navController, app = app)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController, app = app)
        }

        composable(Screen.Rules.route) {
            RulesScreen(navController = navController, app = app)
        }

        composable(Screen.QuestionManage.route) {
            QuestionManageScreen(navController = navController, app = app)
        }

        composable(Screen.Grade.route) {
            GradeScreen(navController = navController, app = app)
        }

        composable(Screen.Data.route) {
            DataScreen(navController = navController, app = app)
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(navController = navController, app = app)
        }

        composable(Screen.ApiConfig.route) {
            ApiConfigScreen(navController = navController, app = app)
        }
    }
}
