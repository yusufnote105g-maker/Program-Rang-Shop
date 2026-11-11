package com.example.habittracker.data.di

import android.content.Context
import androidx.room.Room
import com.example.habittracker.data.local.AppDatabase
import com.example.habittracker.data.local.dao.TaskDao
import com.example.habittracker.data.local.dao.TaskLogDao
import com.example.habittracker.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideTaskLogDao(db: AppDatabase): TaskLogDao = db.taskLogDao()

    /** Часы инжектируем, чтобы логику дня/streak можно было тестировать. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
