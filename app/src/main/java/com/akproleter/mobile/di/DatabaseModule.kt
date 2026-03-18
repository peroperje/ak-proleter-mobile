package com.akproleter.mobile.di

import android.content.Context
import androidx.room.Room
import com.akproleter.mobile.data.local.AkProleterDao
import com.akproleter.mobile.data.local.AkProleterDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AkProleterDatabase {
        return Room.databaseBuilder(
            context,
            AkProleterDatabase::class.java,
            AkProleterDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideDao(database: AkProleterDatabase): AkProleterDao {
        return database.dao()
    }
}
