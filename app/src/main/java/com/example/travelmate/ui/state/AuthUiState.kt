package com.example.travelmate.ui.state

import com.example.travelmate.domain.model.User

data class AuthUiState(
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val user: User? = null,
    val isOtpSent: Boolean = false,
    val isOtpVerified: Boolean = false,
    val isPasswordReset: Boolean = false,
    val isAwaitingEmailVerification: Boolean = false,
    val isRegistrationComplete: Boolean = false,
    val isFirstTime: Boolean = false,
    val otpCode: String? = null
)
