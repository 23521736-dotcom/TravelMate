package com.example.travelmate.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
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
import com.example.travelmate.ui.screen.components.SocialButton
import com.example.travelmate.ui.theme.BlueHeader
import com.example.travelmate.ui.theme.BluePrimary
import com.example.travelmate.ui.theme.GrayText
import com.example.travelmate.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AuthHeader(
            title = "TravelMate",
            subtitle = "Chào mừng trở lại!",
            backgroundColor = BlueHeader,
            showLogo = true
        )

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Đăng nhập",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tiếp tục hành trình của bạn",
                fontSize = 14.sp,
                color = GrayText,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "your@email.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mật khẩu",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Quên mật khẩu?",
                    color = BluePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onForgotPasswordClick() }
                )
            }
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "",
                placeholder = "Nhập mật khẩu",
                leadingIcon = Icons.Default.Lock,
                isPassword = true
            )

            if (uiState.error != null) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                    
                    if (uiState.error!!.contains("xác thực email")) {
                        Text(
                            text = "Gửi lại email xác thực",
                            color = BluePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { viewModel.resendVerificationEmail(email) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                AuthButton(
                    text = "Đăng nhập",
                    onClick = { viewModel.login(email, password) },
                    color = BluePrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            /* 
            Text(
                text = "hoặc tiếp tục với",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = GrayText,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SocialButton(icon = Icons.Default.Public)
                Spacer(modifier = Modifier.width(16.dp))
                SocialButton(icon = Icons.Default.Language)
            }
            */

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Chưa có tài khoản? ", color = GrayText)
                Text(
                    text = "Đăng ký",
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSignUpClick() }
                )
            }
        }
    }
}
