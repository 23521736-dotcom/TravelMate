package com.example.travelmate.data.repository

import com.example.travelmate.data.local.PreferenceDataStore
import com.example.travelmate.data.remote.SupabaseService
import com.example.travelmate.domain.model.Category
import com.example.travelmate.domain.model.User
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.EmailUtils
import com.example.travelmate.util.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError
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
    private val reviewRepository: ReviewRepository,
    private val tripRepository: TripRepository,
    private val json: Json,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usersRef = database.getReference("users")
    private val pendingRegRef = database.getReference("pending_registrations")
    private val destinationsRef = database.getReference("destinations")

    private fun getEmailKey(email: String): String {
        return email.trim()
            .replace(".", ",")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
    }

    override suspend fun login(email: String, password: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
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
                    if (user.isBlocked) {
                        firebaseAuth.signOut()
                        preferenceDataStore.clearUser()
                        return@withContext NetworkResult.Error("Tài khoản của bạn đã bị khóa.")
                    }
                    preferenceDataStore.saveUser(user)
                    NetworkResult.Success(user)
                } else {
                    val newUser = User(uid = uid, email = firebaseUser.email ?: trimmedEmail, fullName = "Traveler")
                    usersRef.child(uid).setValue(newUser).await()
                    preferenceDataStore.saveUser(newUser)
                    NetworkResult.Success(newUser)
                }
            } else {
                NetworkResult.Error("Xác thực thất bại")
            }
        } catch (e: Exception) {
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
            val snapshot = usersRef.child(uid).get().await()
            val user = mapSnapshotToUser(snapshot, uid)
            if (user.isBlocked) {
                firebaseAuth.signOut()
                preferenceDataStore.clearUser()
                emit(NetworkResult.Error("Tài khoản của bạn đã bị khóa."))
                return@flow
            }
            preferenceDataStore.saveUser(user)
            emit(NetworkResult.Success(user))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Lỗi đồng bộ dữ liệu: ${e.message}"))
        }
    }

    private fun mapSnapshotToUser(snapshot: DataSnapshot, uid: String): User {
        try {
            if (!snapshot.exists()) {
                return User(uid = uid, email = firebaseAuth.currentUser?.email ?: "", fullName = "Thành viên mới")
            }
            
            val interestsList = mutableListOf<Category>()
            try {
                snapshot.child("interests").children.forEach { child ->
                    child.getValue(String::class.java)?.let { categoryName ->
                        try {
                            interestsList.add(Category.valueOf(categoryName))
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}

            fun safeInt(snap: DataSnapshot): Int {
                return try {
                    when (val v = snap.value) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull() ?: 0
                        else -> 0
                    }
                } catch (e: Exception) { 0 }
            }

            fun safeDouble(snap: DataSnapshot): Double {
                return try {
                    when (val v = snap.value) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                } catch (e: Exception) { 0.0 }
            }

            return User(
                uid = uid,
                email = snapshot.child("email").getValue(String::class.java) ?: "",
                fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Traveler",
                photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: snapshot.child("photoURL").getValue(String::class.java),
                bio = snapshot.child("bio").getValue(String::class.java),
                location = snapshot.child("location").getValue(String::class.java),
                displayName = snapshot.child("displayName").getValue(String::class.java),
                isBlocked = snapshot.child("isBlocked").getValue(Boolean::class.java) ?: false,
                blockedReason = snapshot.child("blockedReason").getValue(String::class.java),
                totalTrips = safeInt(snapshot.child("totalTrips")),
                totalSpent = safeDouble(snapshot.child("totalSpent")),
                upcomingTrips = safeInt(snapshot.child("upcomingTrips")),
                interests = interestsList,
                wishlistIds = try { snapshot.child("wishlistIds").children.mapNotNull { it.getValue(String::class.java) } } catch (e: Exception) { emptyList() }
            )
        } catch (e: Exception) {
            return User(uid = uid, email = "", fullName = "User")
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedName = name.trim()
        
        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty() || trimmedName.isEmpty()) {
            return@withContext NetworkResult.Error("Vui lòng điền đầy đủ thông tin")
        }
        
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val otp = (100000..999999).random().toString()
            val expiry = System.currentTimeMillis() + 10 * 60 * 1000
            
            val pendingData = mapOf(
                "email" to trimmedEmail,
                "password" to trimmedPassword,
                "fullName" to trimmedName,
                "otp" to otp,
                "expiry" to expiry
            )
            pendingRegRef.child(emailKey).setValue(pendingData).await()
            
            val success = EmailUtils.sendOtpEmail(trimmedEmail, otp)
            if (success) {
                NetworkResult.Success(User(uid = "pending", email = trimmedEmail, fullName = trimmedName))
            } else {
                NetworkResult.Error("Không thể gửi email OTP. Vui lòng thử lại.")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Đăng ký thất bại: ${e.message}")
        }
    }

    override suspend fun verifyRegistrationOTP(email: String, otp: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val snapshot = pendingRegRef.child(emailKey).get().await()
            
            if (!snapshot.exists()) return@withContext NetworkResult.Error("Thông tin đăng ký không tồn tại")
            
            val storedOtp = snapshot.child("otp").getValue(String::class.java)
            val expiry = snapshot.child("expiry").getValue(Long::class.java) ?: 0L
            val password = snapshot.child("password").getValue(String::class.java) ?: ""
            val name = snapshot.child("fullName").getValue(String::class.java) ?: ""
            
            if (storedOtp == otp && System.currentTimeMillis() < expiry) {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                val uid = authResult.user?.uid ?: return@withContext NetworkResult.Error("Lỗi tạo user")
                
                val newUser = User(uid = uid, email = trimmedEmail, fullName = name, password = password)
                usersRef.child(uid).setValue(newUser).await()
                pendingRegRef.child(emailKey).removeValue()
                preferenceDataStore.saveUser(newUser)
                
                NetworkResult.Success(Unit)
            } else if (System.currentTimeMillis() >= expiry) {
                NetworkResult.Error("Mã OTP đã hết hạn")
            } else {
                NetworkResult.Error("Mã OTP không chính xác")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Xác thực thất bại: ${e.message}")
        }
    }

    override suspend fun forgotPassword(email: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val otp = (100000..999999).random().toString()
            val expiry = System.currentTimeMillis() + 10 * 60 * 1000
            
            database.getReference("otps").child(emailKey).setValue(
                mapOf("code" to otp, "expiry" to expiry)
            ).await()
            
            val success = EmailUtils.sendOtpEmail(trimmedEmail, otp)
            if (success) NetworkResult.Success(Unit)
            else NetworkResult.Error("Lỗi gửi email")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Lỗi")
        }
    }

    override suspend fun verifyOTP(email: String, otp: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val snapshot = database.getReference("otps").child(emailKey).get().await()
            val storedOtp = snapshot.child("code").getValue(String::class.java)
            val expiry = snapshot.child("expiry").getValue(Long::class.java) ?: 0L
            
            if (storedOtp == otp && System.currentTimeMillis() < expiry) NetworkResult.Success(Unit)
            else NetworkResult.Error("Mã OTP không hợp lệ hoặc hết hạn")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Lỗi")
        }
    }

    override suspend fun resendVerificationEmail(email: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val emailKey = getEmailKey(trimmedEmail)
        val otp = (100000..999999).random().toString()
        val expiry = System.currentTimeMillis() + 10 * 60 * 1000
        
        try {
            val pendingSnap = pendingRegRef.child(emailKey).get().await()
            if (pendingSnap.exists()) {
                pendingRegRef.child(emailKey).child("otp").setValue(otp).await()
                pendingRegRef.child(emailKey).child("expiry").setValue(expiry).await()
            } else {
                database.getReference("otps").child(emailKey).child("code").setValue(otp).await()
                database.getReference("otps").child(emailKey).child("expiry").setValue(expiry).await()
            }
            EmailUtils.sendOtpEmail(trimmedEmail, otp)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Lỗi")
        }
    }

    override suspend fun checkEmailVerificationStatus(): NetworkResult<Boolean> = NetworkResult.Success(true)

    override suspend fun resetPasswordWithOTP(email: String, otp: String, newPassword: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val snapshot = database.getReference("otps").child(emailKey).get().await()
            val storedOtp = snapshot.child("code").getValue(String::class.java)
            val expiry = snapshot.child("expiry").getValue(Long::class.java) ?: 0L
            
            if (storedOtp == otp && System.currentTimeMillis() < expiry) {
                // 1. Cập nhật mật khẩu vào Database (Theo yêu cầu của bạn)
                val userSnapshot = usersRef.orderByChild("email").equalTo(trimmedEmail).get().await()
                val uid = userSnapshot.children.firstOrNull()?.key
                if (uid != null) {
                    usersRef.child(uid).child("password").setValue(newPassword).await()
                }

                // 2. Gửi email đặt lại mật khẩu của Firebase (Bắt buộc để Firebase Auth cập nhật mật khẩu đăng nhập)
                // Lưu ý: Người dùng VẪN PHẢI nhấn vào link trong email này để có thể đăng nhập bằng mật khẩu mới.
                firebaseAuth.sendPasswordResetEmail(trimmedEmail).await()

                database.getReference("otps").child(emailKey).removeValue()
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("Mã xác thực không hợp lệ hoặc đã hết hạn")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Lỗi: ${e.message}")
        }
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            withTimeoutOrNull(2000) { preferenceDataStore.currentUser.first() }
        } catch (e: Exception) { null }
    }

    override fun observeUser(): Flow<User?> = preferenceDataStore.currentUser

    override fun listenToUserRealtime(uid: String): Flow<User?> = callbackFlow {
        val userRef = usersRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = mapSnapshotToUser(snapshot, uid)
                // Sync to local data store as well
                repositoryScope.launch {
                    preferenceDataStore.saveUser(user)
                }
                trySend(user)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        userRef.addValueEventListener(listener)
        awaitClose { userRef.removeEventListener(listener) }
    }

    override fun logout() {
        firebaseAuth.signOut()
        repositoryScope.launch { preferenceDataStore.clearUser() }
    }

    override fun updateProfile(name: String, displayName: String?, location: String?, bio: String?, interests: List<Category>, photoUrl: String?): Flow<NetworkResult<User>> = flow {
        val uid = firebaseAuth.currentUser?.uid ?: return@flow
        emit(NetworkResult.Loading)
        try {
            val updates = mutableMapOf<String, Any>(
                "fullName" to name.trim(),
                "displayName" to (displayName ?: ""),
                "location" to (location ?: ""),
                "bio" to (bio ?: ""),
                "interests" to interests.map { it.name }
            )
            photoUrl?.let { updates["photoUrl"] = it }
            usersRef.child(uid).updateChildren(updates).await()
            val user = mapSnapshotToUser(usersRef.child(uid).get().await(), uid)
            preferenceDataStore.saveUser(user)
            emit(NetworkResult.Success(user))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Lỗi"))
        }
    }

    override fun checkDatabaseStatus(): Flow<NetworkResult<Boolean>> = flow {
        try {
            val snapshot = database.getReference(".info/connected").get().await()
            emit(NetworkResult.Success(snapshot.getValue(Boolean::class.java) ?: false))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Lỗi kết nối DB"))
        }
    }

    override fun seedTestData(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val jsonString = context.assets.open("data/seed_data.json").bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(jsonString).jsonObject
            root["destinations"]?.jsonObject?.let { destinationsRef.updateChildren(it.toAny() as Map<String, Any>).await() }
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Lỗi"))
        }
    }

    private fun kotlinx.serialization.json.JsonElement.toAny(): Any? {
        return when (this) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                if (isString) content else content.toBooleanStrictOrNull() ?: content.toLongOrNull() ?: content.toDoubleOrNull() ?: content
            }
            is kotlinx.serialization.json.JsonObject -> this.mapValues { it.value.toAny() }
            is kotlinx.serialization.json.JsonArray -> this.map { it.toAny() }
            else -> null
        }
    }

    override fun isFirstTime(): Flow<Boolean> = preferenceDataStore.isFirstTime
    override suspend fun setFirstTime(isFirst: Boolean) = preferenceDataStore.setFirstTime(isFirst)
    override fun toggleWishlist(destinationId: String): Flow<NetworkResult<Boolean>> = flow {
        val uid = firebaseAuth.currentUser?.uid ?: return@flow
        emit(NetworkResult.Loading)
        try {
            val snapshot = usersRef.child(uid).child("wishlistIds").get().await()
            val current = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
            val added = if (current.contains(destinationId)) { current.remove(destinationId); false } else { current.add(destinationId); true }
            usersRef.child(uid).child("wishlistIds").setValue(current).await()
            preferenceDataStore.saveUser(mapSnapshotToUser(usersRef.child(uid).get().await(), uid))
            emit(NetworkResult.Success(added))
        } catch (e: Exception) { emit(NetworkResult.Error(e.message ?: "Lỗi")) }
    }

    override fun uploadImage(path: String, byteArray: ByteArray): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try { emit(NetworkResult.Success(supabaseService.uploadImage(path, byteArray))) }
        catch (e: Exception) { emit(NetworkResult.Error(e.message ?: "Upload failed")) }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext NetworkResult.Error("Chưa đăng nhập")
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, oldPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            NetworkResult.Success(Unit)
        } catch (e: Exception) { NetworkResult.Error(e.message ?: "Lỗi") }
    }
}
