package com.example.firetvvoicealarm

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var list: LinearLayout
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tts = TextToSpeech(this, this)
        buildUi()
        requestExactAlarmPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) {
            AlarmScheduler.rescheduleAll(this)
            refreshList()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(java.util.Locale("es", "ES"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                val fallback = tts?.setLanguage(java.util.Locale.getDefault()) ?: TextToSpeech.ERROR
                ttsReady = fallback != TextToSpeech.LANG_MISSING_DATA && fallback != TextToSpeech.LANG_NOT_SUPPORTED && fallback >= 0
            }
        }
    }

    private fun buildUi() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(36, 24, 36, 24)
            setBackgroundColor(0xFF101820.toInt())
        }
        outer.addView(TextView(this).apply {
            text = "DESPERTADOR POR VOZ"; textSize = 28f; setTextColor(0xFFFFFFFF.toInt())
        })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(actionButton("NUEVA ALARMA") { editAlarm(null) })
        buttons.addView(actionButton("PROBAR VOZ") { speakTest() })
        buttons.addView(actionButton("RECARGAR") { refreshList() })
        outer.addView(buttons)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { isFocusable = true; addView(list) }
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(outer)
        refreshList()
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 16f; isFocusable = true; setOnClickListener { action() }
    }

    private fun refreshList() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val alarms = AlarmStore.getAll(this).sortedWith(compareBy<Alarm> { it.hour }.thenBy { it.minute })
        if (alarms.isEmpty()) list.addView(TextView(this).apply {
            text = "No hay alarmas. Seleccioná NUEVA ALARMA para crear una."
            textSize = 20f; setTextColor(0xFFFFFFFF.toInt()); setPadding(8, 24, 8, 24)
        })
        alarms.forEach { alarm ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(4, 8, 4, 8)
            }
            val info = TextView(this).apply {
                text = "%02d:%02d  •  %s  •  %s".format(alarm.hour, alarm.minute,
                    if (alarm.isEnabled) "ACTIVA" else "INACTIVA", alarm.message)
                textSize = 18f; setTextColor(0xFFFFFFFF.toInt()); isFocusable = false
            }
            row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(actionButton("EDITAR") { editAlarm(alarm) })
            row.addView(actionButton(if (alarm.isEnabled) "DESACTIVAR" else "ACTIVAR") {
                alarm.isEnabled = !alarm.isEnabled
                AlarmStore.upsert(this, alarm)
                if (alarm.isEnabled) {
                    if (!AlarmScheduler.schedule(this, alarm)) showExactAlarmSettings()
                } else AlarmScheduler.cancel(this, alarm)
                refreshList()
            })
            row.addView(actionButton("ELIMINAR") {
                AlertDialog.Builder(this).setTitle("Eliminar alarma")
                    .setMessage("¿Eliminar %02d:%02d?".format(alarm.hour, alarm.minute))
                    .setNegativeButton("CANCELAR", null)
                    .setPositiveButton("ELIMINAR") { _, _ ->
                        AlarmScheduler.cancel(this, alarm); AlarmStore.remove(this, alarm.id); refreshList()
                    }.show()
            })
            list.addView(row)
            list.addView(View(this).apply { setBackgroundColor(0xFF405060.toInt()) },
                LinearLayout.LayoutParams(-1, 1))
        }
    }

    private fun editAlarm(existing: Alarm?) {
        val alarm = existing?.copy() ?: Alarm(hour = 7, minute = 0, message = "Es hora de levantarse")
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 12, 24, 12) }
        val timeButton = Button(this)
        fun updateTime() { timeButton.text = "HORA: %02d:%02d".format(alarm.hour, alarm.minute) }
        timeButton.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> alarm.hour = h; alarm.minute = m; updateTime() },
                alarm.hour, alarm.minute, true).show()
        }
        updateTime(); panel.addView(timeButton)
        val message = EditText(this).apply {
            setText(alarm.message); hint = "Mensaje que dirá el televisor"; textSize = 18f
            isSingleLine = false; minLines = 2
        }
        panel.addView(message)
        val enabled = CheckBox(this).apply { text = "Alarma activa"; isChecked = alarm.isEnabled }
        panel.addView(enabled)
        val repeat = CheckBox(this).apply { text = "Repetir todos los días"; isChecked = alarm.repeatDaily }
        panel.addView(repeat)
        AlertDialog.Builder(this).setTitle(if (existing == null) "Nueva alarma" else "Editar alarma")
            .setView(panel).setNegativeButton("CANCELAR", null)
            .setPositiveButton("GUARDAR") { _, _ ->
                alarm.message = message.text.toString().trim().ifBlank { "Es hora de levantarse" }
                alarm.isEnabled = enabled.isChecked; alarm.repeatDaily = repeat.isChecked
                AlarmStore.upsert(this, alarm)
                if (alarm.isEnabled) {
                    if (!AlarmScheduler.schedule(this, alarm)) showExactAlarmSettings()
                } else AlarmScheduler.cancel(this, alarm)
                refreshList()
            }.show()
    }

    private fun speakTest() {
        if (ttsReady) tts?.speak("Prueba de voz. El despertador funciona.", TextToSpeech.QUEUE_FLUSH, null, "test")
        else Toast.makeText(this, "El motor de voz no está listo o no tiene español instalado.", Toast.LENGTH_LONG).show()
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!manager.canScheduleExactAlarms()) showExactAlarmSettings()
        }
    }

    private fun showExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")))
            } catch (_: Exception) {
                Toast.makeText(this, "Este Fire OS no expone el permiso de alarmas exactas. La programación exacta puede no estar disponible.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
