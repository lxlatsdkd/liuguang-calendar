package com.example.liuguangcalendar

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderScheduler {
    internal const val ACTION_REMINDER = "com.example.liuguangcalendar.TASK_REMINDER"
    internal const val ACTION_TASK_UPDATED = "com.example.liuguangcalendar.TASK_UPDATED"
    private const val ACTION_TASK_ACTION = "com.example.liuguangcalendar.TASK_ACTION"
    private const val CHANNEL_ID = "calendar_reminders"
    internal const val EXTRA_TASK_ID = "task_id"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_TIME = "time"
    internal const val EXTRA_DATE = "date"
    private const val EXTRA_START_DATE = "start_date"
    private const val EXTRA_REPEAT = "repeat"
    private const val EXTRA_DAYS = "days"
    internal const val EXTRA_COMPLETED = "completed"
    private const val EXTRA_ACTION = "action"
    private const val EXTRA_SNOOZED = "snoozed"
    private const val EXTRA_AT_TIME = "at_time"
    private const val ACTION_COMPLETE = "complete"
    private const val ACTION_SNOOZE = "snooze"
    private const val STATE_PREFS = "task_completion_state"
    private const val STATE_COMPLETED = "completed"

    fun schedule(context: Context, task: CalendarTask) {
        cancel(context, task.id)
        val date = nextOccurrence(task, LocalDate.now(), System.currentTimeMillis()) ?: return
        scheduleOccurrence(context, task, date)
    }

    internal fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    fun cancel(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        listOf(0, 1, 4).forEach { variant ->
            val pendingIntent = pendingIntent(context, taskId, null, variant)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleOccurrence(context: Context, task: CalendarTask, date: LocalDate) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val scheduledTime = date
            .atTime(parseTime(task.time))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val now = System.currentTimeMillis()
        if (scheduledTime <= now) return

        val baseIntent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_TASK_ID, task.id)
            .putExtra(EXTRA_TITLE, task.title)
            .putExtra(EXTRA_TIME, task.time)
            .putExtra(EXTRA_DATE, date.toString())
            .putExtra(EXTRA_START_DATE, task.date.toString())
            .putExtra(EXTRA_REPEAT, task.repeatType.name)
            .putExtra(EXTRA_DAYS, task.repeatDays.joinToString(",") { it.value.toString() })

        val earlyAt = scheduledTime - 10 * 60 * 1000L
        if (earlyAt > now) {
            val earlyIntent = Intent(baseIntent).putExtra(EXTRA_AT_TIME, false)
            setAlarm(alarmManager, earlyAt, pendingIntent(context, task.id, earlyIntent, variant = 0))
        }

        val exactIntent = Intent(baseIntent).putExtra(EXTRA_AT_TIME, true)
        setAlarm(alarmManager, scheduledTime, pendingIntent(context, task.id, exactIntent, variant = 4))
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAt: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    internal fun scheduleSnooze(context: Context, task: CalendarTask, occurrenceDate: LocalDate) {
        val triggerAt = System.currentTimeMillis() + 10 * 60 * 1000L
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_TASK_ID, task.id)
            .putExtra(EXTRA_TITLE, task.title)
            .putExtra(EXTRA_TIME, task.time)
            .putExtra(EXTRA_DATE, occurrenceDate.toString())
            .putExtra(EXTRA_START_DATE, task.date.toString())
            .putExtra(EXTRA_REPEAT, task.repeatType.name)
            .putExtra(EXTRA_DAYS, task.repeatDays.joinToString(",") { it.value.toString() })
            .putExtra(EXTRA_SNOOZED, true)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        setAlarm(alarmManager, triggerAt, pendingIntent(context, task.id, intent, variant = 1))
    }

    private fun pendingIntent(
        context: Context,
        taskId: Long,
        sourceIntent: Intent?,
        variant: Int
    ): PendingIntent {
        val intent = sourceIntent ?: Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMINDER)
        intent.data = android.net.Uri.parse("calendar://reminder/$taskId/$variant")
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId, variant),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(taskId: Long, variant: Int): Int {
        return ((taskId xor (taskId ushr 32)).toInt() * 10) + variant
    }

    internal fun notificationId(taskId: Long, atTime: Boolean, snoozed: Boolean): Int {
        val base = (taskId xor (taskId ushr 32)).toInt() * 10
        return base + when {
            snoozed -> 1
            atTime -> 4
            else -> 0
        }
    }

    internal fun cancelNotifications(context: Context, taskId: Long) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(notificationId(taskId, atTime = false, snoozed = false))
        manager.cancel(notificationId(taskId, atTime = true, snoozed = false))
        manager.cancel(notificationId(taskId, atTime = false, snoozed = true))
    }

    private fun nextOccurrence(task: CalendarTask, from: LocalDate, nowMillis: Long): LocalDate? {
        var date = if (task.date.isAfter(from)) task.date else from
        repeat(370) {
            if (task.occursOn(date)) {
                val triggerAt = date
                    .atTime(parseTime(task.time))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                if (triggerAt > nowMillis) return date
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun parseTime(value: String): LocalTime {
        return runCatching {
            LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm"))
        }.getOrDefault(LocalTime.of(9, 0))
    }

    internal fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "日历提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "待办事项提前十分钟提醒"
            }
        )
    }

    internal fun notificationChannelId(): String = CHANNEL_ID

    internal fun actionPendingIntent(
        context: Context,
        task: CalendarTask,
        occurrenceDate: LocalDate,
        action: String
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ACTION_TASK_ACTION)
            .putExtra(EXTRA_ACTION, action)
            .putExtra(EXTRA_TASK_ID, task.id)
            .putExtra(EXTRA_TITLE, task.title)
            .putExtra(EXTRA_TIME, task.time)
            .putExtra(EXTRA_DATE, occurrenceDate.toString())
            .putExtra(EXTRA_START_DATE, task.date.toString())
            .putExtra(EXTRA_REPEAT, task.repeatType.name)
            .putExtra(EXTRA_DAYS, task.repeatDays.joinToString(",") { it.value.toString() })
        return PendingIntent.getBroadcast(
            context,
            requestCode(task.id, if (action == ACTION_COMPLETE) 2 else 3),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun setCompleted(context: Context, taskId: Long, date: LocalDate, completed: Boolean) {
        val key = "$taskId|$date"
        val preferences = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        val completedDates = preferences.getStringSet(STATE_COMPLETED, emptySet()).orEmpty().toMutableSet()
        if (completed) completedDates += key else completedDates -= key
        preferences.edit().putStringSet(STATE_COMPLETED, completedDates).apply()
    }

    internal fun isCompleted(context: Context, taskId: Long, date: LocalDate): Boolean {
        return context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .getStringSet(STATE_COMPLETED, emptySet())
            .orEmpty()
            .contains("$taskId|$date")
    }

    internal fun nextTaskFromIntent(intent: Intent): CalendarTask? {
        val id = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return null
        val time = intent.getStringExtra(EXTRA_TIME) ?: return null
        val date = intent.getStringExtra(EXTRA_START_DATE)?.let { LocalDate.parse(it) }
            ?: intent.getStringExtra(EXTRA_DATE)?.let { LocalDate.parse(it) }
            ?: return null
        val repeat = intent.getStringExtra(EXTRA_REPEAT)?.let { RepeatType.valueOf(it) } ?: RepeatType.Once
        val days = intent.getStringExtra(EXTRA_DAYS)
            .orEmpty()
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .mapNotNull { value -> java.time.DayOfWeek.entries.firstOrNull { it.value == value } }
            .toSet()
        return CalendarTask(
            id = id,
            title = title,
            time = time,
            date = date,
            category = "提醒",
            color = androidx.compose.ui.graphics.Color(0xFF2A8CFF),
            repeatType = repeat,
            repeatDays = days
        )
    }

    internal fun occurrenceDateFromIntent(intent: Intent): LocalDate? {
        return intent.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.liuguangcalendar.TASK_REMINDER") return
        ReminderScheduler.createChannel(context)
        val task = ReminderScheduler.nextTaskFromIntent(intent) ?: return
        val snoozed = intent.getBooleanExtra("snoozed", false)
        val atTime = intent.getBooleanExtra("at_time", false)
        val openIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.notificationChannelId())
            .setSmallIcon(R.drawable.ic_launcher_day_01)
            .setContentTitle(task.title)
            .setContentText(
                if (snoozed) "已延后 10 分钟，${task.time} 的待办提醒"
                else if (atTime) "${task.time} 的待办现在开始"
                else "${task.time} 的待办将在 10 分钟后开始"
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_day_01,
                    "已完成",
                    ReminderScheduler.actionPendingIntent(
                        context,
                        task,
                        ReminderScheduler.occurrenceDateFromIntent(intent) ?: task.date,
                        "complete"
                    )
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_day_01,
                    "延后10分钟",
                    ReminderScheduler.actionPendingIntent(
                        context,
                        task,
                        ReminderScheduler.occurrenceDateFromIntent(intent) ?: task.date,
                        "snooze"
                    )
                ).build()
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(
                ReminderScheduler.notificationId(task.id, atTime, snoozed),
                notification
            )
        } catch (_: SecurityException) {
            // Android 13+ requires the user to grant POST_NOTIFICATIONS.
        }
        if (!snoozed && atTime && task.repeatType != RepeatType.Once) {
            ReminderScheduler.schedule(context, task)
        }
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.liuguangcalendar.TASK_ACTION") return
        val task = ReminderScheduler.nextTaskFromIntent(intent) ?: return
        val occurrenceDate = ReminderScheduler.occurrenceDateFromIntent(intent) ?: task.date
        val action = intent.getStringExtra("action") ?: return
        ReminderScheduler.cancelNotifications(context, task.id)
        when (action) {
            "complete" -> {
                ReminderScheduler.cancel(context, task.id)
                ReminderScheduler.setCompleted(context, task.id, occurrenceDate, true)
                context.sendBroadcast(
                    Intent(ReminderScheduler.ACTION_TASK_UPDATED)
                        .setPackage(context.packageName)
                        .putExtra(ReminderScheduler.EXTRA_TASK_ID, task.id)
                        .putExtra(ReminderScheduler.EXTRA_COMPLETED, true)
                )
            }
            "snooze" -> ReminderScheduler.scheduleSnooze(context, task, occurrenceDate)
        }
    }
}
