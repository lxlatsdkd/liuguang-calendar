package com.example.liuguangcalendar

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

internal object TaskRepository {
    private const val PREFS = "calendar_tasks"
    private const val TASKS_KEY = "tasks"

    fun load(context: Context): List<CalendarTask> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TASKS_KEY, null)
            ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val task = runCatching { decode(item) }.getOrNull() ?: continue
                add(task)
            }
        }
    }

    fun save(context: Context, tasks: Collection<CalendarTask>) {
        val array = JSONArray()
        tasks.distinctBy { it.id }.forEach { array.put(encode(it)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TASKS_KEY, array.toString())
            .apply()
    }

    private fun encode(task: CalendarTask): JSONObject = JSONObject().apply {
        put("id", task.id)
        put("title", task.title)
        put("time", task.time)
        put("date", task.date.toString())
        put("category", task.category)
        put("completed", task.completed)
        put("repeatType", task.repeatType.name)
        put("repeatDays", JSONArray().apply {
            task.repeatDays.forEach { put(it.value) }
        })
    }

    private fun decode(item: JSONObject): CalendarTask {
        val category = item.optString("category", "工作")
        val repeatType = runCatching {
            RepeatType.valueOf(item.optString("repeatType", RepeatType.Once.name))
        }.getOrDefault(RepeatType.Once)
        val repeatDays = buildSet {
            val values = item.optJSONArray("repeatDays") ?: JSONArray()
            for (index in 0 until values.length()) {
                val value = values.optInt(index, -1)
                DayOfWeek.entries.firstOrNull { it.value == value }?.let(::add)
            }
        }
        return CalendarTask(
            id = item.getLong("id"),
            title = item.getString("title"),
            time = item.getString("time"),
            date = LocalDate.parse(item.getString("date")),
            category = category,
            color = persistedTaskColor(category),
            completed = item.optBoolean("completed", false),
            repeatType = repeatType,
            repeatDays = repeatDays
        )
    }

    private fun persistedTaskColor(category: String): Color = when (category) {
        "工作" -> Color(0xFF2A8CFF)
        "家庭" -> Color(0xFFFFB51B)
        else -> Color(0xFF13B9B3)
    }
}
