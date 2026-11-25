package com.example.firetvvoicealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {
    private fun pendingIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.firetvvoicealarm.FIRE_ALARM"
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        return PendingIntent.getBroadcast(
            context, alarm.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, alarm: Alarm): Boolean {
        if (!alarm.isEnabled) { cancel(context, alarm); return true }
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) return false
        val whenNext = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        return try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenNext.timeInMillis, pendingIntent(context, alarm))
            true
        } catch (_: SecurityException) { false }
    }

    fun scheduleNextOccurrence(context: Context, alarm: Alarm) {
        if (alarm.repeatDaily && alarm.isEnabled) schedule(context, alarm)
        else {
            alarm.isEnabled = false
            AlarmStore.upsert(context, alarm)
            cancel(context, alarm)
        }
    }

    fun cancel(context: Context, alarm: Alarm) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context, alarm))
    }

    fun rescheduleAll(context: Context) {
        AlarmStore.getAll(context).filter { it.isEnabled }.forEach { schedule(context, it) }
    }
}
