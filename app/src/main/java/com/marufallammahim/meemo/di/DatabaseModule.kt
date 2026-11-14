package com.marufallammahim.meemo.di

import android.content.Context
import androidx.room.Room
import com.marufallammahim.meemo.data.database.MeemoDatabase
import com.marufallammahim.meemo.data.database.MeemoMigrations
import com.marufallammahim.meemo.data.database.dao.MeemoDao
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
    fun provideMeemoDatabase(
        @ApplicationContext context: Context
    ): MeemoDatabase {
        return Room.databaseBuilder(
            context,
            MeemoDatabase::class.java,
            MeemoDatabase.DATABASE_NAME
        )
            .addMigrations(MeemoMigrations.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideMeemoDao(database: MeemoDatabase): MeemoDao = database.meemoDao()
}
