package com.stretchdaily.app.core.di

import com.stretchdaily.app.core.util.Clock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for things the engine needs that aren't constructor-injectable.
 *
 * The engine components themselves ([CategoryWeightCalculator],
 * [SelectionShield], [SessionBuilder], [LongevityEngine]) all use
 * `@Inject constructor`, so Hilt finds them automatically — no @Provides
 * needed for those.
 */
@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock { System.currentTimeMillis() }
}
