package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelmate.ui.theme.GrayText

@Composable
fun PolicyScreen(onBackClick: () -> Unit) {
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
                text = "Điều khoản & Chính sách",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PolicySection(
                title = "1. Điều khoản sử dụng",
                content = "Chào mừng bạn đến với TravelMate. Bằng việc sử dụng ứng dụng, bạn đồng ý tuân thủ các điều khoản sau đây...\n\nỨng dụng này cung cấp các công cụ để lập kế hoạch chuyến đi, quản lý ngân sách và gợi ý địa danh dựa trên sở thích cá nhân."
            )
            
            PolicySection(
                title = "2. Chính sách bảo mật",
                content = "Chúng tôi tôn trọng quyền riêng tư của bạn. Dữ liệu cá nhân (email, tên, sở thích) được thu thập chỉ nhằm mục đích cải thiện trải nghiệm người dùng và cá nhân hóa các gợi ý du lịch.\n\nDữ liệu của bạn được lưu trữ an toàn trên hệ thống Firebase và không được chia sẻ với bên thứ ba mà không có sự đồng ý của bạn."
            )

            PolicySection(
                title = "3. Trách nhiệm người dùng",
                content = "Người dùng có trách nhiệm bảo mật thông tin tài khoản của mình. Mọi hoạt động diễn ra dưới tài khoản của bạn sẽ thuộc trách nhiệm của bạn."
            )

            PolicySection(
                title = "4. Cập nhật chính sách",
                content = "Chúng tôi có thể cập nhật các điều khoản này theo thời gian. Việc bạn tiếp tục sử dụng ứng dụng đồng nghĩa với việc chấp nhận các thay đổi đó."
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
