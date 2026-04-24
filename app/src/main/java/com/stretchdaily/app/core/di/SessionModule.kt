package com.stretchdaily.app.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for session-adjacent singletons. `TodaySessionHolder` itself
 * uses `@Inject constructor` so no explicit `@Provides` is required — this
 * empty object exists purely to anchor the `core/di/` convention and give
 * future R4+ code (e.g. session player caches) an obvious home.
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionModule
