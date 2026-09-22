package com.example.liuguangcalendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.time.LocalDate
import java.time.ZoneId

object LauncherIconUpdater {
    private const val UPDATE_ACTION = "com.example.liuguangcalendar.UPDATE_ICON"
    private const val REQUEST_CODE = 71021

    fun update(context: Context) {
        val packageManager = context.packageManager
        val currentDay = LocalDate.now().dayOfMonth
        for (day in 1..31) {
            val component = ComponentName(
                context,
                "${context.packageName}.Day${day.toString().padStart(2, '0')}Alias"
            )
            packageManager.setComponentEnabledSetting(
                component,
                if (day == currentDay) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun schedule(context: Context) {
        update(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, LauncherIconUpdateReceiver::class.java).setAction(UPDATE_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextMidnight = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextMidnight,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}

class LauncherIconUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        LauncherIconUpdater.schedule(context)
    }
}
