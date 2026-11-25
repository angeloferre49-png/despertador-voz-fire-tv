package com.example.firetvvoicealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val alarm = AlarmStore.find(context, id) ?: return
        if (!alarm.isEnabled) return

        // Schedule this alarm's next daily occurrence before opening the alarm UI.
        AlarmScheduler.scheduleNextOccurrence(context, alarm)

        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val lock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "FireTvVoiceAlarm:AlarmWakeLock"
            )
            lock.acquire(8 * 60 * 1000L)
        } catch (_: Exception) { }

        val activity = Intent(context, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try { context.startActivity(activity) } catch (_: Exception) { }
    }

    companion object { const val EXTRA_ALARM_ID = "alarm_id" }
}
