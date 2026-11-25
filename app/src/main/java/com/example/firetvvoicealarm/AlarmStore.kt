package com.example.firetvvoicealarm

import android.content.Context
import org.json.JSONArray

object AlarmStore {
    private const val PREFS = "voice_alarm_store"
    private const val KEY = "alarms"

    @Synchronized
    fun getAll(context: Context): MutableList<Alarm> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { i -> Alarm.fromJson(array.getJSONObject(i)) }
        } catch (_: Exception) { mutableListOf() }
    }

    @Synchronized
    fun saveAll(context: Context, alarms: List<Alarm>) {
        val array = JSONArray()
        alarms.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun find(context: Context, id: String): Alarm? = getAll(context).firstOrNull { it.id == id }

    fun upsert(context: Context, alarm: Alarm) {
        val all = getAll(context)
        val index = all.indexOfFirst { it.id == alarm.id }
        if (index >= 0) all[index] = alarm else all.add(alarm)
        saveAll(context, all)
    }

    fun remove(context: Context, id: String) {
        saveAll(context, getAll(context).filterNot { it.id == id })
    }
}
