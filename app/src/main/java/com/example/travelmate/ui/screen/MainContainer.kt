package com.example.travelmate.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.travelmate.ui.screen.components.ChatFloatingWindow
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.viewmodel.AuthViewModel
import com.example.travelmate.ui.theme.BluePrimary
import com.example.travelmate.ui.theme.GrayText

import com.example.travelmate.ui.navigation.Screen as AppScreen

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput

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
    onMyReviewsClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onDestinationClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onJoinTripClick: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var selectedScreen by rememberSaveable { mutableStateOf<String>(NavItem.Home.route) }
    var isChatVisible by remember { mutableStateOf(false) }
    
    // FAB position state
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

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
                    val isSelected = selectedScreen == item.route
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = { selectedScreen = item.route },
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
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
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
                NavItem.Home.route -> HomeScreen(
                    onProfileClick = { selectedScreen = NavItem.Profile.route },
                    onNotificationsClick = onNotificationsClick,
                    onSearchClick = { selectedScreen = NavItem.Explore.route },
                    onDestinationClick = onDestinationClick,
                    onTripClick = onTripClick,
                    authViewModel = authViewModel
                )
                NavItem.Explore.route -> ExploreScreen(
                    onDestinationClick = onDestinationClick,
                    authViewModel = authViewModel
                )
                NavItem.Trips.route -> MyTripsScreen(
                    onAddTripClick = onPlanTripClick, 
                    onJoinTripClick = onJoinTripClick,
                    onTripClick = onTripClick
                )
                NavItem.Expense.route -> ExpenseScreen(onAddExpenseClick = { onAddExpenseClick() })
                NavItem.Profile.route -> ProfileScreen(
                    onEditProfileClick = onEditProfileClick,
                    onWishlistClick = onWishlistClick,
                    onMyReviewsClick = onMyReviewsClick,
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
