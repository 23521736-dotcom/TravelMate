package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Category
import com.example.travelmate.domain.model.User
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.usecase.auth.ForgotPasswordUseCase
import com.example.travelmate.domain.usecase.auth.LoginUseCase
import com.example.travelmate.domain.usecase.auth.SignUpUseCase
import com.example.travelmate.domain.usecase.auth.VerifyOTPUseCase
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.ui.state.AuthUiState
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val verifyOTPUseCase: VerifyOTPUseCase,
    private val repository: AuthRepository,
    private val destinationRepository: DestinationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var verificationPollingJob: Job? = null

    init {
        checkSession()
        observeFirstTime()
    }

    private fun observeFirstTime() {
        viewModelScope.launch {
            repository.isFirstTime().collect { isFirst ->
                _uiState.update { it.copy(isFirstTime = isFirst) }
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            try {
                repository.getCurrentUser()?.let { user ->
                    _uiState.update { it.copy(user = user, isCheckingSession = false) }
                    refreshUserProfile(user.uid)
                } ?: run {
                    _uiState.update { it.copy(isCheckingSession = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCheckingSession = false) }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setFirstTime(false)
        }
    }

    private fun refreshUserProfile(uid: String) {
        viewModelScope.launch {
            repository.getUserProfile(uid).collect { result ->
                if (result is NetworkResult.Success) {
                    _uiState.update { it.copy(user = result.data) }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
        when {
            trimmedEmail.isEmpty() -> {
                _uiState.update { it.copy(error = "Email không được để trống") }
                return
            }
            !isValidEmail(trimmedEmail) -> {
                _uiState.update { it.copy(error = "Định dạng email không hợp lệ") }
                return
            }
            trimmedPassword.isEmpty() -> {
                _uiState.update { it.copy(error = "Mật khẩu không được để trống") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = loginUseCase(trimmedEmail, trimmedPassword)
            when (result) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true, user = result.data) }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedName = name.trim()
        
        // ... (validation logic remains the same)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = signUpUseCase(trimmedEmail, trimmedPassword, trimmedName)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = false, isAwaitingEmailVerification = true, user = result.data) }
                }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun verifyRegistrationOTP(email: String, otp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.verifyRegistrationOTP(email, otp)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isRegistrationComplete = true) }
                }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resendRegistrationEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.resendVerificationEmail(email)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, error = "Mã OTP mới đã được gửi!") }
                }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun forgotPassword(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            _uiState.update { it.copy(error = "Vui lòng nhập email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = forgotPasswordUseCase(trimmedEmail)
            when (result) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, isOtpSent = true) }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun verifyOTP(email: String, otp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = verifyOTPUseCase(email, otp)
            when (result) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, isOtpVerified = true, otpCode = otp) }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resendVerificationEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.resendVerificationEmail(email)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, error = "Email xác thực đã được gửi lại!") }
                    startEmailVerificationPolling()
                }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startEmailVerificationPolling() {
        verificationPollingJob?.cancel()
        verificationPollingJob = viewModelScope.launch {
            while (true) {
                delay(3000) // Poll every 3 seconds
                val result = repository.checkEmailVerificationStatus()
                if (result is NetworkResult.Success && result.data == true) {
                    _uiState.update { it.copy(isSuccess = true, isAwaitingEmailVerification = false, error = "Xác thực email thành công! Bạn có thể đăng nhập ngay.") }
                    stopEmailVerificationPolling()
                    break
                }
            }
        }
    }

    fun stopEmailVerificationPolling() {
        verificationPollingJob?.cancel()
        verificationPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopEmailVerificationPolling()
    }

    fun resetPassword(email: String, newPassword: String) {
        val otp = _uiState.value.otpCode ?: ""
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.resetPasswordWithOTP(email, otp, newPassword)
            when (result) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, isPasswordReset = true) }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateFullProfile(
        name: String,
        handle: String?,
        location: String?,
        bio: String?,
        interests: List<Category>
    ) {
        viewModelScope.launch {
            repository.updateProfile(name, handle, location, bio, interests, null).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, user = result.data, isSuccess = true) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState()
    }

    fun toggleWishlist(destinationId: String) {
        viewModelScope.launch {
            repository.toggleWishlist(destinationId).collect { result ->
                if (result is NetworkResult.Success) {
                    // Update user in UI state
                    val currentUser = repository.getCurrentUser()
                    _uiState.update { it.copy(user = currentUser) }
                }
            }
        }
    }

    fun clearSuccessState() {
        _uiState.update { it.copy(isSuccess = false, isLoading = false, error = null) }
    }

    fun seedDatabase(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            destinationRepository.seedDatabase().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        repository.seedTestData().collect { testResult ->
                            when (testResult) {
                                is NetworkResult.Success -> {
                                    _uiState.update { it.copy(isLoading = false, error = "Đã nạp toàn bộ dữ liệu thành công!") }
                                    onSuccess()
                                }
                                is NetworkResult.Error -> {
                                    _uiState.update { it.copy(isLoading = false, error = "Lỗi nạp dữ liệu mẫu: ${testResult.message}") }
                                }
                                else -> {}
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }

    fun resetState() {
        viewModelScope.launch {
            val currentUser = repository.getCurrentUser()
            _uiState.update { AuthUiState(user = currentUser, isSuccess = false, error = null, isLoading = false) }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
