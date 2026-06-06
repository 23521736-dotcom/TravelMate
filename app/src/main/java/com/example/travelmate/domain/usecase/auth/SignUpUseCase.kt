package com.example.travelmate.domain.usecase.auth

import com.example.travelmate.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, name: String) =
        repository.signUp(email, password, name)
}
