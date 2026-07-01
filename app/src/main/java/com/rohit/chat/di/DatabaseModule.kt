package com.rohit.chat.di

import android.content.Context
import androidx.room.Room
import com.rohit.chat.data.database.HuddleDatabase
import com.rohit.chat.data.database.MessageDao
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
    fun provideDatabase(@ApplicationContext context: Context): HuddleDatabase {
        return Room.databaseBuilder(
            context,
            HuddleDatabase::class.java,
            "huddle_db"
        ).build()
    }

    @Provides
    fun provideMessageDao(database: HuddleDatabase): MessageDao {
        return database.messageDao()
    }
}
