package com.example.travelmate.di

import com.example.travelmate.data.repository.DestinationRepositoryImpl
import com.example.travelmate.data.repository.NotificationRepositoryImpl
import com.example.travelmate.data.repository.TripRepositoryImpl
import com.example.travelmate.data.repository.ChatRepositoryImpl
import com.example.travelmate.data.repository.BudgetRepositoryImpl
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.domain.repository.NotificationRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.domain.repository.ChatRepository
import com.example.travelmate.domain.repository.BudgetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDestinationRepository(
        destinationRepositoryImpl: DestinationRepositoryImpl
    ): DestinationRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(
        tripRepositoryImpl: TripRepositoryImpl
    ): TripRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository
}
