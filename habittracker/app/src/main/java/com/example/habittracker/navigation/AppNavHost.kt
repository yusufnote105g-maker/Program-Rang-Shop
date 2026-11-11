package com.example.habittracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.ui.screens.analytics.AnalyticsScreen
import com.example.habittracker.ui.screens.taskform.TaskFormScreen
import com.example.habittracker.ui.screens.taskform.TaskFormViewModel
import com.example.habittracker.ui.screens.tasks.TasksScreen

object Routes {
    const val TASKS = "tasks"
    const val ANALYTICS = "analytics"

    /** Тип новой задачи передаём аргументом, иначе «+» на вкладке всегда открывал бы привычку. */
    const val ADD = "add?type={type}"

    /** S06: редактирование по id. */
    const val EDIT = "edit/{taskId}"

    fun add(type: TaskType): String = "add?type=${type.name}"

    fun edit(taskId: String): String = "edit/$taskId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.TASKS) {
        composable(Routes.TASKS) {
            TasksScreen(
                onAddTask = { type -> navController.navigate(Routes.add(type)) },
                onEditTask = { taskId -> navController.navigate(Routes.edit(taskId)) },
                onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
            )
        }
        composable(Routes.ANALYTICS) {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.ADD,
            arguments = listOf(
                navArgument(TaskFormViewModel.ARG_TYPE) {
                    type = NavType.StringType
                    defaultValue = TaskType.HABIT.name
                },
            ),
        ) {
            TaskFormScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument(TaskFormViewModel.ARG_TASK_ID) {
                    type = NavType.StringType
                },
            ),
        ) {
            TaskFormScreen(onDone = { navController.popBackStack() })
        }
    }
}
