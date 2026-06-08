package com.yunfan.douyincontrol.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Study : Screen("study/{subject}") {
        fun createRoute(subject: String) = "study/$subject"
    }
    object Douyin : Screen("douyin")
    object WrongBook : Screen("wrong_book")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object Rules : Screen("settings/rules")
    object QuestionManage : Screen("settings/questions")
    object Grade : Screen("settings/grade")
    object Data : Screen("settings/data")
    object ChangePassword : Screen("settings/password")
    object ApiConfig : Screen("settings/api")
}
