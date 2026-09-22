package com.example.liuguangcalendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun launch() {
        compose.setContent { LiuguangCalendarTheme { CalendarApp() } }
    }

    private fun monthLabel(month: YearMonth) = "${month.year}年${month.monthValue}月"

    @Test fun lunarDayNamesKeepTheirTens() {
        val expected = listOf(
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
        )
        assertEquals(expected, (1..30).map(::lunarDayName))
    }

    @Test fun toolbarOrderAndCalendarPosition() {
        launch()
        val title = compose.onNodeWithText("日历").fetchSemanticsNode().boundsInRoot
        val mode = compose.onNodeWithText("月视图").fetchSemanticsNode().boundsInRoot
        val search = compose.onNodeWithTag("glass-搜索").fetchSemanticsNode().boundsInRoot
        val today = compose.onNodeWithText("今天").fetchSemanticsNode().boundsInRoot
        assertTrue(title.right <= mode.left)
        assertTrue(mode.right <= search.left)
        assertTrue(search.right <= today.left)
        compose.onNodeWithContentDescription("更多").assertDoesNotExist()
        assertTrue(today.right <= compose.onNodeWithTag("calendar-toolbar").fetchSemanticsNode().boundsInRoot.right)
        val toolbar = compose.onNodeWithTag("calendar-toolbar").fetchSemanticsNode().boundsInRoot
        val pager = compose.onNodeWithTag("month-pager").fetchSemanticsNode().boundsInRoot
        assertTrue(pager.top - toolbar.bottom in 0f..16f)
        compose.onNodeWithText("今日待办").assertIsDisplayed()
        screenshot("calendar-v6-screen")
    }

    @Test fun swipesArrowsYearSelectionAndToday() {
        launch()
        val current = YearMonth.now()
        compose.onNodeWithTag("month-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText(monthLabel(current.plusMonths(1))).assertIsDisplayed()
        compose.onAllNodesWithContentDescription("下个月")
            .filter(SemanticsMatcher("inside viewport") { it.boundsInRoot.width > 0f })[0]
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithText(monthLabel(current.plusMonths(2))).assertIsDisplayed()
        compose.onNodeWithText("今天").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(monthLabel(current)).assertIsDisplayed()
        compose.onNodeWithText("月视图").performClick()
        compose.onNodeWithText("12月").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(monthLabel(YearMonth.of(current.year, 12))).assertIsDisplayed()
        compose.onNodeWithText("今天").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(monthLabel(current)).assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun compactScreenKeepsTasksVisible() {
        launch()
        compose.onNodeWithText("今日待办").assertIsDisplayed()
        compose.onNodeWithText("今天").assertIsDisplayed()
        screenshot("calendar-v6-screen-compact")
    }

    @Test fun monthFollowsFingerBeforeRelease() {
        launch()
        val current = YearMonth.now()
        compose.onNodeWithTag("month-pager").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.35f, 0f), delayMillis = 300)
        }
        compose.waitForIdle()
        compose.onNodeWithText(monthLabel(current)).assertIsDisplayed()
        compose.onNodeWithText(monthLabel(current.plusMonths(1))).assertIsDisplayed()
        screenshot("calendar-v6-swipe")
        compose.onNodeWithTag("month-pager").performTouchInput { up() }
    }

    private fun screenshot(name: String) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            val target = File("../outputs/$name.png")
            target.parentFile?.mkdirs()
            target.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
