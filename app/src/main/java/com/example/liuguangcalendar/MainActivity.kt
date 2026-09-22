package com.example.liuguangcalendar

import android.icu.util.ChineseCalendar
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Locale
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private val AppBackground = Color(0xFFF3F8FC)
private val GlassWhite = Color(0xDDFEFFFF)
private val GlassTint = Color(0xC9FFFFFF)
private val Ink = Color(0xFF162536)
private val MutedInk = Color(0xFF7F8C9A)
private val Blue = Color(0xFF2A8CFF)
private val Teal = Color(0xFF13B9B3)
private val Coral = Color(0xFFFF6D62)
private val WarmYellow = Color(0xFFFFB51B)
private val Divider = Color(0x1A6B8298)
private val lunarLabelCache = mutableMapOf<LocalDate, String>()

data class CalendarTask(
    val id: Long,
    val title: String,
    val time: String,
    val date: LocalDate,
    val category: String,
    val color: Color,
    val completed: Boolean = false,
    val repeatType: RepeatType = RepeatType.Once,
    val repeatDays: Set<DayOfWeek> = emptySet()
)

enum class RepeatType {
    Once,
    Daily,
    Weekdays,
    Weekly,
    Custom
}

private data class CalendarDay(
    val date: LocalDate?,
    val lunar: String = "",
    val holiday: HolidayInfo? = null,
    val isCurrentMonth: Boolean = true
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        ReminderScheduler.createChannel(this)
        setContent {
            LiuguangCalendarTheme {
                CalendarApp()
            }
        }
        // Defer launcher alias work and the permission dialog until the first frame is visible.
        window.decorView.post {
            LauncherIconUpdater.schedule(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1401)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !ReminderScheduler.canScheduleExactAlarms(this)) {
                startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }
    }
}

