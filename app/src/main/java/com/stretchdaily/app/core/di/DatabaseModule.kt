package com.stretchdaily.app.core.di

import android.content.Context
import com.stretchdaily.app.core.database.StretchDailyDatabase
import com.stretchdaily.app.core.database.dao.BenchmarkDao
import com.stretchdaily.app.core.database.dao.BenchmarkLogDao
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.database.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope
    ): StretchDailyDatabase = StretchDailyDatabase.build(context, scope)

    @Provides
    fun provideExerciseDao(db: StretchDailyDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideBenchmarkDao(db: StretchDailyDatabase): BenchmarkDao = db.benchmarkDao()

    @Provides
    fun provideBenchmarkLogDao(db: StretchDailyDatabase): BenchmarkLogDao = db.benchmarkLogDao()

    @Provides
    fun provideSessionDao(db: StretchDailyDatabase): SessionDao = db.sessionDao()
}
