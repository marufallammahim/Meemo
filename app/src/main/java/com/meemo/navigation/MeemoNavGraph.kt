package com.meemo.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meemo.domain.usecase.ProcessCommandUseCase
import com.meemo.feature.assistant.AssistantScreen
import com.meemo.feature.assistant.AssistantViewModel
import com.meemo.feature.assistant.AssistantViewModelFactory

@Composable
fun MeemoNavGraph(processCommandUseCase: ProcessCommandUseCase) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Destinations.ASSISTANT
    ) {
        composable(Destinations.ASSISTANT) {
            val assistantViewModel: AssistantViewModel = viewModel(
                factory = AssistantViewModelFactory(processCommandUseCase)
            )
            AssistantScreen(viewModel = assistantViewModel)
        }
    }
}
