package com.example.travelmate.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelmate.ui.screen.components.AuthHeader
import com.example.travelmate.ui.theme.GrayBackground
import com.example.travelmate.ui.theme.GrayText
import com.example.travelmate.ui.theme.GreenHeader
import com.example.travelmate.ui.theme.GreenLight

@Composable
fun EmailSentScreen(
    onBackClick: () -> Unit,
    onResendClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AuthHeader(
            title = "Xác thực OTP",
            subtitle = "Nhập mã 6 số từ email của bạn",
            backgroundColor = GreenHeader,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Checkmark
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = GreenHeader,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Email đã được gửi!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vui lòng nhập mã OTP để xác thực tài khoản",
                fontSize = 14.sp,
                color = GrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Steps
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GrayBackground
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Các bước tiếp theo:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    
                    StepItem(number = "1", text = "Mở email bạn vừa nhận")
                    StepItem(number = "2", text = "Lấy mã OTP 6 chữ số")
                    StepItem(number = "3", text = "Nhập mã vào ô xác thực trong ứng dụng")
                    StepItem(number = "4", text = "Tài khoản sẽ được kích hoạt ngay lập tức")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onResendClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GreenHeader)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = GreenHeader)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Gửi lại email", color = GreenHeader, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StepItem(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, color = GreenHeader, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp)
    }
}
