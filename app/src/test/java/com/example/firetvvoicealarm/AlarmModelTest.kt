package com.example.firetvvoicealarm

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmModelTest {
    @Test fun alarmRoundTripsJson() {
        val original = Alarm(id = "alarm-1", hour = 18, minute = 32,
            message = "Es hora de cenar", isEnabled = true, repeatDaily = false)
        val restored = Alarm.fromJson(original.toJson())
        assertEquals(original.id, restored.id)
        assertEquals(original.hour, restored.hour)
        assertEquals(original.minute, restored.minute)
        assertEquals(original.message, restored.message)
        assertEquals(original.isEnabled, restored.isEnabled)
        assertEquals(original.repeatDaily, restored.repeatDaily)
    }

    @Test fun invalidTimeIsClamped() {
        val alarm = Alarm.fromJson(JSONObject().put("hour", 30).put("minute", -4))
        assertEquals(23, alarm.hour)
        assertEquals(0, alarm.minute)
    }
}
