package com.marufallammahim.meemo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marufallammahim.meemo.domain.model.UserPreferences
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.presentation.navigation.MeemoNavHost
import com.marufallammahim.meemo.presentation.theme.MeemoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repository: MeemoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by repository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = UserPreferences(onboardingCompleted = true)
            )
            var micPermissionResult by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                micPermissionResult?.invoke(permissions[Manifest.permission.RECORD_AUDIO] == true)
                micPermissionResult = null
            }

            MeemoTheme(themeMode = prefs.themeMode) {
                MeemoNavHost(
                    onboardingCompleted = prefs.onboardingCompleted,
                    onRequestMicPermission = { onResult ->
                        micPermissionResult = onResult
                        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    }
                )
            }
        }
    }
}
