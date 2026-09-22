package com.example.liuguangcalendar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun keywordSearchIncludesCompletedAndOtherDates() {
        val today = LocalDate.now()
        val tasks = listOf(
            CalendarTask(1, "会议总结", "09:00", today.minusDays(10), "工作", Color.Blue, true),
            CalendarTask(2, "项目会议", "14:30", today.plusDays(5), "工作", Color.Blue),
            CalendarTask(3, "Team review", "12:00", today, "生活", Color.Green)
        )
        assertEquals(listOf(2L, 1L), searchTasks(tasks, " 会 ").map { it.id })
        assertEquals(listOf(2L), searchTasks(tasks, "工作 项目").map { it.id })
        assertEquals(listOf(3L), searchTasks(tasks, "REVIEW").map { it.id })
        assertTrue(searchTasks(tasks, "不存在").isEmpty())
        assertEquals(3, searchTasks(tasks, " ").size)
    }

    @Test fun searchOpensFiltersAndKeepsCompletedResults() {
        compose.setContent { LiuguangCalendarTheme { CalendarApp() } }
        compose.onNodeWithTag("glass-搜索").performClick()
        compose.onNodeWithTag("task-search").performTextInput("项目")
        compose.onNodeWithText("项目会议").assertIsDisplayed()
        compose.onNodeWithText("和家人视频").assertDoesNotExist()
        compose.onNodeWithContentDescription("标记已完成").performClick()
        compose.onNodeWithText("项目会议").assertIsDisplayed()
        compose.onNodeWithContentDescription("取消完成").assertIsDisplayed()
        screenshot("calendar-v6-search")
        compose.onNodeWithContentDescription("清空搜索").performClick()
        compose.onNodeWithTag("task-search").performTextInput("没有这条待办")
        compose.onNodeWithText("未找到相关待办").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回日历").performClick()
        compose.onNodeWithText("日历").assertIsDisplayed()
    }

    @Test fun searchResultOpensDateAndSupportsDelete() {
        compose.setContent { LiuguangCalendarTheme { CalendarApp() } }
        compose.onNodeWithTag("glass-搜索").performClick()
        compose.onNodeWithTag("task-search").performTextInput("项目")
        compose.onNodeWithText("项目会议").performClick()
        compose.onNodeWithText("当天安排").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithTag("glass-搜索").performClick()
        compose.onNodeWithContentDescription("删除待办").performClick()
        compose.onNodeWithText("未找到相关待办").assertIsDisplayed()
    }

    private fun launchFields() {
        compose.setContent {
            LiuguangCalendarTheme {
                var date by remember { mutableStateOf(LocalDate.of(2028, 1, 31)) }
                var time by remember { mutableStateOf(LocalTime.of(9, 0)) }
                Column(Modifier.padding(20.dp)) {
                    TaskDateTimeFields(date, time, { date = it }, { time = it })
                    Text("selected=$date $time")
                }
            }
        }
    }

    @Test fun dateFieldOpensWheelAndCalendarCommitsWithoutExtraButton() {
        launchFields()
        compose.onNodeWithTag("task-date").performClick()
        compose.onNodeWithTag("date-wheels").assertIsDisplayed()
        screenshot("calendar-v6-date-wheel")
        compose.onNodeWithTag("task-date-icon").performClick()
        compose.onNodeWithTag("date-wheels").assertDoesNotExist()
        compose.onNodeWithTag("compact-calendar").assertIsDisplayed()
        compose.onNodeWithTag("picker-day-2028-01-15").performClick()
        compose.onNodeWithTag("task-date-icon").performClick()
        compose.onNodeWithTag("compact-calendar").assertDoesNotExist()
        compose.onNodeWithText("2028年1月15日").assertIsDisplayed()
        compose.onNodeWithText("使用这个日期").assertDoesNotExist()
        compose.onNodeWithTag("task-date-icon").performClick()
        screenshot("calendar-v6-date-calendar")
    }

    @Test fun wheelManualInputClampsLeapYearAndPreservesExactTime() {
        launchFields()
        compose.onNodeWithTag("task-date").performClick()
        enterWheel("月", "2")
        compose.onNodeWithText("2028年2月29日").assertIsDisplayed()
        enterWheel("年", "2027")
        compose.onNodeWithText("2027年2月28日").assertIsDisplayed()
        compose.onNodeWithTag("task-time").performClick()
        enterWheel("时", "23")
        enterWheel("分", "59")
        compose.onNodeWithText("23:59").assertIsDisplayed()
        screenshot("calendar-v6-time-wheel")
        compose.onNodeWithTag("task-time-icon").performClick()
        compose.onNodeWithTag("time-wheels").assertDoesNotExist()
        compose.onNodeWithTag("compact-clock").assertIsDisplayed()
        compose.onNodeWithTag("task-time-icon").performClick()
        compose.onNodeWithTag("compact-clock").assertDoesNotExist()
        compose.onNodeWithText("23:59").assertIsDisplayed()
        compose.onNodeWithText("使用这个时间").assertDoesNotExist()
    }

    @Test fun addSheetSavesPickedDateWithoutConfirmationFooter() {
        var saved: CalendarTask? = null
        compose.setContent {
            LiuguangCalendarTheme {
                AddTaskSheet(LocalDate.of(2028, 1, 31), null, {}, { saved = it })
            }
        }
        compose.onNodeWithTag("task-title").performTextInput("日期测试")
        compose.onNodeWithTag("task-date").performScrollTo().performClick()
        screenshot("calendar-v6-add-sheet")
        compose.onNodeWithTag("task-date-icon").performScrollTo().performClick()
        compose.onNodeWithTag("picker-day-2028-01-15").performScrollTo().performClick()
        compose.onNodeWithTag("task-date-icon").performScrollTo().performClick()
        compose.onNodeWithText("保存待办").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(LocalDate.of(2028, 1, 15), saved?.date)
            assertEquals("09:00", saved?.time)
        }
    }

    @Test fun wheelRespondsToSwipeInsideSheet() {
        compose.setContent {
            LiuguangCalendarTheme { AddTaskSheet(LocalDate.of(2028, 1, 15), null, {}, {}) }
        }
        compose.onNodeWithTag("task-date").performClick()
        compose.onNodeWithTag("wheel-月").performTouchInput { swipeUp(durationMillis = 500) }
        compose.waitForIdle()
        compose.runOnIdle {
            val root = ShadowDialog.getLatestDialog().window!!.decorView
            val picker = descendants(root).filterIsInstance<NumberPicker>()
                .single { it.contentDescription == "月" }
            assertTrue("The month wheel must consume the upward swipe", picker.value > 1)
        }
    }

    @Test fun clockPickerKeepsTheWholeClockBelowTheTimeField() {
        compose.setContent {
            LiuguangCalendarTheme { AddTaskSheet(LocalDate.of(2028, 1, 15), null, {}, {}) }
        }
        compose.onNodeWithTag("task-time-icon").performScrollTo().performClick()
        compose.onNodeWithTag("compact-clock").assertIsDisplayed()
        compose.onNodeWithTag("task-time").assertIsDisplayed()
        compose.onNodeWithText("重复").assertIsDisplayed()
    }

    @Test fun clockPickerSupportsTwentyFourHourSelectionAndMovesToMinutes() {
        launchFields()
        compose.onNodeWithTag("task-time-icon").performClick()
        compose.onNodeWithTag("clock-hour-box").assertIsDisplayed()

        compose.onNodeWithTag("clock-face").performTouchInput {
            val center = Offset(width / 2f, height / 2f)
            val angle = (11 * 30.0 - 90.0) * PI / 180.0
            val radius = minOf(width, height) * 0.46f * 0.56f
            val hour23Position = Offset(
                center.x + cos(angle).toFloat() * radius,
                center.y + sin(angle).toFloat() * radius
            )
            click(hour23Position)
        }
        compose.waitForIdle()

        compose.onNodeWithText("23:00").assertIsDisplayed()
        compose.onNodeWithTag("clock-minute-box").assertIsDisplayed()
    }

    private fun enterWheel(label: String, text: String) {
        compose.runOnIdle {
            val picker = descendants(compose.activity.window.decorView)
                .filterIsInstance<NumberPicker>().single { it.contentDescription == label }
            val input = descendants(picker).filterIsInstance<EditText>().single()
            input.requestFocus()
            input.setText(text)
            input.clearFocus()
        }
        compose.waitForIdle()
    }

    private fun descendants(view: View): List<View> =
        listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
        else emptyList()

    private fun screenshot(name: String) {
        compose.runOnIdle {
            val view = ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView
                ?: compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File("../outputs/$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
