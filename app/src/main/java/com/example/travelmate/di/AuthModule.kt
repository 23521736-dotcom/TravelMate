package com.example.travelmate.di

import android.content.Context
import com.example.travelmate.data.local.PreferenceDataStore
import com.example.travelmate.data.remote.SupabaseService
import com.example.travelmate.data.repository.AuthRepositoryImpl
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.domain.repository.TripRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        preferenceDataStore: PreferenceDataStore,
        supabaseService: SupabaseService,
        reviewRepository: ReviewRepository,
        tripRepository: TripRepository,
        json: Json,
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepositoryImpl(
            firebaseAuth, 
            firestore, 
            preferenceDataStore, 
            supabaseService, 
            reviewRepository, 
            tripRepository, 
            json, 
            context
        )
    }
}
