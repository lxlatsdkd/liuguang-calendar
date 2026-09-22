package com.example.liuguangcalendar

import android.os.Build
import android.view.MotionEvent
import android.widget.NumberPicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private enum class PickerPanel { None, DateWheel, Calendar, TimeWheel, Clock }

private val ClockInk = Color(0xFF162536)
private val ClockBlue = Color(0xFF2A8CFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDateTimeFields(
    date: LocalDate,
    time: LocalTime,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit
) {
    var panel by remember { mutableStateOf(PickerPanel.None) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    fun switchTo(target: PickerPanel) {
        focus.clearFocus()
        keyboard?.hide()
        panel = if (panel == target) PickerPanel.None else target
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PickerField(
            label = "日期",
            value = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
            tag = "task-date",
            icon = Icons.Default.CalendarToday,
            iconLabel = if (panel == PickerPanel.Calendar) "确认日期" else "日历选择日期",
            expanded = panel == PickerPanel.DateWheel || panel == PickerPanel.Calendar,
            onClick = { switchTo(PickerPanel.DateWheel) },
            onIconClick = { switchTo(PickerPanel.Calendar) }
        )
        when (panel) {
            PickerPanel.DateWheel -> Row(
                Modifier.fillMaxWidth().testTag("date-wheels"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NumberWheel(date.year, 1900..2100, "年", Modifier.weight(1.4f)) {
                    onDateChange(date.withYear(it))
                }
                NumberWheel(date.monthValue, 1..12, "月", Modifier.weight(1f)) {
                    onDateChange(date.withMonth(it))
                }
                NumberWheel(date.dayOfMonth, 1..date.lengthOfMonth(), "日", Modifier.weight(1f)) {
                    onDateChange(date.withDayOfMonth(it))
                }
            }
            PickerPanel.Calendar -> CompactDateCalendar(date, onDateChange)
            else -> Unit
        }
        PickerField(
            label = "时间",
            value = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            tag = "task-time",
            icon = Icons.Default.AccessTime,
            iconLabel = if (panel == PickerPanel.Clock) "确认时间" else "时钟选择时间",
            expanded = panel == PickerPanel.TimeWheel || panel == PickerPanel.Clock,
            onClick = { switchTo(PickerPanel.TimeWheel) },
            onIconClick = { switchTo(PickerPanel.Clock) }
        )
        when (panel) {
            PickerPanel.TimeWheel -> Row(
                Modifier.fillMaxWidth().testTag("time-wheels"),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NumberWheel(time.hour, 0..23, "时", Modifier.weight(1f)) {
                    onTimeChange(time.withHour(it))
                }
                NumberWheel(time.minute, 0..59, "分", Modifier.weight(1f)) {
                    onTimeChange(time.withMinute(it))
                }
            }
            PickerPanel.Clock -> {
                ClockPicker(time = time, onTimeChange = onTimeChange)
            }
            else -> Unit
        }
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    tag: String,
    icon: ImageVector,
    iconLabel: String,
    expanded: Boolean,
    onClick: () -> Unit,
    onIconClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.42f),
        border = BorderStroke(
            if (expanded) 2.dp else 1.dp,
            if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).clickable(onClick = onClick).testTag(tag)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.62f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.95f), CircleShape)
                    .clickable(onClick = onIconClick)
                    .testTag("$tag-icon"),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = iconLabel, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private enum class ClockPart { Hour, Minute }

@Composable
private fun ClockPicker(time: LocalTime, onTimeChange: (LocalTime) -> Unit) {
    var part by remember { mutableStateOf(ClockPart.Hour) }
    var faceSize by remember { mutableStateOf(IntSize.Zero) }
    val values = if (part == ClockPart.Minute) (0..55 step 5).toList() else (0..23).toList()
    val selected = if (part == ClockPart.Minute) time.minute / 5 * 5 else time.hour
    Column(
        modifier = Modifier.fillMaxWidth().testTag("compact-clock"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClockValueBox(
                text = "%02d".format(time.hour),
                tag = "clock-hour-box",
                selected = part == ClockPart.Hour,
                onClick = { part = ClockPart.Hour }
            )
            Text(":", fontSize = 34.sp, color = ClockInk, modifier = Modifier.padding(horizontal = 5.dp))
            ClockValueBox(
                text = "%02d".format(time.minute),
                tag = "clock-minute-box",
                selected = part == ClockPart.Minute,
                onClick = { part = ClockPart.Minute }
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(276.dp)
                .aspectRatio(1f)
                .testTag("clock-face")
                .onSizeChanged { faceSize = it }
                .pointerInput(part, time, faceSize) {
                    detectTapGestures(onTap = { tap ->
                        if (faceSize == IntSize.Zero) return@detectTapGestures
                        val center = Offset(faceSize.width / 2f, faceSize.height / 2f)
                        val dx = tap.x - center.x
                        val dy = tap.y - center.y
                        val angle = (atan2(dy, dx) * 180.0 / PI + 90.0 + 360.0) % 360.0
                        val index = ((angle / 30.0).roundToInt()) % 12
                        if (part == ClockPart.Minute) {
                            onTimeChange(time.withMinute(index * 5))
                        } else {
                            val distance = hypot(dx, dy)
                            val outerRing = faceSize.width * 0.67f
                            val hour = if (distance >= outerRing) index else index + 12
                            onTimeChange(time.withHour(hour))
                            part = ClockPart.Minute
                        }
                    })
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.46f
                drawCircle(Color(0xFFE9E3EE), radius = radius)

                val selectedIndex = if (part == ClockPart.Minute) selected / 5 else selected % 12
                val selectedRing = if (part == ClockPart.Hour && selected >= 12) 0.56f else 0.78f
                val selectedAngle = (selectedIndex * 30.0 - 90.0) * PI / 180.0
                val handEnd = Offset(
                    center.x + cos(selectedAngle).toFloat() * radius * selectedRing,
                    center.y + sin(selectedAngle).toFloat() * radius * selectedRing
                )
                drawLine(ClockBlue, center, handEnd, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(ClockBlue, radius = 8.dp.toPx(), center = center)
                drawCircle(ClockBlue, radius = 26.dp.toPx(), center = handEnd)

                values.forEach { value ->
                    val index = if (part == ClockPart.Minute) value / 5 else value % 12
                    val ring = if (part == ClockPart.Hour && value >= 12) 0.56f else 0.78f
                    val angle = (index * 30.0 - 90.0) * PI / 180.0
                    val x = center.x + cos(angle).toFloat() * radius * ring
                    val y = center.y + sin(angle).toFloat() * radius * ring
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (value == selected) android.graphics.Color.WHITE else android.graphics.Color.rgb(22, 37, 54)
                        textSize = 17.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
                    }
                    drawContext.canvas.nativeCanvas.drawText(value.toString(), x, y - (paint.ascent() + paint.descent()) / 2f, paint)
                }
            }
        }
    }
}

@Composable
private fun ClockValueBox(
    text: String,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 82.dp, height = 62.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFE9D8FF) else Color(0xFFE9E4EC))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color(0xFF32126E) else ClockInk,
            fontSize = 34.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NumberWheel(
    value: Int,
    range: IntRange,
    label: String,
    modifier: Modifier,
    onValueChange: (Int) -> Unit
) {
    val latestChange by rememberUpdatedState(onValueChange)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(144.dp).testTag("wheel-$label"),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    this.value = value
                    wrapSelectorWheel = false
                    contentDescription = label
                    setFormatter { if (label == "年") it.toString() else "%02d".format(it) }
                    if (Build.VERSION.SDK_INT >= 29) {
                        textColor = android.graphics.Color.rgb(22, 37, 54)
                        textSize = 20f * resources.displayMetrics.scaledDensity
                    }
                    setOnValueChangedListener { _, _, next -> latestChange(next) }
                    // Keep vertical wheel gestures out of the sheet's scrolling/drag handling.
                    setOnTouchListener { view, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                        } else if (event.actionMasked == MotionEvent.ACTION_UP ||
                            event.actionMasked == MotionEvent.ACTION_CANCEL) {
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
                }
            },
            update = {
                it.minValue = range.first
                it.maxValue = range.last
                if (it.value != value) it.value = value
            }
        )
    }
}

@Composable
internal fun CompactDateCalendar(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.from(date)) }
    Column(Modifier.fillMaxWidth().testTag("compact-calendar")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }, enabled = month > YearMonth.of(1900, 1)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "选择器上个月")
            }
            Text(
                "${month.year}年${month.monthValue}月",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.of(2100, 12)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "选择器下个月")
            }
        }
        Row {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val offset = month.atDay(1).dayOfWeek.value - 1
        repeat(6) { week ->
            Row {
                repeat(7) { column ->
                    val number = week * 7 + column - offset + 1
                    val cellDate = if (number in 1..month.lengthOfMonth()) month.atDay(number) else null
                    Box(
                        modifier = Modifier.weight(1f).height(34.dp)
                            .padding(2.dp)
                            .background(
                                if (cellDate == date) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = cellDate != null) { cellDate?.let(onDateChange) }
                            .testTag("picker-day-$cellDate"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cellDate != null) Text(
                            number.toString(), fontSize = 14.sp,
                            color = if (cellDate == date) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
