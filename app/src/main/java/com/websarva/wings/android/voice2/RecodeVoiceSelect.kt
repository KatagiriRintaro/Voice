package com.websarva.wings.android.voice2

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RecodeVoiceSelect : AppCompatActivity() {

    private var _wordlist: MutableList<MutableMap<String, Any>> = mutableListOf()
    private val _from = arrayOf("title", "duration")
    private val _to = intArrayOf(R.id.tvRecordWord, R.id.tvWordDuration)
    private var _duration = 0
    private var _title = ""
    private var _total = 0
    private val db = FirebaseFirestore.getInstance()
    private lateinit var lvWord: ListView
    private var _diary1 = HashMap<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recode_voice_select)

        lvWord = findViewById(R.id.lvSelectWord)
        checkList("total","総括レポート")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_options_menu_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var returnVal = true
        when (item.itemId) {
            R.id.TotalList ->
                checkList("total","総括レポート")
            R.id.GladList ->
                checkList("glad","感情レポート(喜)")
            R.id.AngryList ->
                checkList("angry","感情レポート(怒)")
            R.id.SadList ->
                checkList("sad","感情レポート(哀)")
            R.id.FunList ->
                checkList("fun","感情レポート(楽)")
            else ->
                returnVal = super.onOptionsItemSelected(item)
        }
        return returnVal
    }



    private fun createList(collection : String) {
        _wordlist.clear()
        db.collection(collection)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this@RecodeVoiceSelect, "なし", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in querySnapshot) {
                        val documentData = document.data
                        val duration = documentData["duration"]?.toString()?.toIntOrNull()
                        val title = documentData["title"]?.toString()

                        if (duration != null && title != null) {
                            Log.d(TAG, "$documentData")
                            Log.d(TAG, "${document.id} => $duration")
                            val menu =
                                mutableMapOf<String, Any>("title" to title, "duration" to duration)
                            _wordlist.add(menu)
                        }
                    }

                    val adapter = SimpleAdapter(
                        this@RecodeVoiceSelect, _wordlist, R.layout.row, _from, _to
                    )
                    lvWord.adapter = adapter
                    lvWord.onItemClickListener = ListItemSelectedListener()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this@RecodeVoiceSelect, "データの取得に失敗しました",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Error getting documents: ", exception)
            }
    }

    private fun checkList(collection: String, title: String) {
        val currentDate = LocalDate.now()
        val currentTime = LocalTime.now()
        val monthFormatter = DateTimeFormatter.ofPattern("MM")
        val dayFormatter = DateTimeFormatter.ofPattern("dd")
        val year = currentDate.year
        val month = currentDate.format(monthFormatter)
        val day = currentDate.format(dayFormatter)
        db.collection(collection)
            .whereEqualTo("year", year)
            .whereEqualTo("month", month)
            .whereEqualTo("day", day)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents.size
                if (documents == 0 ) {
                    _diary1 = hashMapOf(
                        "year" to year, "month" to month, "day" to day,
                        "title" to "$title($year/$month/$day)", "duration" to "30"
                    )
                    addList(collection, year, month, day)
                } else {
                    createList(collection)
                }
            }
            .addOnFailureListener { exception ->
                // 追加失敗時の処理
                Log.e(TAG, "Error adding document: ", exception)
            }
    }

    private fun addList(collection: String, year: Int, month: String, day: String ) {
        db.collection(collection)
            .document("$year$month$day")
            .set(_diary1)
            .addOnSuccessListener {
                createList(collection)
            }
            .addOnFailureListener { exception ->
                // 追加失敗時の処理
                Log.e(TAG, "Error adding document: ", exception)
            }
    }

    private inner class ListItemSelectedListener: AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val item = parent.getItemAtPosition(position) as MutableMap<String, Any>
            get(item)
        }

    }

    private fun get(menu: MutableMap<String, Any>) {
        _title = menu["title"] as String
        _duration = menu["duration"] as Int
        val selectedword = findViewById<TextView>(R.id.tvSelectedWord)
        selectedword.text = _title
    }

    fun onButtonClick2SelectDialog(view: View) {
        val selectgeneration =
            findViewById<Spinner>(R.id.spSelectGeneration).selectedItem.toString()
        val selectradioid = findViewById<RadioGroup>(R.id.rgSex).checkedRadioButtonId
        if (selectradioid == -1) {
            Toast.makeText(
                this@RecodeVoiceSelect, "性別を入力してくれ",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (_title.isEmpty()) {
            Toast.makeText(
                this@RecodeVoiceSelect, "作成するレポートを選択してくれ",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val selectradiotext = findViewById<RadioButton>(selectradioid).text.toString()
        val duration = _duration.toString()

        db.collection("report")
            .whereEqualTo("sex", selectradiotext)
            .whereEqualTo("generation", selectgeneration)
            .whereEqualTo("title", _title)
            .whereEqualTo("duration", duration)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                _total = documents.size
                Log.d(TAG, "$_total")

                val dialogFragment = SelectConfirmDialogFragment()
                Log.d(TAG, "b $_total")
                val args = Bundle().apply {
                    putString("sex", selectradiotext)
                    putString("generation", selectgeneration)
                    putString("title", _title)
                    putInt("duration", _duration)
                    putInt("total", _total)
                }
                Log.d(TAG, "a $_total")
                dialogFragment.arguments = args
                dialogFragment.show(supportFragmentManager, "SelectConfirmDialogFragment")
            }
            .addOnFailureListener (OnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error", e)
            })
    }
}