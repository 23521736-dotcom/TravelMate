package com.example.travelmate.data.repository

import android.content.Context
import com.example.travelmate.data.local.PreferenceDataStore
import com.example.travelmate.data.remote.SupabaseService
import com.example.travelmate.domain.model.Category
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.User
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.EmailUtils
import com.example.travelmate.util.NetworkResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferenceDataStore: PreferenceDataStore,
    private val supabaseService: SupabaseService,
    private val reviewRepository: ReviewRepository,
    private val tripRepository: TripRepository,
    private val json: Json,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usersRef = firestore.collection("users")
    private val pendingRegRef = firestore.collection("pending_registrations")
    private val otpsRef = firestore.collection("otps")
    private val destinationsRef = firestore.collection("destinations")

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
            return@withContext NetworkResult.Error("Vui long nhap day du email va mat khau")
        }

        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
            val firebaseUser = authResult.user ?: return@withContext NetworkResult.Error("Xac thuc that bai")
            val uid = firebaseUser.uid
            val snapshot = usersRef.document(uid).get().await()

            val user = if (snapshot.exists()) {
                mapDocumentToUser(snapshot, uid)
            } else {
                User(uid = uid, email = firebaseUser.email ?: trimmedEmail, fullName = "Traveler").also {
                    usersRef.document(uid).set(it).await()
                }
            }

            if (user.isBlocked) {
                firebaseAuth.signOut()
                preferenceDataStore.clearUser()
                return@withContext NetworkResult.Error("Tai khoan cua ban da bi khoa.")
            }

            preferenceDataStore.saveUser(user)
            NetworkResult.Success(user)
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("user-not-found") == true -> "Tai khoan khong ton tai"
                e.message?.contains("wrong-password") == true -> "Mat khau khong chinh xac"
                else -> "Dang nhap that bai: ${e.message}"
            }
            NetworkResult.Error(errorMsg)
        }
    }

    override fun getUserProfile(uid: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        try {
            val snapshot = usersRef.document(uid).get().await()
            val user = mapDocumentToUser(snapshot, uid)
            if (user.isBlocked) {
                firebaseAuth.signOut()
                preferenceDataStore.clearUser()
                emit(NetworkResult.Error("Tai khoan cua ban da bi khoa."))
                return@flow
            }
            preferenceDataStore.saveUser(user)
            emit(NetworkResult.Success(user))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Loi dong bo du lieu: ${e.message}"))
        }
    }

    private fun mapDocumentToUser(document: DocumentSnapshot, uid: String): User {
        return try {
            if (!document.exists()) {
                return User(uid = uid, email = firebaseAuth.currentUser?.email ?: "", fullName = "Thanh vien moi")
            }

            val data = document.data.orEmpty()
            val interests = (data["interests"] as? List<*>)?.mapNotNull { value ->
                try {
                    Category.valueOf(value.toString())
                } catch (e: Exception) {
                    null
                }
            }.orEmpty()

            fun safeInt(field: String): Int {
                return when (val value = data[field]) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                }
            }

            fun safeDouble(field: String): Double {
                return when (val value = data[field]) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
            }

            User(
                uid = uid,
                email = data["email"] as? String ?: "",
                fullName = data["fullName"] as? String ?: "Traveler",
                photoUrl = data["photoUrl"] as? String ?: data["photoURL"] as? String,
                bio = data["bio"] as? String,
                location = data["location"] as? String,
                displayName = data["displayName"] as? String,
                isBlocked = data["isBlocked"] as? Boolean ?: false,
                blockedReason = data["blockedReason"] as? String,
                totalTrips = safeInt("totalTrips"),
                totalSpent = safeDouble("totalSpent"),
                upcomingTrips = safeInt("upcomingTrips"),
                interests = interests,
                wishlistIds = (data["wishlistIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            )
        } catch (e: Exception) {
            User(uid = uid, email = "", fullName = "User")
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedName = name.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty() || trimmedName.isEmpty()) {
            return@withContext NetworkResult.Error("Vui long dien day du thong tin")
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
            pendingRegRef.document(emailKey).set(pendingData).await()

            val success = EmailUtils.sendOtpEmail(trimmedEmail, otp)
            if (success) {
                NetworkResult.Success(User(uid = "pending", email = trimmedEmail, fullName = trimmedName))
            } else {
                NetworkResult.Error("Khong the gui email OTP. Vui long thu lai.")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Dang ky that bai: ${e.message}")
        }
    }

    override suspend fun verifyRegistrationOTP(email: String, otp: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val snapshot = pendingRegRef.document(emailKey).get().await()

            if (!snapshot.exists()) return@withContext NetworkResult.Error("Thong tin dang ky khong ton tai")

            val storedOtp = snapshot.getString("otp")
            val expiry = snapshot.getLong("expiry") ?: 0L
            val password = snapshot.getString("password") ?: ""
            val name = snapshot.getString("fullName") ?: ""

            if (storedOtp == otp && System.currentTimeMillis() < expiry) {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                val uid = authResult.user?.uid ?: return@withContext NetworkResult.Error("Loi tao user")

                val newUser = User(uid = uid, email = trimmedEmail, fullName = name, password = password)
                usersRef.document(uid).set(newUser).await()
                pendingRegRef.document(emailKey).delete().await()
                preferenceDataStore.saveUser(newUser)

                NetworkResult.Success(Unit)
            } else if (System.currentTimeMillis() >= expiry) {
                NetworkResult.Error("Ma OTP da het han")
            } else {
                NetworkResult.Error("Ma OTP khong chinh xac")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Xac thuc that bai: ${e.message}")
        }
    }

    override suspend fun forgotPassword(email: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val otp = (100000..999999).random().toString()
            val expiry = System.currentTimeMillis() + 10 * 60 * 1000

            otpsRef.document(emailKey).set(mapOf("code" to otp, "expiry" to expiry)).await()

            val success = EmailUtils.sendOtpEmail(trimmedEmail, otp)
            if (success) NetworkResult.Success(Unit)
            else NetworkResult.Error("Loi gui email")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Loi")
        }
    }

    override suspend fun verifyOTP(email: String, otp: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val snapshot = otpsRef.document(emailKey).get().await()
            val storedOtp = snapshot.getString("code")
            val expiry = snapshot.getLong("expiry") ?: 0L

            if (storedOtp == otp && System.currentTimeMillis() < expiry) NetworkResult.Success(Unit)
            else NetworkResult.Error("Ma OTP khong hop le hoac het han")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Loi")
        }
    }

    override suspend fun resendVerificationEmail(email: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val emailKey = getEmailKey(trimmedEmail)
        val otp = (100000..999999).random().toString()
        val expiry = System.currentTimeMillis() + 10 * 60 * 1000

        try {
            val pendingRef = pendingRegRef.document(emailKey)
            val pendingSnap = pendingRef.get().await()
            if (pendingSnap.exists()) {
                pendingRef.update(mapOf("otp" to otp, "expiry" to expiry)).await()
            } else {
                otpsRef.document(emailKey).set(mapOf("code" to otp, "expiry" to expiry), SetOptions.merge()).await()
            }
            EmailUtils.sendOtpEmail(trimmedEmail, otp)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Loi")
        }
    }

    override suspend fun checkEmailVerificationStatus(): NetworkResult<Boolean> = NetworkResult.Success(true)

    override suspend fun resetPasswordWithOTP(email: String, otp: String, newPassword: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        try {
            val emailKey = getEmailKey(trimmedEmail)
            val snapshot = otpsRef.document(emailKey).get().await()
            val storedOtp = snapshot.getString("code")
            val expiry = snapshot.getLong("expiry") ?: 0L

            if (storedOtp == otp && System.currentTimeMillis() < expiry) {
                val userSnapshot = usersRef.whereEqualTo("email", trimmedEmail).limit(1).get().await()
                val userDoc = userSnapshot.documents.firstOrNull()
                if (userDoc != null) {
                    userDoc.reference.update("password", newPassword).await()
                }

                firebaseAuth.sendPasswordResetEmail(trimmedEmail).await()
                otpsRef.document(emailKey).delete().await()
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("Ma xac thuc khong hop le hoac da het han")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Loi: ${e.message}")
        }
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            withTimeoutOrNull(2000) { preferenceDataStore.currentUser.first() }
        } catch (e: Exception) {
            null
        }
    }

    override fun observeUser(): Flow<User?> = preferenceDataStore.currentUser

    override fun listenToUserRealtime(uid: String): Flow<User?> = callbackFlow {
        val registration = usersRef.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val document = snapshot ?: return@addSnapshotListener
            val user = mapDocumentToUser(document, uid)
            repositoryScope.launch {
                preferenceDataStore.saveUser(user)
            }
            trySend(user)
        }
        awaitClose { registration.remove() }
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
        val uid = firebaseAuth.currentUser?.uid ?: return@flow
        emit(NetworkResult.Loading)
        try {
            val updates = mutableMapOf<String, Any?>(
                "fullName" to name.trim(),
                "displayName" to (displayName ?: ""),
                "location" to (location ?: ""),
                "bio" to (bio ?: ""),
                "interests" to interests.map { it.name }
            )
            photoUrl?.let { updates["photoUrl"] = it }

            usersRef.document(uid).set(updates, SetOptions.merge()).await()
            val user = mapDocumentToUser(usersRef.document(uid).get().await(), uid)
            preferenceDataStore.saveUser(user)
            reviewRepository.updateUserInReviews(uid, user.fullName ?: name.trim(), user.photoUrl)
            tripRepository.updateUserInTripMembers(uid, user.fullName ?: name.trim(), user.photoUrl)
            emit(NetworkResult.Success(user))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Loi"))
        }
    }

    override fun checkDatabaseStatus(): Flow<NetworkResult<Boolean>> = flow {
        try {
            usersRef.limit(1).get().await()
            emit(NetworkResult.Success(true))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Loi ket noi DB"))
        }
    }

    override fun seedTestData(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val jsonString = context.assets.open("data/seed_data.json").bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(jsonString).jsonObject
            val destinations = root["destinations"]?.jsonObject.orEmpty()
            val batch = firestore.batch()
            destinations.forEach { (key, value) ->
                val destination = json.decodeFromJsonElement<Destination>(value)
                batch.set(destinationsRef.document(key), destination.copy(id = destination.id.ifEmpty { key }))
            }
            batch.commit().await()
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Loi"))
        }
    }

    private fun JsonElement.toAny(): Any? {
        return when (this) {
            is JsonPrimitive -> {
                if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
            }
            is JsonObject -> mapValues { it.value.toAny() }
            is JsonArray -> map { it.toAny() }
        }
    }

    override fun isFirstTime(): Flow<Boolean> = preferenceDataStore.isFirstTime

    override suspend fun setFirstTime(isFirst: Boolean) = preferenceDataStore.setFirstTime(isFirst)

    override fun toggleWishlist(destinationId: String): Flow<NetworkResult<Boolean>> = flow {
        val uid = firebaseAuth.currentUser?.uid ?: return@flow
        emit(NetworkResult.Loading)
        try {
            val userRef = usersRef.document(uid)
            val added = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val current = (snapshot.get("wishlistIds") as? List<*>)?.mapNotNull { it as? String }?.toMutableList()
                    ?: mutableListOf()
                val isAdded = if (current.contains(destinationId)) {
                    current.remove(destinationId)
                    false
                } else {
                    current.add(destinationId)
                    true
                }
                transaction.set(userRef, mapOf("wishlistIds" to current), SetOptions.merge())
                isAdded
            }.await()
            preferenceDataStore.saveUser(mapDocumentToUser(userRef.get().await(), uid))
            emit(NetworkResult.Success(added))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Loi"))
        }
    }

    override fun uploadImage(path: String, byteArray: ByteArray): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try {
            emit(NetworkResult.Success(supabaseService.uploadImage(path, byteArray)))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Upload failed"))
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext NetworkResult.Error("Chua dang nhap")
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Loi")
        }
    }
}
