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
import androidx.compose.ui.text.style.TextAlign
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
    var confirmPassword by remember { mutableStateOf("") }
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
            val stepState = when {
                uiState.isOtpVerified -> 2
                uiState.isOtpSent -> 1
                else -> 0
            }

            AnimatedContent(targetState = stepState, label = "ForgotPassFlow") { step ->
                Column {
                    when (step) {
                        0 -> {
                            Text(
                                text = "Đừng lo lắng! Nhập địa chỉ email đã đăng ký và chúng tôi sẽ gửi mã OTP xác thực để bạn đặt lại mật khẩu.",
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
                                icon = Icons.Default.MarkEmailRead,
                                title = "Nhận mã OTP qua email",
                                description = "Chúng tôi sẽ gửi mã xác thực 6 số vào email của bạn",
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = GreenHeader
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Mã xác thực đã được gửi!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenHeader
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Vui lòng nhập mã OTP 6 số đã được gửi đến $email",
                                    fontSize = 14.sp,
                                    color = GrayText,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))

                                AuthTextField(
                                    value = otp,
                                    onValueChange = { if (it.length <= 6) otp = it },
                                    label = "Mã OTP",
                                    placeholder = "123456",
                                    leadingIcon = Icons.Default.Pin,
                                    keyboardType = KeyboardType.Number
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                if (uiState.isLoading) {
                                    CircularProgressIndicator()
                                } else {
                                    AuthButton(
                                        text = "Xác nhận mã OTP",
                                        onClick = { if (otp.length == 6) viewModel.verifyOTP(email, otp) },
                                        color = GreenHeader
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = "Không nhận được mã? Gửi lại",
                                        color = BluePrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.forgotPassword(email) }
                                    )
                                }
                            }
                        }
                        2 -> {
                            Column {
                                Text(
                                    text = "Mã OTP đã được xác thực thành công. Vui lòng đặt mật khẩu mới cho tài khoản của bạn.",
                                    fontSize = 14.sp,
                                    color = GrayText,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )

                                AuthTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = "Mật khẩu mới",
                                    placeholder = "Tối thiểu 8 ký tự",
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true
                                )

                                AuthTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = "Xác nhận mật khẩu",
                                    placeholder = "Nhập lại mật khẩu mới",
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                } else {
                                    AuthButton(
                                        text = "Đổi mật khẩu",
                                        onClick = { 
                                            if (newPassword.length < 8) {
                                                viewModel.setError("Mật khẩu phải có ít nhất 8 ký tự")
                                            } else if (newPassword == confirmPassword) {
                                                viewModel.resetPassword(email, newPassword)
                                            } else {
                                                viewModel.setError("Mật khẩu xác nhận không khớp")
                                            }
                                        },
                                        color = GreenHeader
                                    )
                                }
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
