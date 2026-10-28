package com.example.liuguangcalendar

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskRepositoryTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @After
    fun clear() {
        TaskRepository.save(context, emptyList())
    }

    @Test
    fun emptyStorageDoesNotCreatePresetTasks() {
        TaskRepository.save(context, emptyList())

        assertTrue(TaskRepository.load(context).isEmpty())
    }

    @Test
    fun savedTasksRoundTripWithRepeatAndCategory() {
        val task = CalendarTask(
            id = 88001L,
            title = "持久化测试",
            time = "14:30",
            date = LocalDate.of(2026, 10, 28),
            category = "家庭",
            color = Color.Yellow,
            completed = true,
            repeatType = RepeatType.Custom,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        )

        TaskRepository.save(context, listOf(task))
        val loaded = TaskRepository.load(context).single()

        assertEquals(task.id, loaded.id)
        assertEquals(task.title, loaded.title)
        assertEquals(task.time, loaded.time)
        assertEquals(task.date, loaded.date)
        assertEquals(task.category, loaded.category)
        assertEquals(task.completed, loaded.completed)
        assertEquals(task.repeatType, loaded.repeatType)
        assertEquals(task.repeatDays, loaded.repeatDays)
    }
}
