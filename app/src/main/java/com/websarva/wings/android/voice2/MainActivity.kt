package com.websarva.wings.android.voice2

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var _animation1 : Animation
    private lateinit var _animation2 : Animation
    private lateinit var _animation3 : Animation
    private lateinit var _animator : Animator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myTextView = findViewById<TypewriterTextView>(R.id.tvWord)
        val text = getString(R.string.tv_app_title)
        myTextView.animateText(text)

        val button1 = findViewById<Button>(R.id.btRecord)
        val button2 = findViewById<Button>(R.id.btMake)
        val button3 = findViewById<Button>(R.id.btCheck)
        _animation1 = AnimationUtils.loadAnimation(this, R.anim.float_anim)
        _animation2 = AnimationUtils.loadAnimation(this, R.anim.float_anim2)
        _animation3 = AnimationUtils.loadAnimation(this, R.anim.float_anim3)
        button1.startAnimation(_animation3)
        button2.startAnimation(_animation2)
        button3.startAnimation(_animation3)
    }

    fun onButtonClick2RecodeVoiceSelect(view: View) {
        val intent = Intent(this@MainActivity, RecodeVoiceSelect::class.java)
        startActivity(intent)
    }

    fun onButtonClick2MakeVoiceSelect(view: View) {
        val intent = Intent(this@MainActivity, MakeVoiceSelect::class.java)
        startActivity(intent)
    }

    fun onButtonClick2CheckMyReport(view: View) {
        val intent = Intent(this@MainActivity, CheckReport::class.java)
        startActivity(intent)
    }
}