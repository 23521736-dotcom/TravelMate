package com.example.travelmate.di

import android.content.Context
import androidx.room.Room
import com.example.travelmate.data.local.AppDatabase
import com.example.travelmate.data.local.DestinationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "travelmate_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideDestinationDao(appDatabase: AppDatabase): DestinationDao {
        return appDatabase.destinationDao()
    }
}
