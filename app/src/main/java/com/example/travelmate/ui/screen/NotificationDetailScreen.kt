package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.screen.components.formatTimestamp
import com.example.travelmate.ui.screen.components.getNotifConfig
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.NotificationViewModel

@Composable
fun NotificationDetailScreen(
    notificationId: String,
    onBackClick: () -> Unit,
    onActionClick: (String, String?) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notification = uiState.selectedNotification

    LaunchedEffect(notificationId) {
        viewModel.fetchNotificationDetail(notificationId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Chi tiết thông báo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (notification == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BluePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                val config = getNotifConfig(notification.type)
                
                Surface(
                    modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally),
                    shape = CircleShape,
                    color = config.bgColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(config.icon, contentDescription = null, tint = config.tint, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = notification.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Text(
                    text = formatTimestamp(notification.timestamp),
                    fontSize = 13.sp,
                    color = GrayText,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = notification.message,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (notification.tripId != null || notification.actionUrl != null) {
                    Button(
                        onClick = { onActionClick(notification.type, notification.tripId ?: notification.actionUrl) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text(
                            text = when(notification.type) {
                                "trip_start" -> "Xem chuyến đi"
                                "budget" -> "Quản lý chi tiêu"
                                "explore", "promotion" -> "Khám phá ngay"
                                "destination" -> "Xem địa điểm"
                                else -> "Xem chi tiết"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
