package com.ltebandslock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ltebandslock.ui.MainViewModel
import com.ltebandslock.ui.screens.DashboardScreen
import com.ltebandslock.ui.theme.LTEBandsLockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LTEBandsLockTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
