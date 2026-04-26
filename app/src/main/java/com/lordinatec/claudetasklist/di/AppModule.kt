package com.lordinatec.claudetasklist.di

import com.lordinatec.claudetasklist.data.repository.TaskRepository
import com.lordinatec.claudetasklist.data.repository.TaskRepositoryStub
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTaskRepository(): TaskRepository = TaskRepositoryStub()
}
