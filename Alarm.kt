package com.example.firetvvoicealarm

import org.json.JSONObject
import java.util.UUID

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    var hour: Int,
    var minute: Int,
    var message: String,
    var isEnabled: Boolean = true,
    var repeatDaily: Boolean = true
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("hour", hour); put("minute", minute)
        put("message", message); put("isEnabled", isEnabled); put("repeatDaily", repeatDaily)
    }

    companion object {
        fun fromJson(o: JSONObject) = Alarm(
            id = o.optString("id", UUID.randomUUID().toString()),
            hour = o.optInt("hour", 7).coerceIn(0, 23),
            minute = o.optInt("minute", 0).coerceIn(0, 59),
            message = o.optString("message", "Es hora de levantarse"),
            isEnabled = o.optBoolean("isEnabled", true),
            repeatDaily = o.optBoolean("repeatDaily", true)
        )
    }
}
