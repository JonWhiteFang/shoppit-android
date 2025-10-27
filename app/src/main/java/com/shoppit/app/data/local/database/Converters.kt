package com.shoppit.app.data.local.database

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Type converters for Room database to handle complex data types.
 * Converts between LocalDateTime/LocalDate and Long for database storage.
 */
class Converters {
    
    /**
     * Converts a timestamp (Long) to LocalDateTime.
     * @param value The timestamp in seconds since epoch (UTC)
     * @return LocalDateTime or null if value is null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
    }

    /**
     * Converts LocalDateTime to timestamp (Long).
     * @param date The LocalDateTime to convert
     * @return Timestamp in seconds since epoch (UTC) or null if date is null
     */
    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }

    /**
     * Converts a Long value to LocalDate.
     * @param value The number of days since epoch
     * @return LocalDate or null if value is null
     */
    @TypeConverter
    fun fromLocalDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    /**
     * Converts LocalDate to Long.
     * @param date The LocalDate to convert
     * @return Number of days since epoch or null if date is null
     */
    @TypeConverter
    fun localDateToLong(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}
