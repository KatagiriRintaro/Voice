package com.websarva.wings.android.voice2

import android.Manifest
import android.content.ContentValues.TAG
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.random.Random



private const val LOG_TAG = "AudioRecode"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_EXTERNAL_STORAGE_PERMISSION = 100


class RecodeVoice : AppCompatActivity() {
    private var _recorder: MediaRecorder? = null
    private var _fileName: String = ""
    private var _duration: Int = 0
    private var _player: MediaPlayer? = null
    private lateinit var _seekbar: SeekBar

    private var permissionToRecodeAccepted = false
    private var permissionToExternalStorageAccepted = false
    private var audiopermissions: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO)
    private var storagepermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val db = FirebaseFirestore.getInstance()
    private var audiodata = HashMap<String, Any>()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    permissionToRecodeAccepted = true
                    requestExternalStoragePermission()
                } else {
                    Toast.makeText(this, "録音の許可が必要です", Toast.LENGTH_LONG).show()
                }
            }

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

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            audiopermissions,
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }

    private fun requestExternalStoragePermission() {
        if (!permissionToRecodeAccepted) {
            requestRecordAudioPermission()
        } else {
            Toast.makeText(this, "ストレージの許可が必要です", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this,
                storagepermissions,
                REQUEST_EXTERNAL_STORAGE_PERMISSION
            )
        }
    }

    private fun onRecord(start: Boolean) = if (start) {
        startRecording()
    }
    else{
        stopRecording()
    }


    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    }
    else {
        stopPlaying()
    }

    private fun startPlaying() {
        val seekBar = findViewById<SeekBar>(R.id.sbRecordPlayback)
        _player = MediaPlayer().apply {
            try {
                setDataSource(_fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
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

    private fun startRecording() {
        _recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(_fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            //録音時間の固定(ms)
            val maxDurationMs = _duration*1000
            setMaxDuration(maxDurationMs)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
            start()
            val record = findViewById<Button>(R.id.RecordStart)
            record.setText(R.string.bt_record_now)

            val recodecomplete = Runnable {
                if (_recorder != null) {
                    stop()
                    release()
                    record.setText(R.string.bt_record_finish)
                }
            }
            Handler().postDelayed(recodecomplete, maxDurationMs.toLong())
        }
    }

    private fun stopRecording() {
        _recorder?.apply {
            stop()
            reset()
            release()
            val record = findViewById<Button>(R.id.RecordStart)
            record.setText(R.string.bt_record_start)
        }
        _recorder =null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        _fileName = "${externalCacheDir?.absolutePath}/${UUID.randomUUID()}.3gp"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recode_voice)
        ActivityCompat.requestPermissions(this@RecodeVoice, audiopermissions,
            REQUEST_RECORD_AUDIO_PERMISSION)
        ActivityCompat.requestPermissions(this@RecodeVoice, storagepermissions,
            REQUEST_EXTERNAL_STORAGE_PERMISSION)
        val sex = intent.getStringExtra("sex")?: ""
        val generation = intent.getStringExtra("generation")?: ""
        val title = intent.getStringExtra("title")?: ""
        _duration = intent.getIntExtra("duration",0)
        val duration = _duration.toString()
        val tvsex = findViewById<TextView>(R.id.tvSelectSex)
        tvsex.text = sex
        val tvgeneration = findViewById<TextView>(R.id.tvSelectGeneration)
        tvgeneration.text = generation
        val tvword = findViewById<TextView>(R.id.tvSelectWord)
        tvword.text = title
        val record = findViewById<Button>(R.id.RecordStart)
        val stop = findViewById<Button>(R.id.RecordStop)
        val playback = findViewById<Button>(R.id.RecordPlayback)
        val upload = findViewById<Button>(R.id.RecordRetry)
        val listener = RecordButton()
        record.setOnClickListener(listener)
        stop.setOnClickListener(listener)
        playback.setOnClickListener(listener)
        upload.setOnClickListener(listener)
        _seekbar = findViewById(R.id.sbRecordPlayback)
        _seekbar.setOnSeekBarChangeListener(seekBarChangeListener)

        audiodata = hashMapOf("sex" to sex, "generation" to generation, "title" to title,
            "duration" to duration, "assess_voice" to "0", "assess_content" to "0",
            "assess_sympathy" to "0", "assess_power" to "0")

    }

    private inner class RecordButton : View.OnClickListener {
        override fun onClick(v: View?) {
            Log.i(LOG_TAG, "クリック成功")
            Log.i(LOG_TAG, _fileName)

            if(v !=null) {
                when(v.id) {
                    R.id.RecordStart -> {
                        findViewById<Button>(R.id.RecordStart).isEnabled = false
                        findViewById<Button>(R.id.RecordStop).isEnabled = true
                        onRecord(true)
                        Log.i(LOG_TAG, "録音開始")
                        findViewById<Button>(R.id.RecordPlayback).isEnabled = true
                        findViewById<Button>(R.id.RecordRetry).isEnabled = true
                    }
                    R.id.RecordStop -> {
                        findViewById<Button>(R.id.RecordStart).isEnabled = true
                        findViewById<Button>(R.id.RecordStop).isEnabled = false
                        onRecord(false)
                        Log.i(LOG_TAG, "録音完了")
                    }
                    R.id.RecordPlayback -> {
                        onPlay(true)
                        Log.i(LOG_TAG, "再生中")
                    }
                    R.id.RecordRetry -> {
                        findViewById<Button>(R.id.RecordStop).isEnabled = false
                        findViewById<Button>(R.id.RecordPlayback).isEnabled = false
                        val tvload = findViewById<TextView>(R.id.tvUpload)
                        uploadAudioToFirestore()
                        tvload.setText(R.string.tv_record_check_upload)
                        findViewById<Button>(R.id.RecordRetry).isEnabled = false

                    }

                }
            }
        }
    }

    private fun uploadAudioToFirestore() {
        val storageRef = Firebase.storage.reference

        val documentReference = db.collection("report").document()
        val file = Uri.fromFile(File(_fileName))
        val audioRef = storageRef.child("report/${file.lastPathSegment}")
        val uploadTask = audioRef.putFile(file)
        val tvupload = findViewById<TextView>(R.id.tvUpload)

        audiodata["url"] = audioRef.path

        uploadTask.continueWithTask {task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            audioRef.downloadUrl
        }.addOnCompleteListener {task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                audiodata["downloadUrl"] = downloadUri.toString()
                documentReference.set(audiodata).addOnSuccessListener {
                    val uploaddocumentID = documentReference.id
                    Log.d(TAG, "Audio added")
                    tvupload.setText(R.string.tv_record_check_upload)
                    val myApp = application as MyApp
                    sharedPreferences = myApp.sharedPreferences
                    val editor = sharedPreferences.edit()
                    val randomNumber = generateRandomNumber()
                    editor.putString("$randomNumber", uploaddocumentID)
                    editor.apply()

                }.addOnFailureListener(OnFailureListener { e ->
                    Log.w(TAG, "Error", e)
                })
            } else {
                Log.w(TAG, "Error getting download url", task.exception)
            }
        }
    }

    private fun generateRandomNumber(): Int {
        val random = Random(System.currentTimeMillis())
        return random.nextInt(1, 999999)
    }
}

