package com.marufallammahim.meemo.di

import com.marufallammahim.meemo.capabilities.CapabilityRegistry
import com.marufallammahim.meemo.root.RootCommandPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CapabilityModule {

    @Provides
    @Singleton
    fun provideCapabilityRegistry(): CapabilityRegistry = CapabilityRegistry()

    @Provides
    @Singleton
    fun provideRootCommandPolicy(): RootCommandPolicy = RootCommandPolicy()
}
