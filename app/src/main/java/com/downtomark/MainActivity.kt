package com.downtomark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.downtomark.navigation.AppNavigation
import com.downtomark.ui.theme.AppTheme
import com.downtomark.ui.theme.DownToMarkTheme

class MainActivity : ComponentActivity() {

    companion object {
        var currentTheme by mutableStateOf(AppTheme.EVERFOREST_DARK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DownToMarkApp
        currentTheme = app.repository.loadThemePreference()

        setContent {
            DownToMarkTheme(appTheme = currentTheme) {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}
