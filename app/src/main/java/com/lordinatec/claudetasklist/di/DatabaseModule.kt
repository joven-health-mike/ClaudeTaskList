package com.lordinatec.claudetasklist.di

import android.content.Context
import androidx.room.Room
import com.lordinatec.claudetasklist.data.dao.TagDao
import com.lordinatec.claudetasklist.data.dao.TaskDao
import com.lordinatec.claudetasklist.data.db.TaskDatabase
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
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase =
        Room.databaseBuilder(context, TaskDatabase::class.java, "claudetasklist.db")
            .addCallback(TaskDatabase.seedCallback)
            .build()

    @Provides
    fun provideTaskDao(db: TaskDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideTagDao(db: TaskDatabase): TagDao = db.tagDao()
}
