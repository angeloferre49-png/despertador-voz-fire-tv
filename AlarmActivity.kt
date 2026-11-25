package com.example.firetvvoicealarm

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : Activity(), TextToSpeech.OnInitListener {
    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var stopped = false
    private var alarmId: String? = null
    private var message = "Es hora de levantarse"
    private lateinit var messageView: TextView
    private lateinit var timeView: TextView

    private val clock = object : Runnable {
        override fun run() {
            if (!stopped) {
                timeView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWakeScreen()
        alarmId = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID)
        message = alarmId?.let { AlarmStore.find(this, it)?.message } ?: message
        buildUi()
        tts = TextToSpeech(this, this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmId = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID)
        message = alarmId?.let { AlarmStore.find(this, it)?.message } ?: "Es hora de levantarse"
        messageView.text = message
        if (ready) speakAgain()
    }

    private fun configureWakeScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(48, 24, 48, 24); setBackgroundColor(0xFF7A1010.toInt())
        }
        timeView = TextView(this).apply {
            textSize = 76f; gravity = Gravity.CENTER; setTextColor(0xFFFFFFFF.toInt())
        }
        messageView = TextView(this).apply {
            text = message; textSize = 30f; gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 20, 0, 32)
        }
        val stop = Button(this).apply {
            text = "APAGAR ALARMA"; textSize = 24f; isFocusable = true
            setOnClickListener { stopAlarm() }
        }
        root.addView(timeView); root.addView(messageView); root.addView(stop)
        setContentView(root)
        stop.requestFocus()
        handler.post(clock)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var result = tts?.setLanguage(Locale("es", "ES")) ?: TextToSpeech.ERROR
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts?.setLanguage(Locale.getDefault()) ?: TextToSpeech.ERROR
            }
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED && result >= 0
            if (ready && !stopped) speakAgain()
        }
    }

    private fun speakAgain() {
        if (stopped || !ready) return
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onError(utteranceId: String?) { scheduleRepeat() }
            override fun onDone(utteranceId: String?) { handler.post { scheduleRepeat() } }
        })
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "alarm-utterance")
    }

    private fun scheduleRepeat() {
        if (!stopped) handler.postDelayed({ speakAgain() }, 7000L)
    }

    private fun stopAlarm() {
        if (stopped) return
        stopped = true
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        finish()
        // Deliberately do not cancel or modify any scheduled alarm here.
    }

    override fun onBackPressed() {
        // BACK must not silence or dismiss an active alarm.
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
