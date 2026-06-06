package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.domain.model.Notification
import com.example.travelmate.ui.screen.components.formatTimestamp
import com.example.travelmate.ui.screen.components.getNotifConfig
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.NotificationViewModel
import androidx.compose.foundation.lazy.items

@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifications = uiState.notifications

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.clip(CircleShape).background(Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Text(text = "Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.markAllAsRead() }) {
                Text(text = "Mark all read", color = BluePrimary, fontSize = 13.sp)
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BluePrimary)
            }
        } else if (notifications.isEmpty()) {
            EmptyNotificationsPlaceholder()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Simplified grouping for now
                item {
                    Text(text = "RECENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrayText, modifier = Modifier.padding(vertical = 12.dp))
                }
                items(notifications) { notif ->
                    NotificationItem(notif, onClick = { onNotificationClick(notif.id) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notif: Notification, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = if (!notif.isRead) androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.1f)) else null,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val config = getNotifConfig(notif.type)
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = config.bgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(config.icon, contentDescription = null, tint = config.tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = notif.message, color = GrayText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                Text(
                    text = formatTimestamp(notif.timestamp),
                    color = GrayText.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (!notif.isRead) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BluePrimary))
            }
        }
    }
}

@Composable
fun EmptyNotificationsPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(80.dp), tint = GrayBorder)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Bạn không có thông báo nào", color = GrayText)
    }
}
