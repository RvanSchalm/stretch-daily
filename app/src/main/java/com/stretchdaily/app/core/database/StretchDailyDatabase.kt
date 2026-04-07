package com.stretchdaily.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stretchdaily.app.core.database.dao.BenchmarkDao
import com.stretchdaily.app.core.database.dao.BenchmarkLogDao
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.database.dao.SessionDao
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.model.SessionExercise
import com.stretchdaily.app.core.model.SessionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Exercise::class,
        Benchmark::class,
        BenchmarkLog::class,
        SessionRecord::class,
        SessionExercise::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StretchDailyDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun benchmarkDao(): BenchmarkDao
    abstract fun benchmarkLogDao(): BenchmarkLogDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DB_NAME = "stretch_daily.db"

        /**
         * Builds the singleton database instance and seeds the static
         * exercise + benchmark catalog the first time the file is created.
         *
         * The seed runs inside [RoomDatabase.Callback.onCreate] on a
         * background coroutine so it never blocks the UI thread. The
         * provided [scope] keeps the seed alive even if the calling
         * [Context] goes away mid-operation.
         */
        fun build(context: Context, scope: CoroutineScope): StretchDailyDatabase {
            lateinit var instance: StretchDailyDatabase
            instance = Room.databaseBuilder(
                context.applicationContext,
                StretchDailyDatabase::class.java,
                DB_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            instance.exerciseDao().insertAll(DatabaseSeeder.exercises())
                            instance.benchmarkDao().insertAll(DatabaseSeeder.benchmarks())
                        }
                    }
                })
                .build()
            return instance
        }
    }
}
