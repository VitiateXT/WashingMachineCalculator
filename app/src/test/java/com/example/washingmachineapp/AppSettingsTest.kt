package de.moritz.waschzeitrechner

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {

    @Test
    fun fromStringReturnsSystemForUnknownValue() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("UNKNOWN"))
    }

    @Test
    fun fromStringReturnsSystemForEmptyString() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""))
    }

    @Test
    fun fromStringParsesLightCorrectly() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
    }

    @Test
    fun fromStringParsesDarkCorrectly() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
    }

    @Test
    fun fromStringParsesSystemCorrectly() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"))
    }

    @Test
    fun fromStringIsCaseSensitive() {
        // lowercase is not a valid entry name, falls back to SYSTEM
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("light"))
    }
}
