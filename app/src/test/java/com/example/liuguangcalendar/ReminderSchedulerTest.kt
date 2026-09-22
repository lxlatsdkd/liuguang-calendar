package com.example.liuguangcalendar

import android.content.Context
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSchedulerTest {
    @Test fun completionStateIsStoredForTheReminderOccurrence() {
        val context = RuntimeEnvironment.getApplication() as Context
        val date = LocalDate.of(2026, 9, 22)
        val otherDate = date.plusDays(1)

        ReminderScheduler.setCompleted(context, 99001L, date, false)
        assertFalse(ReminderScheduler.isCompleted(context, 99001L, date))

        ReminderScheduler.setCompleted(context, 99001L, date, true)
        assertTrue(ReminderScheduler.isCompleted(context, 99001L, date))
        assertFalse(ReminderScheduler.isCompleted(context, 99001L, otherDate))
    }

    @Test fun reminderIntentKeepsTheDateThatWasActuallyReminded() {
        val intent = Intent()
            .putExtra("task_id", 99002L)
            .putExtra("title", "测试提醒")
            .putExtra("time", "14:30")
            .putExtra("date", "2026-09-25")
            .putExtra("start_date", "2026-09-22")
            .putExtra("repeat", RepeatType.Once.name)

        val task = ReminderScheduler.nextTaskFromIntent(intent)

        assertTrue(task != null)
        assertTrue(
            ReminderScheduler.occurrenceDateFromIntent(intent) == LocalDate.of(2026, 9, 25)
        )
    }
}
