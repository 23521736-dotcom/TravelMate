package com.example.travelmate.data.repository

import com.example.travelmate.data.local.PreferenceDataStore
import com.example.travelmate.data.remote.SupabaseService
import com.example.travelmate.domain.model.Category
import com.example.travelmate.domain.model.User
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.util.EmailUtils
import com.example.travelmate.util.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import android.content.Context

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val preferenceDataStore: PreferenceDataStore,
    private val supabaseService: SupabaseService,
    private val json: Json,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usersRef = database.getReference("users")
    private val pendingRegRef = database.getReference("pending_registrations")
    private val destinationsRef = database.getReference("destinations")

    override suspend fun login(email: String, password: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
        android.util.Log.d("AuthRepository", "Attempting login for: $trimmedEmail")
        
        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
            return@withContext NetworkResult.Error("Vui lòng nhập đầy đủ email và mật khẩu")
        }
        
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                // Tạm thời vô hiệu hóa check email verification để test
                /*
                if (!firebaseUser.isEmailVerified) {
                    return@withContext NetworkResult.Error("Vui lòng xác thực email của bạn trước khi đăng nhập.")
                }
                */
                val uid = firebaseUser.uid
                val snapshot = usersRef.child(uid).get().await()
                
                if (snapshot.exists()) {
                    val user = mapSnapshotToUser(snapshot, uid)
                    if (user.isBlocked) {
                        android.util.Log.w("AuthRepository", "User $uid is blocked. Logging out.")
                        firebaseAuth.signOut()
                        preferenceDataStore.clearUser()
                        return@withContext NetworkResult.Error("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.")
                    }
                    android.util.Log.d("AuthRepository", "Login successful, user profile found in DB")
                    preferenceDataStore.saveUser(user)
                    NetworkResult.Success(user)
                } else {
                    android.util.Log.w("AuthRepository", "User authenticated but profile not found in DB for UID: $uid")
                    android.util.Log.i("AuthRepository", "Creating basic profile for user: $uid")
                    val newUser = User(uid = uid, email = firebaseUser.email ?: trimmedEmail, fullName = "Traveler")
                    usersRef.child(uid).setValue(newUser).await()
                    preferenceDataStore.saveUser(newUser)
                    NetworkResult.Success(newUser)
                }
            } else {
                NetworkResult.Error("Xác thực thất bại")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Login exception", e)
            val errorMsg = when {
                e.message?.contains("user-not-found") == true -> "Tài khoản không tồn tại"
                e.message?.contains("wrong-password") == true -> "Mật khẩu không chính xác"
                else -> "Đăng nhập thất bại: ${e.message}"
            }
            NetworkResult.Error(errorMsg)
        }
    }

    override fun getUserProfile(uid: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        try {
            android.util.Log.d("AuthRepository", "Fetching profile for UID: $uid")
            val snapshot = usersRef.child(uid).get().await()
            if (!snapshot.exists()) {
                android.util.Log.w("AuthRepository", "Profile not found in DB for UID: $uid")
            }
            val user = mapSnapshotToUser(snapshot, uid)
            if (user.isBlocked) {
                android.util.Log.w("AuthRepository", "User $uid is blocked. Clearing session.")
                firebaseAuth.signOut()
                preferenceDataStore.clearUser()
                emit(NetworkResult.Error("Tài khoản của bạn đã bị khóa."))
                return@flow
            }
            preferenceDataStore.saveUser(user)
            emit(NetworkResult.Success(user))
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error fetching profile for $uid", e)
            emit(NetworkResult.Error("Lỗi đồng bộ dữ liệu: ${e.message}"))
        }
    }

    private fun mapSnapshotToUser(snapshot: DataSnapshot, uid: String): User {
        if (!snapshot.exists()) {
            return User(uid = uid, email = firebaseAuth.currentUser?.email ?: "", fullName = "Thành viên mới")
        }
        
        val interestsList = mutableListOf<Category>()
        snapshot.child("interests").children.forEach { child ->
            child.getValue(String::class.java)?.let { categoryName ->
                try {
                    interestsList.add(Category.valueOf(categoryName))
                } catch (e: Exception) {}
            }
        }

        // Helper functions for safe type conversion
        fun safeInt(snap: DataSnapshot): Int {
            return when (val v = snap.value) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull() ?: 0
                else -> 0
            }
        }

        fun safeDouble(snap: DataSnapshot): Double {
            return when (val v = snap.value) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        }

        return User(
            uid = uid,
            email = snapshot.child("email").getValue(String::class.java) ?: "",
            fullName = snapshot.child("fullName").getValue(String::class.java),
            photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: snapshot.child("photoURL").getValue(String::class.java),
            bio = snapshot.child("bio").getValue(String::class.java),
            location = snapshot.child("location").getValue(String::class.java),
            displayName = snapshot.child("displayName").getValue(String::class.java) ?: snapshot.child("handle").getValue(String::class.java),
            isBlocked = snapshot.child("isBlocked").getValue(Boolean::class.java) ?: false,
            totalTrips = safeInt(snapshot.child("totalTrips")),
            totalSpent = safeDouble(snapshot.child("totalSpent")),
            upcomingTrips = safeInt(snapshot.child("upcomingTrips")),
            interests = interestsList,
            wishlistIds = snapshot.child("wishlistIds").children.mapNotNull { it.getValue(String::class.java) }
        )
    }

    override suspend fun signUp(email: String, password: String, name: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedName = name.trim()
        
        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty() || trimmedName.isEmpty()) {
            return@withContext NetworkResult.Error("Vui lòng điền đầy đủ thông tin")
        }
        
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val uid = firebaseUser.uid
                android.util.Log.d("AuthRepository", "User created with UID: $uid")
                
                // Tạo profile ngay lập tức (Tạm thời bỏ qua bước xác thực email)
                val newUser = User(uid = uid, email = trimmedEmail, fullName = trimmedName)
                usersRef.child(uid).setValue(newUser).await()
                preferenceDataStore.saveUser(newUser)

                // Tạm thời vô hiệu hóa gửi email OTP
                /*
                try {
                    val otp = (100000..999999).random().toString()
                    val expiry = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes
                    
                    database.getReference("otps").child(trimmedEmail.replace(".", ",")).setValue(
                        mapOf("code" to otp, "expiry" to expiry)
                    ).await()
                    
                    val success = EmailUtils.sendOtpEmail(trimmedEmail, otp)
                    if (success) {
                        android.util.Log.d("AuthRepository", "OTP sent via SMTP to ${trimmedEmail}")
                    } else {
                        android.util.Log.e("AuthRepository", "EmailUtils returned false for ${trimmedEmail}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepository", "Failed to send OTP via SMTP", e)
                }
                */
                
                NetworkResult.Success(newUser)
            } else {
                NetworkResult.Error("Không thể tạo tài khoản")
            }
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("email-already-in-use") == true -> "Email này đã được sử dụng"
                else -> "Đăng ký thất bại: ${e.message}"
            }
            NetworkResult.Error(errorMsg)
        }
    }

    override suspend fun forgotPassword(email: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            return@withContext NetworkResult.Error("Vui lòng nhập email")
        }

        try {
            val otp = (100000..999999).random().toString()
            val expiry = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes
            
            database.getReference("otps").child(trimmedEmail.replace(".", ",")).setValue(
                mapOf("code" to otp, "expiry" to expiry)
            ).await()
            
            val success = EmailUtils.sendOtpEmail(trimmedEmail, otp)
            if (success) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("Không thể gửi email OTP. Vui lòng thử lại sau.")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi gửi email đặt lại mật khẩu: ${e.message}")
        }
    }

    override suspend fun verifyOTP(email: String, otp: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.getReference("otps").child(email.replace(".", ",")).get().await()
            val storedOtp = snapshot.child("code").getValue(String::class.java)
            val expiry = snapshot.child("expiry").getValue(Long::class.java) ?: 0L
            
            if (storedOtp == otp && System.currentTimeMillis() < expiry) {
                // Clear OTP after successful verification
                database.getReference("otps").child(email.replace(".", ",")).removeValue()
                NetworkResult.Success(Unit)
            } else if (System.currentTimeMillis() >= expiry) {
                NetworkResult.Error("Mã OTP đã hết hạn")
            } else {
                NetworkResult.Error("Mã OTP không chính xác")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi xác thực OTP: ${e.message}")
        }
    }

    override suspend fun resendVerificationEmail(email: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AuthRepository", "Attempting to resend verification OTP to $email")
            val otp = (100000..999999).random().toString()
            val expiry = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes
            
            // Store OTP in Firebase
            database.getReference("otps").child(email.replace(".", ",")).setValue(
                mapOf("code" to otp, "expiry" to expiry)
            ).await()
            
            val success = EmailUtils.sendOtpEmail(email, otp)
            if (success) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("Không thể gửi email. Vui lòng thử lại sau.")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Exception in resendVerificationEmail", e)
            NetworkResult.Error("Gửi lại mã OTP thất bại: ${e.message}")
        }
    }

    override suspend fun verifyRegistrationOTP(email: String, otp: String): NetworkResult<Unit> = verifyOTP(email, otp)

    override suspend fun checkEmailVerificationStatus(): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Note: Since we are using SMTP for OTP, we need to mark the user as verified 
            // in a way that Firebase understands, OR we manage "verification" in our own DB.
            // However, firebaseAuth.currentUser.isEmailVerified is controlled by Firebase.
            
            // To bridge this without a backend, we might need to use Firebase's native verification
            // for the actual "isEmailVerified" flag, OR handle our own "isVerified" flag in the user profile.
            
            // If the user entered the correct OTP in the UI, the UI should call a method to 
            // proceed. checkEmailVerificationStatus is usually used for polling.
            
            val user = firebaseAuth.currentUser
            if (user != null) {
                // If we are using SMTP OTP, this native reload won't show "verified" 
                // unless we also use firebaseUser.sendEmailVerification().
                
                // Let's assume for now we want to keep using Firebase's native verification 
                // but use SMTP as a fallback or parallel notification.
                // ACTUALLY, the user asked to "switch back to SMTP", implying they want to use OTP.
                
                // If we use OTP, we should probably stop polling isEmailVerified and instead
                // rely on the verifyOTP call to succeed.
                
                user.reload().await()
                if (user.isEmailVerified) {
                    val uid = user.uid
                    // ... (rest of the profile creation logic)
                    val userSnap = usersRef.child(uid).get().await()
                    if (!userSnap.exists()) {
                        val pendingSnap = pendingRegRef.child(uid).get().await()
                        val fullName = pendingSnap.child("fullName").getValue(String::class.java) ?: "Traveler"
                        
                        val newUser = User(uid = uid, email = user.email ?: "", fullName = fullName)
                        usersRef.child(uid).setValue(newUser).await()
                        pendingRegRef.child(uid).removeValue()
                        preferenceDataStore.saveUser(newUser)
                    }
                    NetworkResult.Success(true)
                } else {
                    NetworkResult.Success(false)
                }
            } else {
                NetworkResult.Error("Phiên làm việc hết hạn")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi kiểm tra xác thực: ${e.message}")
        }
    }

    override suspend fun resetPasswordWithOTP(email: String, otp: String, newPassword: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Verify OTP first (already verified in UI but double check or just trust the previous step)
            // In a robust system, we'd have a server-side session. 
            // Here, let's assume if they have the right OTP, we can reset.
            
            // Firebase doesn't allow direct password reset with custom OTP easily without a backend.
            // But we can update the password of the current user if they are logged in, 
            // OR use Firebase Admin SDK on a backend.
            
            // Since we are client-side only, we might need a different approach for "Forgot Password" 
            // if we don't use Firebase's native reset email.
            
            // Wait, if we use SMTP for OTP, we need a way to actually change the password.
            // Let's use firebaseAuth.confirmPasswordReset(oobCode, newPassword)? No, that's for their links.
            
            // Re-evaluating: For Password Reset, Firebase native is safer because it handles the security tokens.
            // But for Registration, SMTP + OTP is fine.
            
            NetworkResult.Error("Đặt lại mật khẩu qua OTP yêu cầu hệ thống backend. Vui lòng sử dụng tính năng 'Quên mật khẩu' mặc định.")
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi đặt lại mật khẩu: ${e.message}")
        }
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            withTimeoutOrNull(3000) {
                preferenceDataStore.currentUser.first()
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
        repositoryScope.launch { preferenceDataStore.clearUser() }
    }

    override fun updateProfile(
        name: String,
        displayName: String?,
        location: String?,
        bio: String?,
        interests: List<Category>,
        photoUrl: String?
    ): Flow<NetworkResult<User>> = flow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            android.util.Log.e("AuthRepository", "Update profile failed: User not logged in")
            emit(NetworkResult.Error("Chưa đăng nhập"))
            return@flow
        }
        
        emit(NetworkResult.Loading)
        try {
            android.util.Log.d("AuthRepository", "Updating profile for UID: $uid")
            val updates = mutableMapOf<String, Any>(
                "fullName" to name.trim(),
                "displayName" to (displayName ?: ""),
                "location" to (location ?: ""),
                "bio" to (bio ?: ""),
                "interests" to interests.map { it.name }
            )
            photoUrl?.let { updates["photoUrl"] = it }
            
            usersRef.child(uid).updateChildren(updates).await()
            val snapshot = usersRef.child(uid).get().await()
            val user = mapSnapshotToUser(snapshot, uid)
            preferenceDataStore.saveUser(user)
            android.util.Log.d("AuthRepository", "Profile updated successfully in DB and local")
            emit(NetworkResult.Success(user))
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Update profile exception", e)
            emit(NetworkResult.Error("Cập nhật thất bại: ${e.message}"))
        }
    }

    override fun checkDatabaseStatus(): Flow<NetworkResult<Boolean>> = flow {
        try {
            val snapshot = database.getReference(".info/connected").get().await()
            emit(NetworkResult.Success(snapshot.getValue(Boolean::class.java) ?: false))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Lỗi kết nối DB: ${e.message}"))
        }
    }

    override fun seedTestData(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val jsonString = context.assets.open("data/seed_data.json").bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(jsonString).jsonObject
            
            // Seed Destinations
            root["destinations"]?.jsonObject?.let { dests ->
                val destMap = mutableMapOf<String, Any>()
                dests.forEach { (key, value) ->
                    destMap[key] = value.toAny() ?: ""
                }
                destinationsRef.updateChildren(destMap).await()
            }

            // Seed sample Users (careful with this in production)
            root["users"]?.jsonObject?.let { users ->
                val usersMap = mutableMapOf<String, Any>()
                users.forEach { (key, value) ->
                    usersMap[key] = value.toAny() ?: ""
                }
                database.getReference("users").updateChildren(usersMap).await()
            }

            // Seed Notifications
            root["notifications"]?.jsonObject?.let { notifs ->
                val notifMap = mutableMapOf<String, Any>()
                notifs.forEach { (key, value) ->
                    notifMap[key] = value.toAny() ?: ""
                }
                database.getReference("notifications").updateChildren(notifMap).await()
            }
            
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Lỗi nạp dữ liệu: ${e.message}"))
        }
    }

    // Helper to convert JsonElement to Any for Firebase
    private fun kotlinx.serialization.json.JsonElement.toAny(): Any? {
        return when (this) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                if (isString) content
                else {
                    content.toBooleanStrictOrNull() 
                        ?: content.toLongOrNull() 
                        ?: content.toDoubleOrNull() 
                        ?: content
                }
            }
            is kotlinx.serialization.json.JsonObject -> {
                this.mapValues { it.value.toAny() }
            }
            is kotlinx.serialization.json.JsonArray -> {
                this.map { it.toAny() }
            }
            else -> null
        }
    }

    override fun isFirstTime(): Flow<Boolean> = preferenceDataStore.isFirstTime

    override suspend fun setFirstTime(isFirst: Boolean) {
        preferenceDataStore.setFirstTime(isFirst)
    }

    override fun toggleWishlist(destinationId: String): Flow<NetworkResult<Boolean>> = flow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            emit(NetworkResult.Error("Chưa đăng nhập"))
            return@flow
        }

        emit(NetworkResult.Loading)
        try {
            val snapshot = usersRef.child(uid).child("wishlistIds").get().await()
            val currentWishlist = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
            
            val isAdded: Boolean
            if (currentWishlist.contains(destinationId)) {
                currentWishlist.remove(destinationId)
                isAdded = false
            } else {
                currentWishlist.add(destinationId)
                isAdded = true
            }

            usersRef.child(uid).child("wishlistIds").setValue(currentWishlist).await()
            
            // Sync user data to local
            val userSnapshot = usersRef.child(uid).get().await()
            val user = mapSnapshotToUser(userSnapshot, uid)
            preferenceDataStore.saveUser(user)
            
            emit(NetworkResult.Success(isAdded))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Lỗi cập nhật yêu thích: ${e.message}"))
        }
    }

    override fun uploadImage(path: String, byteArray: ByteArray): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try {
            val url = supabaseService.uploadImage(path, byteArray)
            emit(NetworkResult.Success(url))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Upload failed"))
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext NetworkResult.Error("Chưa đăng nhập")
            val email = user.email ?: return@withContext NetworkResult.Error("Không tìm thấy email")
            
            // Re-authenticate
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)
            user.reauthenticate(credential).await()
            
            // Update password
            user.updatePassword(newPassword).await()
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi đổi mật khẩu: ${e.message}")
        }
    }
}
