package com.example.habittracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.habittracker.ui.screens.addhabit.AddHabitScreen
import com.example.habittracker.ui.screens.tasks.TasksScreen

object Routes {
    const val TASKS = "tasks"
    const val ADD = "add"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.TASKS) {
        composable(Routes.TASKS) {
            TasksScreen(onAddTask = { navController.navigate(Routes.ADD) })
        }
        composable(Routes.ADD) {
            AddHabitScreen(onDone = { navController.popBackStack() })
        }
    }
}
