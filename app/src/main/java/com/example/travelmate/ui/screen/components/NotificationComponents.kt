package com.example.travelmate.ui.screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.travelmate.ui.theme.BluePrimary
import com.example.travelmate.ui.theme.GrayText
import com.example.travelmate.ui.theme.OrangePrimary
import com.example.travelmate.ui.theme.PurplePrimary
import java.text.SimpleDateFormat
import java.util.*

data class NotifConfig(val icon: ImageVector, val bgColor: Color, val tint: Color)

@Composable
fun getNotifConfig(type: String): NotifConfig {
    return when(type) {
        "trip_start" -> NotifConfig(Icons.Default.FlightTakeoff, Color(0xFFE3F2FD), BluePrimary)
        "budget" -> NotifConfig(Icons.Default.Paid, Color(0xFFFFF3E0), OrangePrimary)
        "welcome" -> NotifConfig(Icons.Default.NotificationsActive, Color(0xFFF3E5F5), PurplePrimary)
        "promotion" -> NotifConfig(Icons.Default.LocalOffer, Color(0xFFE8F5E9), Color(0xFF43A047))
        "system_alert" -> NotifConfig(Icons.Default.Error, Color(0xFFFFEBEE), Color(0xFFD32F2F))
        else -> NotifConfig(Icons.Default.Info, Color(0xFFF5F5F5), GrayText)
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm, dd MMM", Locale.US)
    return sdf.format(Date(timestamp))
}
