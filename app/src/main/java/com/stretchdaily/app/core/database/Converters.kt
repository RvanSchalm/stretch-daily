package com.stretchdaily.app.core.database

import androidx.room.TypeConverter
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters for Stretch Daily.
 *
 * Lists and maps go through kotlinx.serialization JSON so the schema stays
 * stable and human-readable. Enums are stored by `name` so renaming them is a
 * deliberate, traceable migration step.
 */
class Converters {

    private val json = Json { encodeDefaults = true }
    private val stringListSerializer = ListSerializer(String.serializer())
    private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())

    @TypeConverter
    fun stringListToJson(value: List<String>): String =
        json.encodeToString(stringListSerializer, value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        json.decodeFromString(stringListSerializer, value)

    @TypeConverter
    fun stringMapToJson(value: Map<String, String>): String =
        json.encodeToString(stringMapSerializer, value)

    @TypeConverter
    fun jsonToStringMap(value: String): Map<String, String> =
        json.decodeFromString(stringMapSerializer, value)

    @TypeConverter
    fun categoryToString(value: Category): String = value.name

    @TypeConverter
    fun stringToCategory(value: String): Category = Category.valueOf(value)

    @TypeConverter
    fun tierToString(value: FlexibilityTier): String = value.name

    @TypeConverter
    fun stringToTier(value: String): FlexibilityTier = FlexibilityTier.valueOf(value)

    @TypeConverter
    fun inputTypeToString(value: BenchmarkInputType): String = value.name

    @TypeConverter
    fun stringToInputType(value: String): BenchmarkInputType = BenchmarkInputType.valueOf(value)
}
