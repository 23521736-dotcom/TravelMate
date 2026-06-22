package com.example.travelmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.travelmate.ui.navigation.NavGraph
import com.example.travelmate.ui.theme.TravelMateTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.travelmate.ui.viewmodel.MainViewModel

import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme

import com.example.travelmate.ui.screen.BlockedScreen
import com.example.travelmate.ui.viewmodel.AuthViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val user by viewModel.currentUser.collectAsState()
            
            TravelMateTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (user?.isBlocked == true) {
                        BlockedScreen(
                            reason = user?.blockedReason,
                            onLogout = { authViewModel.logout() }
                        )
                    } else {
                        val navController = rememberNavController()
                        Box(modifier = Modifier.safeDrawingPadding()) {
                            NavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
