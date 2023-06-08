package com.websarva.wings.android.voice2

import android.Manifest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.random.Random

private const val REQUEST_EXTERNAL_STORAGE_PERMISSION = 300

class RecodeMake : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var _file : File
    private var _player: MediaPlayer? = null
    private var _fileName: String = ""
    private lateinit var _seekbar: SeekBar
    private var _documentId : String = ""
    private lateinit var _radiobutton: RadioButton

    private var permissionToExternalStorageAccepted = false
    private var storagepermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            REQUEST_EXTERNAL_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    permissionToExternalStorageAccepted = true
                } else {
                    requestExternalStoragePermission()
                }
            }
            else -> {
            }
        }
    }

    private fun requestExternalStoragePermission() {
        if (!permissionToExternalStorageAccepted) {
            Toast.makeText(this, "ストレージの許可が必要です", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                storagepermissions,
                REQUEST_EXTERNAL_STORAGE_PERMISSION
            )
        }
    }


    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    }
    else {
        stopPlaying()
    }

    private fun startPlaying() {
        _player = MediaPlayer().apply {
            try {
                val fileDescriptor = FileInputStream(_file).fd
                setDataSource(fileDescriptor)
                prepare()
                start()
            } catch (_: IOException) {
            }
        }
        val audioDuration = _player?.duration
        if (audioDuration != null) {
            _seekbar.max = audioDuration
        }

        val updateSeekBar = object : Runnable {
            override fun run() {
                _seekbar.progress = _player?.currentPosition ?: 0
                if (_seekbar.progress >= (audioDuration ?: 0)) {
                    _seekbar.progress = 0
                } else {
                    Handler().postDelayed(this, 1000)
                }
            }
        }
        Handler().postDelayed(updateSeekBar, 1000)
    }

    private fun stopPlaying() {
        _player?.release()
        _player = null
        Handler().removeCallbacksAndMessages(null)
    }

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                // ユーザーがシークバーを操作した場合は、再生位置を変更する
                _player?.seekTo(progress)
            }
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            // シークバーの操作を開始したときは特に何も行わない
        }
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            // シークバーの操作を終了したときは特に何も行わない
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recode_make)

        ActivityCompat.requestPermissions(this@RecodeMake, storagepermissions,
            REQUEST_EXTERNAL_STORAGE_PERMISSION)


        val listener = MakeButton()
        findViewById<Button>(R.id.btMakeVoice).setOnClickListener(listener)
        findViewById<Button>(R.id.btPlayMadeVoice).setOnClickListener(listener)
        findViewById<Button>(R.id.btSave).setOnClickListener(listener)

        _seekbar = findViewById(R.id.sbMakePlay)
        _seekbar.setOnSeekBarChangeListener(seekBarChangeListener)

    }

    fun downloadAudioFile(sex: String, generation: String, title: String, ) {
        val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        db.collection("report")
            .whereEqualTo("sex", sex)
            .whereEqualTo("generation", generation)
            .whereEqualTo("title", title)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                val totalDocuments = documents.size

                if (totalDocuments > 0) {
                    val randomIndex = Random.nextInt(totalDocuments)
                    val document = documents[randomIndex]
                    _documentId = document.id
                    val filePath = document.getString("url")

                    if (filePath != null) {
                        val storageRef = FirebaseStorage.getInstance().reference.child(filePath)
                        _fileName = "file_0"
                        this._file = File(downloadDir, _fileName)

                        storageRef.getFile(_file)
                            .addOnSuccessListener {
                                val myTextView = findViewById<TypewriterTextView>(R.id.tvCheck)
                                val text = _documentId
                                myTextView.animateText(text)
                            }
                            .addOnFailureListener(OnFailureListener { e ->
                                val text = findViewById<TextView>(R.id.tvCheck)
                                text.setText(R.string.tv_make_ng)
                            })
                    }
                }
            }
            .addOnFailureListener (OnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error", e)
            })
    }

    fun appearAssess() {
        val radioButtonIds = listOf(R.id.rbVoice, R.id.rbContent, R.id.rbSympathy, R.id.rbPower)

        var delay = 1000L
        for (element in radioButtonIds) {
            val radioButton = findViewById<RadioButton>(element)
            radioButton.visibility = View.VISIBLE
            val animation = AnimationUtils.loadAnimation(this, R.anim.slide)
            animation.startOffset = delay
            radioButton.startAnimation(animation)
            delay += 1000L // アニメーションの遅延時間（ミリ秒）を調整する
        }
    }




    fun assessReport() {
        var _voice = ""
        var _content = ""
        var _sympathy = ""
        var _power = ""
        var newData: HashMap<String, Any> = hashMapOf()

        db.collection("report")
            .document(_documentId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val voice = documentSnapshot.getString("assess_voice")
                    val radiobutton1 = findViewById<RadioButton>(R.id.rbVoice)
                    if (radiobutton1.isChecked) {
                        if (voice != null) {
                            _voice = plusone(voice)
                            Log.d(TAG, _voice)
                        }
                    }
                    val content = documentSnapshot.getString("assess_content")
                    val radiobutton2 = findViewById<RadioButton>(R.id.rbContent)
                    if (radiobutton2.isChecked) {
                        if (content != null) {
                            _content = plusone(content)
                            Log.d(TAG, _voice)
                        }
                    }
                    val sympathy = documentSnapshot.getString("assess_sympathy")
                    val radiobutton3 = findViewById<RadioButton>(R.id.rbSympathy)
                    if (radiobutton3.isChecked) {
                        if (sympathy != null) {
                            _sympathy = plusone(sympathy)
                            Log.d(TAG, _voice)
                        }
                    }
                    val power = documentSnapshot.getString("assess_power")
                    val radiobutton4 = findViewById<RadioButton>(R.id.rbPower)
                    if (radiobutton4.isChecked) {
                        if (power != null) {
                            _power = plusone(power)
                            Log.d(TAG, _voice)
                        }
                    }
                    newData = hashMapOf(
                        "assess_voice" to _voice, "assess_content" to _content,
                        "assess_sympathy" to _sympathy, "assess_power" to _power
                    )
                    db.collection("report")
                        .document(_documentId)
                        .update(newData)
                        .addOnSuccessListener {
                            findViewById<TextView>(R.id.tvCheck).setText(R.string.tv_check_length)
                        }
                        .addOnFailureListener {
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting document: ", exception)
            }

    }

    private fun plusone(number: String): String {
        val newnumber = number.toInt() + 1
        return newnumber.toString()
    }


    private inner class MakeButton : View.OnClickListener {
        override fun onClick(v: View?) {
            if(v !=null) {
                when(v.id) {
                    R.id.btMakeVoice -> {
                        val sex = intent.getStringExtra("sex")?: ""
                        val generation = intent.getStringExtra("generation")?: ""
                        val title = intent.getStringExtra("title")?: ""
                        downloadAudioFile(sex,generation,title)
                        findViewById<Button>(R.id.btPlayMadeVoice).isEnabled = true
                    }
                    R.id.btPlayMadeVoice -> {
                        onPlay(true)
                        val myTextView = findViewById<TypewriterTextView>(R.id.tvAssess)
                        val text = getString(R.string.rb_assess)
                        myTextView.animateText(text)
                        appearAssess()
                        findViewById<Button>(R.id.btSave).isEnabled = true
                    }
                    R.id.btSave -> {
                        assessReport()
                    }
                }
            }
        }
    }
}