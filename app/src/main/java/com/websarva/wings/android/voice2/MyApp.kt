package com.websarva.wings.android.voice2

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class TypewriterTextView : AppCompatTextView {
    private var typewriterText: CharSequence = ""
    private var index: Int = 0
    private var canType: Boolean = true
    private var delay: Long = 50
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private val typewriter: Runnable = object : Runnable {
        override fun run() {
            text = typewriterText.subSequence(0, index++)
            if (index <= typewriterText.length && canType) {
                mainHandler.postDelayed(this, delay)
            }
        }
    }

    fun setDelay(millis: Long) {
        delay = millis
    }

    fun animateText(text: String) {
        typewriterText = text
        index = 0
        setText("")
        mainHandler.removeCallbacks(typewriter)
        mainHandler.postDelayed(typewriter, delay)
    }

    fun stopAnimation() {
        mainHandler.removeCallbacks(typewriter)
    }

    fun pauseAnimation() {
        canType = false
    }

    fun restartAnimation() {
        canType = true
        mainHandler.postDelayed(typewriter, delay)
    }

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}
}

class MyApp : Application() {
    // プリファレンスファイルの名前
    private val prefsName = "MyPrefs"

    // SharedPreferences インスタンス
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()

        // SharedPreferences インスタンスを取得
        sharedPreferences = applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
}



