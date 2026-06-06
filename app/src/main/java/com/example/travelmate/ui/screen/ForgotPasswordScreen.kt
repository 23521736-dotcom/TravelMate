package com.example.travelmate.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.screen.components.AuthButton
import com.example.travelmate.ui.screen.components.AuthHeader
import com.example.travelmate.ui.screen.components.AuthTextField
import com.example.travelmate.ui.screen.components.InfoCard
import com.example.travelmate.ui.theme.BluePrimary
import com.example.travelmate.ui.theme.GrayText
import com.example.travelmate.ui.theme.GreenHeader
import com.example.travelmate.ui.theme.GreenLight
import com.example.travelmate.ui.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onSendClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isPasswordReset) {
        if (uiState.isPasswordReset) {
            onSendClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AuthHeader(
            title = "Quên mật khẩu?",
            subtitle = if (uiState.isOtpSent) "Xác thực mã OTP" else "Nhập email để khôi phục tài khoản",
            backgroundColor = GreenHeader,
            onBackClick = onBackClick,
            showLogo = false
        )

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            AnimatedContent(targetState = when {
                uiState.isOtpVerified -> 2
                uiState.isOtpSent -> 1
                else -> 0
            }, label = "ForgotPassFlow") { step ->
                Column {
                    when (step) {
                        0 -> {
                            Text(
                                text = "Đừng lo lắng! Nhập địa chỉ email đã đăng ký và chúng tôi sẽ gửi mã OTP đặt lại mật khẩu cho bạn.",
                                fontSize = 14.sp,
                                color = GrayText,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            AuthTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Địa chỉ email",
                                placeholder = "your@email.com",
                                leadingIcon = Icons.Default.Email,
                                keyboardType = KeyboardType.Email
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            InfoCard(
                                icon = Icons.Default.MarkEmailUnread,
                                title = "Nhận mã OTP",
                                description = "Chúng tôi sẽ gửi mã OTP 6 số vào email của bạn",
                                backgroundColor = GreenLight
                            )
                            InfoCard(
                                icon = Icons.Default.Schedule,
                                title = "Hiệu lực 5 phút",
                                description = "Mã OTP có hiệu lực trong 5 phút kể từ khi nhận được",
                                backgroundColor = GreenLight
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else {
                                AuthButton(
                                    text = "Gửi mã OTP",
                                    onClick = { viewModel.forgotPassword(email) },
                                    color = GreenHeader
                                )
                            }
                        }
                        1 -> {
                            Text(
                                text = "Mã OTP đã được gửi đến $email. Vui lòng kiểm tra hộp thư của bạn.",
                                fontSize = 14.sp,
                                color = GrayText,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            AuthTextField(
                                value = otp,
                                onValueChange = { if (it.length <= 6) otp = it },
                                label = "Mã OTP",
                                placeholder = "123456",
                                leadingIcon = Icons.Default.LockClock,
                                keyboardType = KeyboardType.Number
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else {
                                AuthButton(
                                    text = "Xác nhận OTP",
                                    onClick = { viewModel.verifyOTP(email, otp) },
                                    color = GreenHeader
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Không nhận được mã? Gửi lại",
                                    color = BluePrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .clickable { viewModel.forgotPassword(email) }
                                )
                            }
                        }
                        2 -> {
                            Text(
                                text = "Xác thực thành công! Vui lòng nhập mật khẩu mới của bạn.",
                                fontSize = 14.sp,
                                color = GrayText,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            AuthTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = "Mật khẩu mới",
                                placeholder = "********",
                                leadingIcon = Icons.Default.Lock,
                                keyboardType = KeyboardType.Password,
                                isPassword = true
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else {
                                AuthButton(
                                    text = "Đổi mật khẩu",
                                    onClick = { viewModel.resetPassword(email, newPassword) },
                                    color = GreenHeader
                                )
                            }
                        }
                    }

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Nhớ mật khẩu rồi? ", color = GrayText)
                Text(
                    text = "Đăng nhập",
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
        }
    }
}
