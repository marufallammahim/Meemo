package com.marufallammahim.meemo.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marufallammahim.meemo.R
import com.marufallammahim.meemo.presentation.screens.AppsViewModel
import com.marufallammahim.meemo.presentation.screens.AssistantHomeScreen
import com.marufallammahim.meemo.presentation.screens.AssistantViewModel
import com.marufallammahim.meemo.presentation.screens.CapabilitiesAndDiagnosticsScreen
import com.marufallammahim.meemo.presentation.screens.CapabilitiesViewModel
import com.marufallammahim.meemo.presentation.screens.InstalledAppsScreen
import com.marufallammahim.meemo.presentation.screens.MemoryAliasesViewModel
import com.marufallammahim.meemo.presentation.screens.MemoryAndAliasesScreen
import com.marufallammahim.meemo.presentation.screens.OnboardingScreen
import com.marufallammahim.meemo.presentation.screens.OnboardingViewModel
import com.marufallammahim.meemo.presentation.screens.PrivacyAndSettingsScreen
import com.marufallammahim.meemo.presentation.screens.SettingsViewModel

@Composable
fun MeemoNavHost(
    onboardingCompleted: Boolean,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    val startDestination: MeemoDestination = if (onboardingCompleted) {
        MeemoDestination.AssistantHome
    } else {
        MeemoDestination.Onboarding
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (onboardingCompleted) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTabIndex == 0,
                        onClick = {
                            selectedTabIndex = 0
                            navController.navigate(MeemoDestination.AssistantHome) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_assistant)) }
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            navController.navigate(MeemoDestination.InstalledApps) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_apps)) }
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 2,
                        onClick = {
                            selectedTabIndex = 2
                            navController.navigate(MeemoDestination.MemoryAliases) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Memory, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_memory)) }
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 3,
                        onClick = {
                            selectedTabIndex = 3
                            navController.navigate(MeemoDestination.CapabilitiesDiagnostics) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_capabilities)) }
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 4,
                        onClick = {
                            selectedTabIndex = 4
                            navController.navigate(MeemoDestination.PrivacySettings) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<MeemoDestination.Onboarding> {
                val vm: OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    viewModel = vm,
                    onFinishOnboarding = {
                        navController.navigate(MeemoDestination.AssistantHome) {
                            popUpTo(MeemoDestination.Onboarding) { inclusive = true }
                        }
                    }
                )
            }
            composable<MeemoDestination.AssistantHome> {
                val vm: AssistantViewModel = hiltViewModel()
                AssistantHomeScreen(
                    viewModel = vm,
                    onRequestMicPermission = onRequestMicPermission
                )
            }
            composable<MeemoDestination.InstalledApps> {
                val vm: AppsViewModel = hiltViewModel()
                InstalledAppsScreen(viewModel = vm)
            }
            composable<MeemoDestination.MemoryAliases> {
                val vm: MemoryAliasesViewModel = hiltViewModel()
                MemoryAndAliasesScreen(viewModel = vm)
            }
            composable<MeemoDestination.CapabilitiesDiagnostics> {
                val vm: CapabilitiesViewModel = hiltViewModel()
                CapabilitiesAndDiagnosticsScreen(viewModel = vm)
            }
            composable<MeemoDestination.PrivacySettings> {
                val vm: SettingsViewModel = hiltViewModel()
                PrivacyAndSettingsScreen(viewModel = vm)
            }
        }
    }
}
