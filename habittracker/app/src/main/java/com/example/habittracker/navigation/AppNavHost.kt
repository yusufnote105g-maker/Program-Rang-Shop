package com.example.habittracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.ui.screens.addhabit.AddHabitScreen
import com.example.habittracker.ui.screens.addhabit.AddHabitViewModel
import com.example.habittracker.ui.screens.tasks.TasksScreen

object Routes {
    const val TASKS = "tasks"

    /** Тип новой задачи передаём аргументом, иначе «+» на вкладке всегда открывал бы привычку. */
    const val ADD = "add?type={type}"

    fun add(type: TaskType): String = "add?type=${type.name}"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.TASKS) {
        composable(Routes.TASKS) {
            TasksScreen(onAddTask = { type -> navController.navigate(Routes.add(type)) })
        }
        composable(
            route = Routes.ADD,
            arguments = listOf(
                navArgument(AddHabitViewModel.ARG_TYPE) {
                    type = NavType.StringType
                    defaultValue = TaskType.HABIT.name
                },
            ),
        ) {
            AddHabitScreen(onDone = { navController.popBackStack() })
        }
    }
}
