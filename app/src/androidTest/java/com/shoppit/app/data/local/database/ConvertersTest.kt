package com.shoppit.app.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Integration tests for Room TypeConverters.
 * Validates database type conversion functionality.
 */
class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun fromTimestamp_convertsLongToLocalDateTime() {
        // Given
        val timestamp = 1609459200L // 2021-01-01 00:00:00 UTC

        // When
        val result = converters.fromTimestamp(timestamp)

        // Then
        assertEquals(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC), result)
    }

    @Test
    fun fromTimestamp_returnsNullForNullInput() {
        // When
        val result = converters.fromTimestamp(null)

        // Then
        assertNull(result)
    }

    @Test
    fun dateToTimestamp_convertsLocalDateTimeToLong() {
        // Given
        val dateTime = LocalDateTime.of(2021, 1, 1, 0, 0, 0)

        // When
        val result = converters.dateToTimestamp(dateTime)

        // Then
        assertEquals(dateTime.toEpochSecond(ZoneOffset.UTC), result)
    }

    @Test
    fun dateToTimestamp_returnsNullForNullInput() {
        // When
        val result = converters.dateToTimestamp(null)

        // Then
        assertNull(result)
    }

    @Test
    fun fromLocalDate_convertsLongToLocalDate() {
        // Given
        val epochDay = 18628L // 2021-01-01

        // When
        val result = converters.fromLocalDate(epochDay)

        // Then
        assertEquals(LocalDate.ofEpochDay(epochDay), result)
    }

    @Test
    fun fromLocalDate_returnsNullForNullInput() {
        // When
        val result = converters.fromLocalDate(null)

        // Then
        assertNull(result)
    }

    @Test
    fun localDateToLong_convertsLocalDateToLong() {
        // Given
        val date = LocalDate.of(2021, 1, 1)

        // When
        val result = converters.localDateToLong(date)

        // Then
        assertEquals(date.toEpochDay(), result)
    }

    @Test
    fun localDateToLong_returnsNullForNullInput() {
        // When
        val result = converters.localDateToLong(null)

        // Then
        assertNull(result)
    }

    @Test
    fun roundTrip_localDateTime_preservesValue() {
        // Given
        val original = LocalDateTime.of(2021, 6, 15, 14, 30, 45)

        // When
        val timestamp = converters.dateToTimestamp(original)
        val restored = converters.fromTimestamp(timestamp)

        // Then
        assertEquals(original, restored)
    }

    @Test
    fun roundTrip_localDate_preservesValue() {
        // Given
        val original = LocalDate.of(2021, 6, 15)

        // When
        val epochDay = converters.localDateToLong(original)
        val restored = converters.fromLocalDate(epochDay)

        // Then
        assertEquals(original, restored)
    }
}
