package com.abhishek.zerodroid.core.di

import android.content.Context
import androidx.room.Room
import com.abhishek.zerodroid.core.database.AppDatabase
import com.abhishek.zerodroid.core.database.dao.AlertDao
import com.abhishek.zerodroid.core.database.dao.SessionDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "zerodroid.db")
            .addMigrations()
            .build()

    @Provides
    fun provideAlertDao(database: AppDatabase): AlertDao = database.alertDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()
}
