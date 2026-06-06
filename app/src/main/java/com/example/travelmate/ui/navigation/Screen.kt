package com.example.travelmate.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object EmailSent : Screen("email_sent")
    object Onboarding : Screen("onboarding")
    object InterestSelection : Screen("interest_selection")
    object Wishlist : Screen("wishlist")
    object Main : Screen("main")
    object EditProfile : Screen("edit_profile")
    object PlanTrip : Screen("plan_trip")
    object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: String) = "trip_detail/$tripId"
    }
    object AddExpense : Screen("add_expense")
    object EditTrip : Screen("edit_trip")
    object DestinationDetail : Screen("destination_detail/{destinationId}") {
        fun createRoute(destinationId: String) = "destination_detail/$destinationId"
    }
    object Notifications : Screen("notifications")
    object NotificationDetail : Screen("notification_detail/{notificationId}") {
        fun createRoute(notificationId: String) = "notification_detail/$notificationId"
    }
    object Settings : Screen("settings")
    object Policy : Screen("policy")
}
