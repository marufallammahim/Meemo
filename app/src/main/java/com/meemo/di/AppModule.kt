package com.meemo.di

import android.content.Context
import com.meemo.capability.android.ActionExecutor
import com.meemo.domain.command.CommandActionExecutor
import com.meemo.domain.command.CommandParserPort
import com.meemo.domain.usecase.ProcessCommandUseCase
import com.meemo.nlu.CommandParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCommandParser(): CommandParserPort = CommandParser()

    @Provides
    @Singleton
    fun provideActionExecutor(@ApplicationContext context: Context): CommandActionExecutor =
        ActionExecutor(context)

    @Provides
    @Singleton
    fun provideProcessCommandUseCase(
        parser: CommandParserPort,
        executor: CommandActionExecutor
    ): ProcessCommandUseCase = ProcessCommandUseCase(parser, executor)
}
