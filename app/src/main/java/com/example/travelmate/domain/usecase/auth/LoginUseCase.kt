package com.example.travelmate.domain.usecase.auth

import com.example.travelmate.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) = 
        repository.login(email, password)
}
