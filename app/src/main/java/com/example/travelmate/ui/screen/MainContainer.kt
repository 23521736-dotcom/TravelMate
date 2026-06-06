package com.example.travelmate.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.travelmate.ui.screen.components.ChatFloatingWindow
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.viewmodel.AuthViewModel
import com.example.travelmate.ui.theme.BluePrimary
import com.example.travelmate.ui.theme.GrayText

import com.example.travelmate.ui.navigation.Screen as AppScreen

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : NavItem("home", "Home", Icons.Default.Home)
    object Explore : NavItem("explore", "Explore", Icons.Default.Search)
    object Trips : NavItem("trips", "Trips", Icons.Default.LocationOn)
    object Expense : NavItem("expense", "Expense", Icons.AutoMirrored.Filled.ReceiptLong)
    object Profile : NavItem("profile", "Profile", Icons.Default.PersonOutline)
}

@Composable
fun MainContainer(
    onLogoutClick: () -> Unit,
    onPlanTripClick: () -> Unit,
    onTripClick: (String) -> Unit,
    onEditProfileClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onDestinationClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var selectedScreen by remember { mutableStateOf<NavItem>(NavItem.Home) }
    var isChatVisible by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.White,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    NavItem.Home,
                    NavItem.Explore,
                    NavItem.Trips,
                    NavItem.Expense,
                    NavItem.Profile
                )
                items.forEach { item ->
                    val isSelected = selectedScreen == item
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = { selectedScreen = item },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            unselectedIconColor = GrayText,
                            unselectedTextColor = GrayText,
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isChatVisible = !isChatVisible },
                containerColor = BluePrimary,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(
                    if (isChatVisible) Icons.Default.Close else Icons.Default.AutoAwesome,
                    contentDescription = "AI Chat"
                )
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (selectedScreen) {
                is NavItem.Home -> HomeScreen(
                    onProfileClick = { selectedScreen = NavItem.Profile },
                    onNotificationsClick = onNotificationsClick,
                    onDestinationClick = onDestinationClick,
                    onTripClick = onTripClick,
                    authViewModel = authViewModel
                )
                is NavItem.Explore -> ExploreScreen(
                    onDestinationClick = onDestinationClick,
                    authViewModel = authViewModel
                )
                is NavItem.Trips -> MyTripsScreen(onAddTripClick = onPlanTripClick, onTripClick = onTripClick)
                is NavItem.Expense -> ExpenseScreen()
                is NavItem.Profile -> ProfileScreen(
                    onEditProfileClick = onEditProfileClick,
                    onWishlistClick = onWishlistClick,
                    onSettingsClick = onSettingsClick,
                    onLogoutClick = onLogoutClick,
                    viewModel = authViewModel
                )
                else -> PlaceholderScreen("Coming Soon")
            }
        }
        
        ChatFloatingWindow(
            isVisible = isChatVisible,
            onClose = { isChatVisible = false }
        )
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = "$name Screen (Coming Soon)")
    }
}
