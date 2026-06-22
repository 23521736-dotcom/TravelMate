package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.local.PreferenceDataStore
import com.example.travelmate.domain.model.User
import com.example.travelmate.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.distinctUntilChanged

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferenceDataStore: PreferenceDataStore,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        observeUserRealtime()
    }

    private fun observeUserRealtime() {
        viewModelScope.launch {
            // Chỉ bắt đầu lắng nghe Realtime khi UID thay đổi (đăng nhập mới)
            authRepository.observeUser()
                .distinctUntilChanged { old, new -> old?.uid == new?.uid }
                .collectLatest { localUser ->
                    if (localUser != null) {
                        authRepository.listenToUserRealtime(localUser.uid).collectLatest { updatedUser ->
                            _currentUser.value = updatedUser
                        }
                    } else {
                        _currentUser.value = null
                    }
                }
        }
    }

    val isDarkMode: StateFlow<Boolean> = preferenceDataStore.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            preferenceDataStore.setDarkMode(isDark)
        }
    }
}
