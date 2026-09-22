package com.example.liuguangcalendar

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HolidayRepositoryTest {
    @Test fun builtIn2026ContainsOffDaysAndWorkdays() {
        val data = HolidayRepository.builtIn(2026)
        assertEquals("休", data.getValue(java.time.LocalDate.of(2026, 9, 25)).marker)
        assertEquals("中秋节", data.getValue(java.time.LocalDate.of(2026, 9, 25)).name)
        assertEquals("班", data.getValue(java.time.LocalDate.of(2026, 9, 20)).marker)
        assertEquals("休", data.getValue(java.time.LocalDate.of(2026, 10, 1)).marker)
    }

    @Test fun loadFallsBackOfflineAndWritesUsableCache() = runBlocking {
        val context = RuntimeEnvironment.getApplication() as Context
        val data = HolidayRepository.load(context, 2026)
        assertTrue(data.isNotEmpty())
        assertEquals("班", data.getValue(java.time.LocalDate.of(2026, 9, 20)).marker)
        assertEquals("休", data.getValue(java.time.LocalDate.of(2026, 9, 25)).marker)
    }

    @Test fun futureYearHasFestivalNamesBeforeOfficialSchedule() {
        val data = HolidayRepository.builtIn(2027)
        assertTrue(data.values.any { it.name == "春节" && it.marker == null })
        assertTrue(data.values.any { it.name == "端午节" && it.marker == null })
        assertTrue(data.values.any { it.name == "中秋节" && it.marker == null })
    }
}
