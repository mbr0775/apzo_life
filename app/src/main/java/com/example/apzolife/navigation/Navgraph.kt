package com.example.apzolife.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.apzolife.data.repository.AuthRepository
import com.example.apzolife.ui.screens.auth.ForgotPasswordScreen
import com.example.apzolife.ui.screens.auth.LoginScreen
import com.example.apzolife.ui.screens.auth.SignupScreen
import com.example.apzolife.ui.screens.calendar.CalendarScreen
import com.example.apzolife.ui.screens.completed.CompletedScreen
import com.example.apzolife.ui.screens.home.HomeScreen
import com.example.apzolife.ui.screens.insight.InsightScreen
import com.example.apzolife.ui.screens.settings.SettingsScreen
import com.example.apzolife.ui.screens.task.AddTaskScreen
import com.example.apzolife.ui.screens.task.EditTaskScreen
import com.example.apzolife.ui.screens.task.TaskDetailScreen
import com.example.apzolife.viewmodel.ApzoViewModel
import com.example.apzolife.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Signup         : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object Home           : Screen("home")
    object Calendar       : Screen("calendar")
    object Insights       : Screen("insights")
    object Completed      : Screen("completed")
    object Settings       : Screen("settings")
    object AddTask        : Screen("add_task")

    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: String) = "edit_task/$taskId"
    }
}

@Composable
fun ApzoNavGraph(navController: NavHostController) {
    val viewModel: ApzoViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    val startDestination = if (AuthRepository.isLoggedIn()) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenSignup = { navController.navigate(Screen.Signup.route) },
                onOpenForgot = { navController.navigate(Screen.ForgotPassword.route) },
                viewModel = authViewModel
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onBackToLogin = { navController.popBackStack() },
                viewModel = authViewModel
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                viewModel = authViewModel
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                viewModel = viewModel,
                onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) }
            )
        }

        composable(Screen.Insights.route) {
            InsightScreen(viewModel = viewModel)
        }

        composable(Screen.Completed.route) {
            CompletedScreen(
                viewModel = viewModel,
                onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.AddTask.route) {
            AddTaskScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onTaskCreated = {
                    viewModel.loadHomeData()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            TaskDetailScreen(
                taskId = taskId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.EditTask.createRoute(taskId)) }
            )
        }

        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            EditTaskScreen(
                taskId = taskId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = {
                    viewModel.loadHomeData()
                    navController.popBackStack()
                }
            )
        }
    }
}