@Composable
internal fun LiuguangCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            background = AppBackground,
            surface = GlassWhite,
            onSurface = Ink,
            outline = Divider
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarApp() {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val anchorMonth = remember { YearMonth.from(today) }
    var holidaysByYear by remember {
        mutableStateOf(mapOf(today.year to HolidayRepository.builtIn(today.year)))
    }
    LaunchedEffect(Unit) {
        val cached = HolidayRepository.loadCached(context, today.year)
        if (cached != null) holidaysByYear = holidaysByYear + (today.year to cached)
    }
    val pagerState = rememberPagerState(initialPage = 1200, pageCount = { 2401 })
    val visibleMonth = anchorMonth.plusMonths((pagerState.currentPage - 1200).toLong())
    val scope = rememberCoroutineScope()
    LaunchedEffect(visibleMonth.year) {
        val year = visibleMonth.year
        val holidays = HolidayRepository.load(context, year)
        holidaysByYear = holidaysByYear + (year to holidays)
    }
    val navigateMonth: (YearMonth) -> Unit = { month ->
        val page = (1200 + ChronoUnit.MONTHS.between(anchorMonth, month).toInt()).coerceIn(0, 2400)
        scope.launch { pagerState.animateScrollToPage(page, animationSpec = tween(300)) }
    }
    var selectedDate by remember { mutableStateOf(today) }
    var showYearView by remember { mutableStateOf(false) }
    var browsingYear by remember { mutableStateOf(today.year) }
    var showDayDetail by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    val tasks = remember {
        mutableStateListOf(
            CalendarTask(1, "早起运动", "09:00", today, "生活", Teal, true),
            CalendarTask(2, "项目会议", "14:30", today, "工作", Blue, ReminderScheduler.isCompleted(context, 2, today)),
            CalendarTask(3, "整理本周计划", "16:00", today, "生活", WarmYellow, ReminderScheduler.isCompleted(context, 3, today)),
            CalendarTask(4, "和家人视频", "20:00", today, "家庭", Blue, ReminderScheduler.isCompleted(context, 4, today))
        )
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != ReminderScheduler.ACTION_TASK_UPDATED) return
                val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
                val completed = intent.getBooleanExtra(ReminderScheduler.EXTRA_COMPLETED, false)
                val index = tasks.indexOfFirst { it.id == taskId }
                if (index >= 0) {
                    tasks[index] = tasks[index].copy(completed = completed)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ReminderScheduler.ACTION_TASK_UPDATED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    val selectedTasks = tasks
        .filter { it.occursOn(selectedDate) }
        .sortedWith(compareBy<CalendarTask> { it.completed }.thenBy { it.time })

    BackHandler(enabled = showDayDetail) {
        showDayDetail = false
    }
    BackHandler(enabled = showSearch) { showSearch = false }

    val toggleTask: (Long) -> Unit = { id ->
        val index = tasks.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updated = tasks[index].copy(completed = !tasks[index].completed)
            tasks[index] = updated
            ReminderScheduler.setCompleted(context, updated.id, selectedDate, updated.completed)
        }
    }
    val deleteTask: (Long) -> Unit = { id ->
        ReminderScheduler.cancel(context, id)
        tasks.removeAll { it.id == id }
    }

    if (showSearch) {
        TaskSearchScreen(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            tasks = tasks,
            onBack = { showSearch = false },
            onOpen = {
                selectedDate = it.date
                showSearch = false
                showDayDetail = true
            },
            onToggle = toggleTask,
            onEdit = { editingTaskId = it.id; showAddSheet = true },
            onDelete = deleteTask
        )
    } else if (showDayDetail) {
        DayDetailScreen(
            selectedDate = selectedDate,
            tasks = selectedTasks,
            holiday = holidaysByYear[selectedDate.year]?.get(selectedDate),
            onBack = { showDayDetail = false },
            onAdd = { showAddSheet = true },
            onToggle = toggleTask,
            onEdit = {
                editingTaskId = it.id
                showAddSheet = true
            },
            onDelete = deleteTask
        )
    } else {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.navigationBars
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(padding)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                ) {
                    AppTopBar(
                        showYearView = showYearView,
                        onSearch = { showSearch = true },
                        onToggleView = {
                            browsingYear = visibleMonth.year
                            showYearView = !showYearView
                        },
                        onToday = {
                            showYearView = false
                            selectedDate = LocalDate.now()
                            navigateMonth(YearMonth.from(selectedDate))
                        }
                    )
                    if (showYearView) {
                        YearCalendarCard(
                            year = browsingYear,
                            onPreviousYear = { browsingYear -= 1 },
                            onNextYear = { browsingYear += 1 },
                            onMonthSelected = {
                                showYearView = false
                                navigateMonth(it)
                            }
                        )
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            key = { page ->
                                anchorMonth.plusMonths((page - 1200).toLong()).toString()
                            },
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth().testTag("month-pager")
                        ) { page ->
                            val month = anchorMonth.plusMonths((page - 1200).toLong())
                            val holidays = holidaysByYear[month.year] ?: HolidayRepository.builtIn(month.year)
                            val days = remember(month, holidays) { buildCalendarDays(month, holidays) }
                            MonthCalendarCard(
                                visibleMonth = month,
                                todayDate = today.takeIf { YearMonth.from(it) == month },
                                selectedDate = selectedDate.takeIf { YearMonth.from(it) == month },
                                days = days,
                                onMonthClick = { showMonthPicker = true },
                                onPrevious = { navigateMonth(month.minusMonths(1)) },
                                onNext = { navigateMonth(month.plusMonths(1)) },
                                onDateSelected = {
                                    selectedDate = it
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    TodayTasksCard(
                        modifier = Modifier.weight(1f),
                        selectedDate = selectedDate,
                        tasks = selectedTasks,
                        holiday = holidaysByYear[selectedDate.year]?.get(selectedDate),
                        onToggle = toggleTask,
                        onAdd = { showAddSheet = true },
                        onEdit = {
                            editingTaskId = it.id
                            showAddSheet = true
                        },
                        onDelete = deleteTask
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            selectedDate = selectedDate,
            initialTask = editingTaskId?.let { id -> tasks.firstOrNull { it.id == id } },
            onDismiss = { showAddSheet = false; editingTaskId = null },
            onSave = { savedTask ->
                val index = tasks.indexOfFirst { it.id == savedTask.id }
                if (index >= 0) {
                    tasks[index] = savedTask
                } else {
                    tasks.add(savedTask)
                }
                ReminderScheduler.schedule(context, savedTask)
                showAddSheet = false
                editingTaskId = null
            }
        )
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            month = visibleMonth,
            onDismiss = { showMonthPicker = false },
            onSelect = {
                navigateMonth(it)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun AppTopBar(
    showYearView: Boolean,
    onSearch: () -> Unit,
    onToggleView: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("calendar-toolbar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "日历",
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onToggleView, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(if (showYearView) "年视图" else "月视图", color = MutedInk, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        GlassIconButton(onClick = onSearch, contentDescription = "搜索") {
            Icon(Icons.Default.Search, contentDescription = null, tint = Ink)
        }
        GlassTextButton(onClick = onToday, contentDescription = "今天") {
            Text("今天", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.82f), Color(0xBDE9F4FF)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.92f), CircleShape)
            .clickable(onClick = onClick)
            .testTag("glass-$contentDescription"),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun GlassTextButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(19.dp))
            .clickable(onClick = onClick)
            .testTag("glass-$contentDescription")
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun MonthCalendarCard(
    visibleMonth: YearMonth,
    todayDate: LocalDate?,
    selectedDate: LocalDate?,
    days: List<CalendarDay>,
    onMonthClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    Surface(
        color = GlassWhite,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${visibleMonth.year}年${visibleMonth.monthValue}月",
                    color = Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onMonthClick)
                        .padding(start = 8.dp, top = 2.dp, bottom = 10.dp)
                )
                GlassIconButton(onClick = onPrevious, contentDescription = "上个月") {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "上个月", tint = Ink)
                }
                GlassIconButton(onClick = onNext, contentDescription = "下个月") {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "下个月", tint = Ink)
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, label ->
                    Text(
                        text = label,
                        color = if (index >= 5) Coral else Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // Reserve six equal week rows so adjacent months cannot move the task panel.
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().height(62.dp)) {
                    week.forEach { day ->
                        CalendarDayCell(
                            day = day,
                            today = day.date == todayDate,
                            selected = day.date == selectedDate,
                            onClick = { day.date?.let(onDateSelected) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearCalendarCard(
    year: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit
) {
    Surface(
        color = GlassWhite,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(onClick = onPreviousYear, contentDescription = "上一年") {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "上一年", tint = Ink)
                }
                Text(
                    text = "${year}年",
                    color = Ink,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                GlassIconButton(onClick = onNextYear, contentDescription = "下一年") {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "下一年", tint = Ink)
                }
            }
            (1..12).chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { month ->
                        val monthValue = YearMonth.of(year, month)
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onMonthSelected(monthValue) }
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text("${month}月", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${monthValue.lengthOfMonth()}天",
                                    color = MutedInk,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    today: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWeekend = day.date?.dayOfWeek == DayOfWeek.SATURDAY || day.date?.dayOfWeek == DayOfWeek.SUNDAY
    val isHighlighted = today || selected
    val highlightColor = when {
        today -> Color(0xFF1264D8)
        selected -> Color(0xFF7B61C9)
        else -> Color.Transparent
    }
    val dateColor = when {
        isHighlighted -> Color.White
        !day.isCurrentMonth -> MutedInk.copy(alpha = 0.6f)
        isWeekend || day.holiday != null -> Coral
        else -> Ink
    }
    Box(
        modifier = modifier
            .padding(vertical = 3.dp, horizontal = 2.dp)
            .testTag("day-${day.date}")
            .clickable(enabled = day.date != null, onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(highlightColor)
                .padding(top = 8.dp, bottom = 2.dp)
        ) {
            Text(
                text = day.date?.dayOfMonth?.toString() ?: "",
                color = dateColor,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = day.holiday?.name ?: day.lunar,
                color = if (isHighlighted) Color.White else if (day.holiday?.name != null) Coral else MutedInk,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isHighlighted) Color.White else if (day.holiday?.name != null) Coral else Color.Transparent)
            )
        }
        day.holiday?.marker?.let { marker ->
            Text(
                text = marker,
                color = if (marker == "休") Color(0xFF35A36B) else Coral,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd)
                    .offset(y = (-3).dp)
                    .testTag("holiday-${day.date}")
                    .background(GlassWhite, RoundedCornerShape(3.dp))
                    .padding(horizontal = 2.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun TodayTasksCard(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    tasks: List<CalendarTask>,
    holiday: HolidayInfo?,
    onToggle: (Long) -> Unit,
    onAdd: () -> Unit,
    onEdit: (CalendarTask) -> Unit,
    onDelete: (Long) -> Unit
) {
    val dayInfo = buildString {
        append(lunarLabel(selectedDate))
        holiday?.name?.let { append(" · ").append(it) }
        holiday?.marker?.let { append(" · ").append(if (it == "休") "休息日" else "工作日") }
    }
    Surface(
        color = GlassTint,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.84f), RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD4E0EC))
                    .align(Alignment.CenterHorizontally)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (selectedDate == LocalDate.now()) "今日待办" else "当天待办",
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.SIMPLIFIED_CHINESE)),
                        color = MutedInk,
                        fontSize = 13.sp
                    )
                    Text(
                        text = dayInfo,
                        color = if (holiday != null) Coral else MutedInk,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                GlassAddButton(onClick = onAdd, large = false) {
                    Icon(Icons.Default.Add, contentDescription = "添加待办")
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggle(task.id) },
                        onEdit = { onEdit(task) },
                        onDelete = { onDelete(task.id) }
                    )
                }
                if (tasks.isEmpty()) {
                    item {
                        Text(
                            text = "今天还没有安排",
                            color = MutedInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailScreen(
    selectedDate: LocalDate,
    tasks: List<CalendarTask>,
    holiday: HolidayInfo?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onToggle: (Long) -> Unit,
    onEdit: (CalendarTask) -> Unit,
    onDelete: (Long) -> Unit
) {
    val lunar = lunarLabel(selectedDate)
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.navigationBars,
        floatingActionButton = {
            GlassAddButton(
                onClick = onAdd,
                large = true,
                modifier = Modifier.padding(end = 12.dp, bottom = 10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加待办")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "返回", tint = Ink)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.SIMPLIFIED_CHINESE)),
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(lunar)
                            holiday?.name?.let { append(" · ").append(it) }
                            holiday?.marker?.let { append(" · ").append(it) }
                        },
                        color = if (holiday != null) Coral else MutedInk,
                        fontSize = 14.sp
                    )
                }
            }
            Surface(
                color = GlassWhite,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.84f), RoundedCornerShape(28.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("当天安排", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("全部待办和提醒", color = MutedInk, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggle = { onToggle(task.id) },
                                onEdit = { onEdit(task) },
                                onDelete = { onDelete(task.id) }
                            )
                        }
                        if (tasks.isEmpty()) {
                            item {
                                Text(
                                    text = "这一天还没有安排",
                                    color = MutedInk,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 42.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassAddButton(
    onClick: () -> Unit,
    large: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(if (large) 58.dp else 48.dp)
            .clip(if (large) CircleShape else RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF5DA8FF).copy(alpha = 0.94f), Color(0xFF1976E8).copy(alpha = 0.86f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.78f), if (large) CircleShape else RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
internal fun TaskRow(
    task: CalendarTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpen: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (task.completed) Teal.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 2.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (task.completed) "取消完成" else "标记已完成",
                tint = if (task.completed) Teal else MutedInk
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)
            .clickable(enabled = onOpen != null) { onOpen?.invoke() }) {
            Text(
                text = task.title,
                color = if (task.completed) MutedInk else Ink,
                fontSize = 16.sp,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
            )
            Text(text = task.time, color = MutedInk, fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(task.color)
            )
            Spacer(Modifier.width(4.dp))
            Text(task.category, color = MutedInk, fontSize = 12.sp)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "修改待办", tint = Blue)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除待办", tint = Coral)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AddTaskSheet(
    selectedDate: LocalDate,
    initialTask: CalendarTask?,
    onDismiss: () -> Unit,
    onSave: (CalendarTask) -> Unit
) {
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title.orEmpty()) }
    var taskDate by remember(initialTask?.id) { mutableStateOf(initialTask?.date ?: selectedDate) }
    var taskTime by remember(initialTask?.id) {
        mutableStateOf(initialTask?.time?.let { LocalTime.parse(it) } ?: LocalTime.of(9, 0))
    }
    var category by remember(initialTask?.id) { mutableStateOf(initialTask?.category ?: "工作") }
    var repeatType by remember(initialTask?.id) { mutableStateOf(initialTask?.repeatType ?: RepeatType.Once) }
    var repeatDays by remember(initialTask?.id) { mutableStateOf(initialTask?.repeatDays ?: emptySet()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FCFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initialTask == null) "添加待办" else "修改待办",
                    color = Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = MutedInk)
                }
            }
            Text("提前 10 分钟发送提醒通知", color = MutedInk, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("待办内容") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("task-title")
            )
            Spacer(Modifier.height(10.dp))
            TaskDateTimeFields(
                date = taskDate,
                time = taskTime,
                onDateChange = { taskDate = it },
                onTimeChange = { taskTime = it }
            )
            Text("重复", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                listOf(
                    RepeatType.Once to "一次",
                    RepeatType.Daily to "每天",
                    RepeatType.Weekdays to "工作日",
                    RepeatType.Weekly to "每周",
                    RepeatType.Custom to "自定义"
            ).forEach { (type, label) ->
                    GlassFilterChip(
                        selected = repeatType == type,
                        onClick = { repeatType = type },
                        label = { Text(label, fontSize = 12.sp) },
                        accent = repeatColor(type)
                    )
                }
            }
            if (repeatType == RepeatType.Custom) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    listOf(
                        DayOfWeek.MONDAY to "一",
                        DayOfWeek.TUESDAY to "二",
                        DayOfWeek.WEDNESDAY to "三",
                        DayOfWeek.THURSDAY to "四",
                        DayOfWeek.FRIDAY to "五",
                        DayOfWeek.SATURDAY to "六",
                        DayOfWeek.SUNDAY to "日"
                    ).forEach { (day, label) ->
                        GlassFilterChip(
                            selected = day in repeatDays,
                            onClick = {
                                repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            accent = Blue
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("工作", "生活", "家庭").forEach { option ->
                    GlassFilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option, fontSize = 12.sp) },
                        accent = taskColor(option)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (title.isNotBlank() &&
                        (repeatType != RepeatType.Custom || repeatDays.isNotEmpty())
                    ) {
                        onSave(
                            (initialTask ?: CalendarTask(
                                id = System.currentTimeMillis(),
                                title = title.trim(),
                                time = taskTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                date = taskDate,
                                category = category,
                                color = taskColor(category)
                            )).copy(
                                title = title.trim(),
                                time = taskTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                date = taskDate,
                                category = category,
                                color = taskColor(category),
                                repeatType = repeatType,
                                repeatDays = if (repeatType == RepeatType.Custom) repeatDays else emptySet()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() &&
                    (repeatType != RepeatType.Custom || repeatDays.isNotEmpty()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存待办")
            }
        }
    }
}

@Composable
private fun MonthPickerDialog(
    month: YearMonth,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit
) {
    val options = (-2..3).map { month.plusMonths(it.toLong()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择月份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    TextButton(onClick = { onSelect(option) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${option.year}年${option.monthValue}月", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private fun buildCalendarDays(
    month: YearMonth,
    holidays: Map<LocalDate, HolidayInfo> = HolidayRepository.builtIn(month.year)
): List<CalendarDay> {
    val first = month.atDay(1)
    val mondayOffset = first.dayOfWeek.value - 1
    val totalCells = 42
    return (0 until totalCells).map { index ->
        val date = first.minusDays(mondayOffset.toLong()).plusDays(index.toLong())
        CalendarDay(
            date = date,
            lunar = lunarLabel(date, month),
            holiday = holidays[date],
            isCurrentMonth = date.month == month.month
        )
    }
}

fun CalendarTask.occursOn(date: LocalDate): Boolean {
    if (date.isBefore(this.date)) return false
    return when (repeatType) {
        RepeatType.Once -> date == this.date
        RepeatType.Daily -> true
        RepeatType.Weekdays -> date.dayOfWeek.value <= DayOfWeek.FRIDAY.value
        RepeatType.Weekly -> date.dayOfWeek == this.date.dayOfWeek
        RepeatType.Custom -> date.dayOfWeek in repeatDays
    }
}

private fun taskColor(category: String): Color {
    return when (category) {
        "工作" -> Blue
        "家庭" -> WarmYellow
        else -> Teal
    }
}

@Composable
private fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    accent: Color = Blue
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.23f) else Color.White.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) { label() }
    }
}

private fun repeatColor(type: RepeatType): Color = when (type) {
    RepeatType.Once -> Blue
    RepeatType.Daily -> Teal
    RepeatType.Weekdays -> WarmYellow
    RepeatType.Weekly -> Color(0xFF8C6BE8)
    RepeatType.Custom -> Coral
}

private fun lunarLabel(date: LocalDate, visibleMonth: YearMonth? = null): String {
    if (visibleMonth != null && YearMonth.from(date) != visibleMonth) return ""
    synchronized(lunarLabelCache) {
        lunarLabelCache[date]?.let { return it }
    }
    val calendar = ChineseCalendar()
    calendar.timeInMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val day = calendar.get(ChineseCalendar.DAY_OF_MONTH)
    val month = calendar.get(ChineseCalendar.MONTH) + 1
    val monthName = chineseLunarMonthName(month)
    val label = if (day == 1) "${monthName}月" else lunarDayName(day)
    synchronized(lunarLabelCache) {
        if (lunarLabelCache.size > 2048) lunarLabelCache.clear()
        lunarLabelCache[date] = label
    }
    return label
}

private fun chineseLunarMonthName(month: Int): String {
    return listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
        .getOrElse(month - 1) { month.toString() }
}

internal fun lunarDayName(day: Int): String {
    val ones = listOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    return when {
        day <= 10 -> "初${ones[day]}"
        day == 20 -> "二十"
        day == 30 -> "三十"
        day < 20 -> "十${ones[day % 10]}"
        else -> "廿${ones[day % 10]}"
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun CalendarPreview() {
    LiuguangCalendarTheme {
        CalendarApp()
    }
}
