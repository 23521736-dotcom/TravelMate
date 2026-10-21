package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onPolicyClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()

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
                text = "Cài đặt",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "GIAO DIỆN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GrayText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            SettingsSwitchItem(
                icon = Icons.Default.DarkMode,
                title = "Chế độ tối (Dark Mode)",
                checked = isDarkMode,
                onCheckedChange = { viewModel.toggleDarkMode(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CƠ SỞ DỮ LIỆU",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GrayText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val authViewModel: com.example.travelmate.ui.viewmodel.AuthViewModel = hiltViewModel()
            val authUiState by authViewModel.uiState.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            SettingsMenuItem(
                icon = Icons.Default.Place,
                title = "Nạp địa danh & Đánh giá mẫu",
                subtitle = if (authUiState.isLoading) "Đang tải dữ liệu..." else "50 Địa danh Việt Nam & Các đánh giá mẫu",
                onClick = {
                    authViewModel.seedDestinationsAndReviews {
                        android.widget.Toast.makeText(context, "Đã nạp 50 địa danh và đánh giá mẫu vào Firestore!", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            )

            SettingsMenuItem(
                icon = Icons.Default.CloudUpload,
                title = "Nạp toàn bộ dữ liệu mẫu",
                subtitle = if (authUiState.isLoading) "Đang tải dữ liệu..." else "Người dùng, Chuyến đi, Chi tiêu & Địa danh",
                onClick = {
                    authViewModel.seedDatabase {
                        android.widget.Toast.makeText(context, "Đã nạp thành công toàn bộ dữ liệu vào Firestore!", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ỨNG DỤNG",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GrayText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SettingsMenuItem(
                icon = Icons.Default.Language,
                title = "Ngôn ngữ",
                subtitle = "Tiếng Việt",
                onClick = { }
            )
            
            SettingsMenuItem(
                icon = Icons.Default.Description,
                title = "Điều khoản & Chính sách",
                onClick = onPolicyClick
            )

            SettingsMenuItem(
                icon = Icons.Default.Info,
                title = "Phiên bản",
                subtitle = "1.0.0",
                onClick = { },
                isLast = true
            )
        }
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isLast: Boolean = false
) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(text = subtitle, fontSize = 13.sp, color = GrayText)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GrayText)
        }
        if (!isLast) {
            HorizontalDivider(color = GrayBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BluePrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = GrayBorder
            )
        )
    }
}
