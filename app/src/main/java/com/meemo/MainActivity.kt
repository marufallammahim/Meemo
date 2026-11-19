package com.meemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.meemo.core.ui.theme.MeemoTheme
import com.meemo.domain.usecase.ProcessCommandUseCase
import com.meemo.navigation.MeemoNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var processCommandUseCase: ProcessCommandUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeemoTheme {
                MeemoNavGraph(processCommandUseCase)
            }
        }
    }
}
