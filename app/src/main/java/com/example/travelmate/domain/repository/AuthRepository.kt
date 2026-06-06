package com.example.travelmate.domain.repository

import com.example.travelmate.domain.model.Category
import com.example.travelmate.domain.model.User
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): NetworkResult<User>
    fun getUserProfile(uid: String): Flow<NetworkResult<User>>
    suspend fun signUp(email: String, password: String, name: String): NetworkResult<User>
    suspend fun forgotPassword(email: String): NetworkResult<Unit>
    suspend fun verifyOTP(email: String, otp: String): NetworkResult<Unit>
    suspend fun resendVerificationEmail(email: String): NetworkResult<Unit>
    suspend fun verifyRegistrationOTP(email: String, otp: String): NetworkResult<Unit>
    suspend fun checkEmailVerificationStatus(): NetworkResult<Boolean>
    suspend fun resetPasswordWithOTP(email: String, otp: String, newPassword: String): NetworkResult<Unit>
    suspend fun getCurrentUser(): User?
    fun logout()
    fun updateProfile(
        name: String,
        handle: String?,
        location: String?,
        bio: String?,
        interests: List<Category>,
        photoUrl: String? = null
    ): Flow<NetworkResult<User>>
    fun checkDatabaseStatus(): Flow<NetworkResult<Boolean>>
    fun seedTestData(): Flow<NetworkResult<Unit>>
    fun isFirstTime(): Flow<Boolean>
    suspend fun setFirstTime(isFirst: Boolean)
    fun toggleWishlist(destinationId: String): Flow<NetworkResult<Boolean>>
    fun uploadImage(path: String, byteArray: ByteArray): Flow<NetworkResult<String>>
    suspend fun changePassword(oldPassword: String, newPassword: String): NetworkResult<Unit>
}
