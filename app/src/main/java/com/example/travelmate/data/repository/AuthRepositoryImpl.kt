package com.example.travelmate.data.repository

import com.example.travelmate.data.local.PreferenceDataStore
import com.example.travelmate.data.remote.EmailService
import com.example.travelmate.data.remote.SupabaseService
import com.example.travelmate.domain.model.Category
import com.example.travelmate.domain.model.User
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.auth.FirebaseAuth
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
    private val emailService: EmailService,
    private val supabaseService: SupabaseService,
    private val json: Json,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usersRef = database.getReference("users")
    private val otpsRef = database.getReference("otps")
    private val regOtpsRef = database.getReference("registration_otps")
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
                val uid = firebaseUser.uid
                val snapshot = usersRef.child(uid).get().await()
                
                if (snapshot.exists()) {
                    val user = mapSnapshotToUser(snapshot, uid)
                    android.util.Log.d("AuthRepository", "Login successful, user profile found in DB")
                    preferenceDataStore.saveUser(user)
                    NetworkResult.Success(user)
                } else {
                    android.util.Log.w("AuthRepository", "User authenticated but profile not found in DB for UID: $uid")
                    // Check if it's in registration_otps
                    val emailKey = trimmedEmail.replace(".", ",")
                    val regSnapshot = regOtpsRef.child(emailKey).get().await()
                    if (regSnapshot.exists()) {
                        NetworkResult.Error("Vui lòng xác thực mã OTP đã được gửi tới email của bạn.")
                    } else {
                        android.util.Log.i("AuthRepository", "Creating basic profile for user: $uid")
                        val newUser = User(uid = uid, email = firebaseUser.email ?: trimmedEmail, fullName = "Traveler")
                        usersRef.child(uid).setValue(newUser).await()
                        preferenceDataStore.saveUser(newUser)
                        NetworkResult.Success(newUser)
                    }
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
            handle = snapshot.child("handle").getValue(String::class.java),
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
                
                // Generate OTP for registration
                val otp = (100000..999999).random().toString()
                val emailKey = trimmedEmail.replace(".", ",")
                val otpData = mapOf(
                    "otp" to otp,
                    "timestamp" to System.currentTimeMillis(),
                    "fullName" to trimmedName
                )
                
                regOtpsRef.child(emailKey).setValue(otpData).await()
                
                // Send OTP email from buitung6705@gmail.com
                emailService.sendOTPEmail(trimmedEmail, otp)
                
                // Return a temporary user object
                val tempUser = User(uid = uid, email = trimmedEmail, fullName = trimmedName)
                NetworkResult.Success(tempUser)
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
            val timestamp = System.currentTimeMillis()
            val otpData = mapOf("otp" to otp, "timestamp" to timestamp)
            val emailKey = trimmedEmail.replace(".", ",")
            
            otpsRef.child(emailKey).setValue(otpData).await()
            val sent = emailService.sendOTPEmail(trimmedEmail, otp)
            if (sent) NetworkResult.Success(Unit) else NetworkResult.Error("Lỗi gửi email OTP")
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi hệ thống: ${e.message}")
        }
    }

    override suspend fun verifyOTP(email: String, otp: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val emailKey = email.trim().replace(".", ",")
        try {
            val snapshot = otpsRef.child(emailKey).get().await()
            if (snapshot.exists()) {
                val savedOtp = snapshot.child("otp").getValue(String::class.java)
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                val now = System.currentTimeMillis()
                
                if (savedOtp == otp) {
                    if (now - timestamp < 5 * 60 * 1000) NetworkResult.Success(Unit) else NetworkResult.Error("Mã OTP đã hết hạn")
                } else NetworkResult.Error("Mã OTP không chính xác")
            } else NetworkResult.Error("Không tìm thấy mã OTP cho email này")
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi kiểm tra OTP: ${e.message}")
        }
    }

    override suspend fun resendVerificationEmail(email: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val emailKey = email.trim().replace(".", ",")
            val snapshot = regOtpsRef.child(emailKey).get().await()
            val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Traveler"
            
            val otp = (100000..999999).random().toString()
            val otpData = mapOf(
                "otp" to otp,
                "timestamp" to System.currentTimeMillis(),
                "fullName" to fullName
            )
            
            regOtpsRef.child(emailKey).setValue(otpData).await()
            emailService.sendOTPEmail(email.trim(), otp)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error("Gửi lại mã thất bại: ${e.message}")
        }
    }

    override suspend fun verifyRegistrationOTP(email: String, otp: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val emailKey = email.trim().replace(".", ",")
        try {
            val snapshot = regOtpsRef.child(emailKey).get().await()
            if (snapshot.exists()) {
                val savedOtp = snapshot.child("otp").getValue(String::class.java)
                val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Traveler"
                
                if (savedOtp == otp) {
                    // Success! Now create the user in DB
                    val uid = firebaseAuth.currentUser?.uid ?: return@withContext NetworkResult.Error("Lỗi phiên làm việc")
                    val newUser = User(
                        uid = uid,
                        email = email.trim(),
                        fullName = fullName,
                        interests = emptyList()
                    )
                    usersRef.child(uid).setValue(newUser).await()
                    
                    // Clean up OTP
                    regOtpsRef.child(emailKey).removeValue()
                    
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error("Mã xác thực không chính xác")
                }
            } else {
                NetworkResult.Error("Không tìm thấy yêu cầu xác thực")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi xác thực: ${e.message}")
        }
    }

    override suspend fun checkEmailVerificationStatus(): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                val uid = user.uid
                val snapshot = usersRef.child(uid).get().await()
                NetworkResult.Success(snapshot.exists())
            } else {
                NetworkResult.Error("Phiên làm việc hết hạn")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi kiểm tra xác thực: ${e.message}")
        }
    }

    override suspend fun resetPasswordWithOTP(email: String, otp: String, newPassword: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        // First verify OTP again to be safe
        val verifyResult = verifyOTP(email, otp)
        if (verifyResult is NetworkResult.Error) return@withContext verifyResult

        try {
            // In a real app with Firebase, you'd usually use their built-in reset password flow
            // Workaround for demo: use Firebase's built-in reset email as well
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            
            // Clean up OTP
            val emailKey = email.trim().replace(".", ",")
            otpsRef.child(emailKey).removeValue().await()
            
            NetworkResult.Success(Unit)
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
        handle: String?,
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
                "handle" to (handle ?: ""),
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
}
