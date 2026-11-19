package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.symphine.gamespace.ui.navigation.SymphineAppRoot
import com.symphine.gamespace.ui.state.SymphineMainViewModel
import com.symphine.gamespace.ui.theme.SymphineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val mainViewModel: SymphineMainViewModel = viewModel()
            val uiSettings by mainViewModel.uiSettings.collectAsStateWithLifecycle()

            SymphineTheme(uiSettings = uiSettings) {
                SymphineAppRoot(viewModel = mainViewModel)
            }
        }
    }
}
