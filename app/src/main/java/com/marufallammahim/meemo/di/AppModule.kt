package com.marufallammahim.meemo.di

import com.marufallammahim.meemo.assistant.engine.AiInterpreter
import com.marufallammahim.meemo.assistant.engine.DefaultAiInterpreter
import com.marufallammahim.meemo.core.common.DefaultDispatcherProvider
import com.marufallammahim.meemo.core.common.DispatcherProvider
import com.marufallammahim.meemo.core.permissions.AndroidPermissionChecker
import com.marufallammahim.meemo.core.permissions.PermissionChecker
import com.marufallammahim.meemo.core.utilities.SystemTimeProvider
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.data.repository.MeemoRepositoryImpl
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindPermissionChecker(impl: AndroidPermissionChecker): PermissionChecker

    @Binds
    @Singleton
    abstract fun bindMeemoRepository(impl: MeemoRepositoryImpl): MeemoRepository

    @Binds
    @Singleton
    abstract fun bindAiInterpreter(impl: DefaultAiInterpreter): AiInterpreter
}
