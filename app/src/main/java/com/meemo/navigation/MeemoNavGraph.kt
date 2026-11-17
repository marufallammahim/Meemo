package com.meemo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meemo.feature.assistant.AssistantScreen

@Composable
fun MeemoNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Destinations.ASSISTANT
    ) {
        composable(Destinations.ASSISTANT) {
            AssistantScreen()
        }
    }
}
