package com.websarva.wings.android.voice2

import android.Manifest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private const val REQUEST_EXTERNAL_STORAGE_PERMISSION = 400

class CheckReport : AppCompatActivity() {

    private lateinit var check : TextView
    private val db = FirebaseFirestore.getInstance()
    private var _player: MediaPlayer? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var _valueList: MutableList<String> = mutableListOf()
    private var _wordlist: MutableList<MutableMap<String, Any?>> = mutableListOf()
    private val _from = arrayOf("title", "ID", "av", "ac", "as", "ap")
    private val _to = intArrayOf(R.id.tvAssess, R.id.tvAssessId, R.id.tvAssessVoiceNumber,
        R.id.tvAssessContentNumber, R.id.tvAssessSympathyNumber, R.id.tvAssessPowerNumber)
    private lateinit var lvWord: ListView
    private lateinit var _seekbar: SeekBar
    private var _title = ""
    private var _id = ""
    private lateinit var _file : File
    private var _fileName: String = ""

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
        setContentView(R.layout.activity_check_report)

        check = findViewById(R.id.tvCheck)
        ActivityCompat.requestPermissions(this@CheckReport, storagepermissions,
            REQUEST_EXTERNAL_STORAGE_PERMISSION)

        val myApp = application as MyApp
        sharedPreferences = myApp.sharedPreferences
        val allValues: Collection<*> = sharedPreferences.all.values
        for (value in allValues) {
            if (value != null) {
                _valueList.add(value as String)
            }
        }
        lvWord = findViewById(R.id.lvMyReport)
        getMyReport()

        _seekbar = findViewById(R.id.sbCheck)
        _seekbar.setOnSeekBarChangeListener(seekBarChangeListener)
    }

    fun onClickPlay(view: View) {
        onPlay(true)
    }

    private inner class ListItemSelectedListener: AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val item = parent.getItemAtPosition(position) as MutableMap<String, Any>
            get(item)
        }
    }

    private fun get(menu: MutableMap<String, Any>) {
        _title = menu["title"] as String
        _id = menu["ID"] as String
        downloadAudioFile()
    }

     private fun getMyReport() {
         val size = _valueList.size
         //check.text = _valueList.toString()
         val tasks = mutableListOf<Task<DocumentSnapshot>>()
         for (i in 0 until size) {
             val task = db.collection("report")
                 .document(_valueList[i])
                 .get()
                 .addOnSuccessListener { documentSnapshot ->
                     if (documentSnapshot.exists()) {
                         val title = documentSnapshot.getString("title")
                         val assessVoice = documentSnapshot.getString("assess_voice")
                         val assessContent = documentSnapshot.getString("assess_content")
                         val assessSympathy = documentSnapshot.getString("assess_sympathy")
                         val assessPower = documentSnapshot.getString("assess_power")
                         val menu = mutableMapOf<String, Any?>("title" to title,
                             "ID" to _valueList[i], "av" to assessVoice, "ac" to assessContent,
                             "as" to assessSympathy, "ap" to assessPower)
                         _wordlist.add(menu)
                     }
                 }
             tasks.add(task)
         }
         Tasks.whenAllComplete(tasks)
             .addOnSuccessListener {
                 val adapter = SimpleAdapter(
                     this@CheckReport, _wordlist, R.layout.row2, _from, _to)
                 lvWord.adapter = adapter
                 lvWord.onItemClickListener = ListItemSelectedListener()
             }
             .addOnFailureListener {exception ->
                 Log.e(TAG, "Error executing tasks: ", exception)
             }
     }

    private fun downloadAudioFile() {
        val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        db.collection("report")
            .document(_id)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val filePath = querySnapshot.getString("url")
                if (filePath != null) {
                    val storageRef = FirebaseStorage.getInstance().reference.child(filePath)
                    _fileName = "file_0"
                    this._file = File(downloadDir, _fileName)

                    storageRef.getFile(_file)
                        .addOnSuccessListener {
                            val textView = findViewById<TypewriterTextView>(R.id.tvCheck)
                            val text = _id
                            textView.animateText(text)

                        }
                        .addOnFailureListener(OnFailureListener { e ->
                            val textView = findViewById<TypewriterTextView>(R.id.tvCheck)
                            val text = getString(R.string.tv_make_ng)
                            textView.animateText(text)
                        })
                }
            }
            .addOnFailureListener(OnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error", e)
            })
    }
}