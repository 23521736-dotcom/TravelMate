package com.example.travelmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.travelmate.ui.screen.*
import com.example.travelmate.ui.viewmodel.AuthViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()

    // Determine initial route once to avoid NavHost recreation
    val startRoute = remember(authUiState.isCheckingSession, authUiState.user, authUiState.isAwaitingEmailVerification) {
        if (authUiState.isCheckingSession) null
        else if (authUiState.user != null && !authUiState.isAwaitingEmailVerification) Screen.Main.route
        else Screen.Login.route
    }

    LaunchedEffect(authUiState.isCheckingSession, authUiState.user, startRoute) {
        android.util.Log.d("NavGraph", "isCheckingSession: ${authUiState.isCheckingSession}, user: ${authUiState.user?.email}, startRoute: $startRoute")
    }

    if (startRoute == null) {
        SplashScreen()
    } else {
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = modifier
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginClick = { 
                        val user = authViewModel.uiState.value.user
                        android.util.Log.d("NavGraph", "onLoginClick: user=$user, isFirstTime=${authUiState.isFirstTime}")
                        when {
                            authUiState.isFirstTime -> {
                                navController.navigate(Screen.Onboarding.route)
                            }
                            user != null && user.interests.isEmpty() -> {
                                navController.navigate(Screen.InterestSelection.route)
                            }
                            else -> {
                                navController.navigate(Screen.Main.route)
                            }
                        }
                    },
                    onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) },
                    onSignUpClick = { navController.navigate(Screen.SignUp.route) }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onNext = { 
                        authViewModel.completeOnboarding()
                        val user = authViewModel.uiState.value.user
                        if (user != null && user.interests.isEmpty()) {
                            navController.navigate(Screen.InterestSelection.route)
                        } else {
                            navController.navigate(Screen.Main.route)
                        }
                    }
                )
            }
            composable(Screen.InterestSelection.route) {
                InterestSelectionScreen(
                    viewModel = authViewModel,
                    onComplete = { 
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    viewModel = authViewModel,
                    onBackClick = { navController.popBackStack() },
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onSignUpComplete = { 
                        // Do nothing, stay on SignUpScreen to show OTP input
                    }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSendClick = { navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    } },
                    onLoginClick = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.EmailSent.route) {
                EmailSentScreen(
                    onBackClick = { navController.popBackStack() },
                    onResendClick = { 
                        authViewModel.uiState.value.user?.email?.let { email ->
                            authViewModel.resendVerificationEmail(email)
                        }
                    }
                )
            }
            composable(Screen.Main.route) {
                MainContainer(
                    authViewModel = authViewModel,
                    onLogoutClick = { 
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onPlanTripClick = { navController.navigate(Screen.PlanTrip.route) },
                    onTripClick = { id -> 
                        navController.navigate(Screen.TripDetail.createRoute(id))
                    },
                    onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                    onWishlistClick = { navController.navigate(Screen.Wishlist.route) },
                    onAddExpenseClick = { navController.navigate(Screen.AddExpense.createRoute()) },
                    onDestinationClick = { id -> 
                        navController.navigate(Screen.DestinationDetail.createRoute(id))
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onJoinTripClick = {
                        navController.navigate(Screen.JoinTrip.route)
                    }
                )
            }
            composable(Screen.JoinTrip.route) {
                JoinTripScreen(
                    onBackClick = { navController.popBackStack() },
                    onJoinSuccess = { tripId ->
                        navController.navigate(Screen.TripDetail.createRoute(tripId)) {
                            popUpTo(Screen.JoinTrip.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onPolicyClick = { navController.navigate(Screen.Policy.route) }
                )
            }
            composable(Screen.Wishlist.route) {
                WishlistScreen(
                    authViewModel = authViewModel,
                    onBackClick = { navController.popBackStack() },
                    onDestinationClick = { id -> 
                        navController.navigate(Screen.DestinationDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.Policy.route) {
                PolicyScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    viewModel = authViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(Screen.PlanTrip.route + "?destinationId={destinationId}") { backStackEntry ->
                val destinationId = backStackEntry.arguments?.getString("destinationId")
                PlanTripScreen(
                    onBackClick = { navController.popBackStack() },
                    onComplete = { tripId -> 
                        navController.navigate(Screen.TripDetail.createRoute(tripId)) {
                            popUpTo(Screen.PlanTrip.route) { inclusive = true }
                        }
                    },
                    destinationId = destinationId
                )
            }
            composable(
                route = Screen.TripDetail.route,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                TripDetailScreen(
                    tripId = tripId,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigate(Screen.EditTrip.createRoute(tripId)) },
                    onManageMembersClick = { navController.navigate(Screen.ManageMembers.createRoute(tripId)) },
                    onHistoryClick = { navController.navigate(Screen.TripHistory.createRoute(tripId)) },
                    onAddExpenseClick = { navController.navigate(Screen.AddExpense.createRoute(tripId)) },
                    onDeleteTripSuccess = { navController.popBackStack(Screen.Main.route, false) }
                )
            }
            composable(
                route = Screen.ManageMembers.route,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                ManageMembersScreen(
                    tripId = tripId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.TripHistory.route,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                TripHistoryScreen(
                    tripId = tripId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.AddExpense.route,
                arguments = listOf(navArgument("tripId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId")
                AddExpenseScreen(
                    tripId = tripId,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.EditTrip.route,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                EditTripScreen(
                    tripId = tripId,
                    onCloseClick = { navController.popBackStack() },
                    onSaveClick = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.DestinationDetail.route,
                arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
                DestinationDetailScreen(
                    destinationId = destinationId,
                    onBackClick = { navController.popBackStack() },
                    onPlanTripClick = { 
                        navController.navigate(Screen.PlanTrip.route + "?destinationId=$destinationId")
                    },
                    onWriteReviewClick = { id ->
                        navController.navigate(Screen.WriteReview.createRoute(id))
                    }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationScreen(
                    onBackClick = { navController.popBackStack() },
                    onNotificationClick = { id ->
                        navController.navigate(Screen.NotificationDetail.createRoute(id))
                    }
                )
            }
            composable(
                route = Screen.NotificationDetail.route,
                arguments = listOf(navArgument("notificationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val notificationId = backStackEntry.arguments?.getString("notificationId") ?: ""
                NotificationDetailScreen(
                    notificationId = notificationId,
                    onBackClick = { navController.popBackStack() },
                    onActionClick = { type, id ->
                        if (id != null) {
                            when(type) {
                                "trip_start", "budget", "role_change" -> navController.navigate(Screen.TripDetail.createRoute(id))
                                "destination" -> navController.navigate(Screen.DestinationDetail.createRoute(id))
                                "explore", "promotion" -> {
                                    if (id.startsWith("landmark_")) {
                                        navController.navigate(Screen.DestinationDetail.createRoute(id))
                                    } else {
                                        navController.navigate(Screen.Main.route)
                                    }
                                }
                                else -> navController.navigate(Screen.Main.route)
                            }
                        }
                    }
                )
            }
            composable(
                route = Screen.WriteReview.route,
                arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
                WriteReviewScreen(
                    destinationId = destinationId,
                    onBackClick = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }
        }
    }
}
