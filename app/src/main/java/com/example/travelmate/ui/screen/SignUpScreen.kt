package com.example.travelmate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.ui.screen.components.AuthButton
import com.example.travelmate.ui.screen.components.AuthHeader
import com.example.travelmate.ui.screen.components.AuthTextField
import com.example.travelmate.ui.screen.components.InfoCard
import com.example.travelmate.ui.theme.*
import com.example.travelmate.ui.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSignUpComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(1) }
    
    // Step 1 data
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    // Step 2 data
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isRegistrationComplete) {
        if (uiState.isRegistrationComplete) {
            onLoginClick()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopEmailVerificationPolling()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AuthHeader(
            title = if (step == 1) "Tạo tài khoản" else if (uiState.isAwaitingEmailVerification) "Xác thực" else "Bảo mật",
            subtitle = if (step == 1) "Thông tin cá nhân của bạn" else if (uiState.isAwaitingEmailVerification) "Nhập mã OTP từ email" else "Đặt mật khẩu an toàn",
            backgroundColor = OrangeHeader,
            onBackClick = { 
                if (uiState.isAwaitingEmailVerification) {
                    viewModel.resetState()
                } else if (step == 2) {
                    step = 1 
                } else {
                    onBackClick()
                }
            },
            showLogo = false
        )

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            if (uiState.isAwaitingEmailVerification) {
                var otpValue by remember { mutableStateOf("") }
                EmailVerificationContent(
                    email = email,
                    otpValue = otpValue,
                    onOtpChange = { if (it.length <= 6) otpValue = it },
                    onVerifyOtp = { viewModel.verifyRegistrationOTP(email, otpValue) },
                    onResendEmail = { viewModel.resendRegistrationEmail(email) },
                    onLoginClick = onLoginClick,
                    isLoading = uiState.isLoading,
                    error = uiState.error
                )
            } else {
                Text(
                    text = "Bước $step/2",
                    color = GrayText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (step == 1) {
                    StepOneContent(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        email = email,
                        onEmailChange = { email = it },
                        onNext = { step = 2 },
                        onLoginClick = onLoginClick
                    )
                } else {
                    StepTwoContent(
                        password = password,
                        onPasswordChange = { password = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        agreeToTerms = agreeToTerms,
                        onAgreeChange = { agreeToTerms = it },
                        onSignUp = { viewModel.signUp(email, password, fullName) },
                        onLoginClick = onLoginClick,
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun EmailVerificationContent(
    email: String,
    otpValue: String,
    onOtpChange: (String) -> Unit,
    onVerifyOtp: () -> Unit,
    onResendEmail: () -> Unit,
    onLoginClick: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = OrangeHeader
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Xác thực tài khoản",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mã OTP đã được gửi đến:",
            color = GrayText,
            fontSize = 14.sp
        )
        Text(
            text = email,
            fontWeight = FontWeight.Bold,
            color = BluePrimary,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        AuthTextField(
            value = otpValue,
            onValueChange = onOtpChange,
            label = "Mã OTP",
            placeholder = "123456",
            leadingIcon = Icons.Default.LockClock,
            keyboardType = KeyboardType.Number
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = OrangeHeader)
        } else {
            AuthButton(
                text = "Xác nhận OTP",
                onClick = onVerifyOtp,
                color = OrangeHeader
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Không nhận được mã? Gửi lại",
            color = BluePrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onResendEmail() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onLoginClick) {
            Text(text = "Quay lại Đăng nhập", color = BluePrimary)
        }
    }
}

@Composable
fun StepOneContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    onNext: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column {
        AuthTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = "Họ và tên",
            placeholder = "Nguyễn Văn A",
            leadingIcon = Icons.Default.Person
        )

        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "your@email.com",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallInfoCard(icon = Icons.Default.Map, text = "Lên kế hoạch trip", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            SmallInfoCard(icon = Icons.Default.Paid, text = "Quản lý chi tiêu", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallInfoCard(icon = Icons.Default.SmartToy, text = "AI tư vấn lịch trình", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            SmallInfoCard(icon = Icons.Default.PushPin, text = "Khám phá địa điểm", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        AuthButton(
            text = "Tiếp theo",
            onClick = onNext,
            color = OrangeHeader
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Đã có tài khoản? ", color = GrayText)
            Text(
                text = "Đăng nhập",
                color = BluePrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLoginClick() }
            )
        }
    }
}

@Composable
fun StepTwoContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    agreeToTerms: Boolean,
    onAgreeChange: (Boolean) -> Unit,
    onSignUp: () -> Unit,
    onLoginClick: () -> Unit,
    isLoading: Boolean = false
) {
    Column {
        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Mật khẩu",
            placeholder = "Tối thiểu 8 ký tự",
            leadingIcon = Icons.Default.Lock,
            isPassword = true
        )

        AuthTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Xác nhận mật khẩu",
            placeholder = "Nhập lại mật khẩu",
            leadingIcon = Icons.Default.Lock,
            isPassword = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(
            icon = Icons.Default.Shield,
            title = "Mẹo bảo mật",
            description = "Sử dụng ít nhất 8 ký tự, bao gồm chữ hoa, số và ký tự đặc biệt (!@#$)",
            backgroundColor = Color(0xFFE3F2FD) // Light blue for security tip
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onAgreeChange(!agreeToTerms) }
        ) {
            Checkbox(
                checked = agreeToTerms,
                onCheckedChange = onAgreeChange,
                colors = CheckboxDefaults.colors(checkedColor = OrangeHeader)
            )
            Text(
                text = "Tôi đồng ý với Điều khoản dịch vụ và Chính sách bảo mật của TravelMate",
                fontSize = 12.sp,
                color = GrayText
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            AuthButton(
                text = "Tạo tài khoản",
                onClick = onSignUp,
                color = OrangeHeader
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Đã có tài khoản? ", color = GrayText)
            Text(
                text = "Đăng nhập",
                color = BluePrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLoginClick() }
            )
        }
    }
}

@Composable
fun SmallInfoCard(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(OrangeLight, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
