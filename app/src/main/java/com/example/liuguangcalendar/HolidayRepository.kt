package com.example.liuguangcalendar

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import android.icu.util.ChineseCalendar

internal data class HolidayInfo(
    val name: String? = null,
    val marker: String? = null,
    val isOfficial: Boolean = false
)

internal object HolidayRepository {
    private const val PREFS = "holiday_calendar"
    private const val CACHE_PREFIX = "year_"
    private const val SOURCE = "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/%d.json"

    private val builtIn2026 = mapOf(
        "2026-01-01" to "元旦|休", "2026-01-02" to "元旦|休", "2026-01-03" to "元旦|休", "2026-01-04" to "元旦|班",
        "2026-02-14" to "春节|班", "2026-02-15" to "春节|休", "2026-02-16" to "春节|休", "2026-02-17" to "春节|休",
        "2026-02-18" to "春节|休", "2026-02-19" to "春节|休", "2026-02-20" to "春节|休", "2026-02-21" to "春节|休",
        "2026-02-22" to "春节|休", "2026-02-23" to "春节|休", "2026-02-28" to "春节|班",
        "2026-04-04" to "清明节|休", "2026-04-05" to "清明节|休", "2026-04-06" to "清明节|休",
        "2026-05-01" to "劳动节|休", "2026-05-02" to "劳动节|休", "2026-05-03" to "劳动节|休", "2026-05-04" to "劳动节|休",
        "2026-05-05" to "劳动节|休", "2026-05-09" to "劳动节|班",
        "2026-06-19" to "端午节|休", "2026-06-20" to "端午节|休", "2026-06-21" to "端午节|休",
        "2026-09-20" to "国庆节|班", "2026-09-25" to "中秋节|休", "2026-09-26" to "中秋节|休", "2026-09-27" to "中秋节|休",
        "2026-10-01" to "国庆节|休", "2026-10-02" to "国庆节|休", "2026-10-03" to "国庆节|休", "2026-10-04" to "国庆节|休",
        "2026-10-05" to "国庆节|休", "2026-10-06" to "国庆节|休", "2026-10-07" to "国庆节|休", "2026-10-10" to "国庆节|班"
    ).toHolidayMap()
    private val builtInCache = mutableMapOf<Int, Map<LocalDate, HolidayInfo>>()

    suspend fun load(context: Context, year: Int): Map<LocalDate, HolidayInfo> = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cached = preferences.getString(CACHE_PREFIX + year, null)
            ?.let(::parse)
            ?.takeIf { it.isNotEmpty() }
        val fallback = builtIn(year)

        val downloaded = runCatching {
            val connection = URL(SOURCE.format(year)).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.inputStream.bufferedReader().use { parse(it.readText()) }
                .also { connection.disconnect() }
        }.getOrNull()?.takeIf { it.isNotEmpty() }

        if (downloaded != null) {
            preferences.edit().putString(CACHE_PREFIX + year, encode(downloaded)).apply()
            downloaded
        } else {
            cached ?: fallback
        }
    }

    fun loadCached(context: Context, year: Int): Map<LocalDate, HolidayInfo>? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CACHE_PREFIX + year, null)
            ?.let(::parse)
            ?.takeIf { it.isNotEmpty() }
    }

    private fun parse(text: String): Map<LocalDate, HolidayInfo> {
        val days = JSONObject(text).optJSONArray("days") ?: JSONArray()
        return buildMap {
            for (index in 0 until days.length()) {
                val item = days.optJSONObject(index) ?: continue
                val date = runCatching { LocalDate.parse(item.optString("date")) }.getOrNull() ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() }
                val marker = if (item.has("isOffDay")) {
                    if (item.optBoolean("isOffDay")) "休" else "班"
                } else {
                    item.optString("marker", "班")
                }
                put(date, HolidayInfo(name, marker, isOfficial = true))
            }
        }
    }

    private fun encode(data: Map<LocalDate, HolidayInfo>): String = JSONObject().apply {
        put("days", JSONArray().apply {
            data.entries.sortedBy { it.key }.forEach { (date, info) ->
                put(JSONObject().apply {
                    put("date", date.toString())
                    put("name", info.name ?: "")
                    put("marker", info.marker)
                })
            }
        })
    }.toString()

    fun builtIn(year: Int): Map<LocalDate, HolidayInfo> {
        synchronized(builtInCache) {
            builtInCache[year]?.let { return it }
            return if (year == 2026) {
                builtIn2026.also { builtInCache[year] = it }
            } else {
                fallbackFestivals(year).also { builtInCache[year] = it }
            }
        }
    }

    private fun fallbackFestivals(year: Int): Map<LocalDate, HolidayInfo> {
        val result = mutableMapOf<LocalDate, HolidayInfo>()
        val chinese = ChineseCalendar()
        for (month in 1..12) for (day in 1..31) {
            val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: continue
            chinese.timeInMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val lunarMonth = chinese.get(ChineseCalendar.MONTH) + 1
            val lunarDay = chinese.get(ChineseCalendar.DAY_OF_MONTH)
            val festival = when {
                lunarMonth == 1 && lunarDay == 1 -> "春节"
                lunarMonth == 5 && lunarDay == 5 -> "端午节"
                lunarMonth == 8 && lunarDay == 15 -> "中秋节"
                month == 1 && day == 1 -> "元旦"
                month == 4 && day in 4..6 -> "清明节"
                month == 5 && day == 1 -> "劳动节"
                month == 10 && day == 1 -> "国庆节"
                else -> null
            }
            if (festival != null) result[date] = HolidayInfo(festival)
        }
        return result
    }

    private fun Map<String, String>.toHolidayMap(): Map<LocalDate, HolidayInfo> = buildMap {
        this@toHolidayMap.forEach { (date, value) ->
            val parts = value.split('|', limit = 2)
            put(LocalDate.parse(date), HolidayInfo(parts[0], parts.getOrElse(1) { "休" }))
        }
    }
}
