package com.websarva.wings.android.voice2

import android.content.ContentValues.TAG
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


class MakeVoiceSelect : AppCompatActivity() {

    private var _wordlist: MutableList<MutableMap<String, Any>> = mutableListOf()
    private val _from = arrayOf("title", "duration")
    private val _to = intArrayOf(R.id.tvRecordWord, R.id.tvWordDuration)
    private var _duration = 0
    private var _title = ""
    private var _total = 0
    private val db = FirebaseFirestore.getInstance()
    private lateinit var lvWord : ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_voice_select)

        lvWord = findViewById<ListView>(R.id.lvSelectWord)
        createList("total")

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_options_menu_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var returnVal = true
        when (item.itemId) {
            R.id.TotalList ->
                createList("total")
            R.id.GladList ->
                createList("glad")
            R.id.AngryList ->
                createList("angry")
            R.id.SadList ->
                createList("sad")
            R.id.FunList ->
                createList("fun")
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
                    Toast.makeText(this@MakeVoiceSelect, "なし", Toast.LENGTH_SHORT).show()
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
                        this@MakeVoiceSelect, _wordlist, R.layout.row, _from, _to
                    )
                    lvWord.adapter = adapter
                    lvWord.onItemClickListener = ListItemSelectedListener()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this@MakeVoiceSelect, "データの取得に失敗しました",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Error getting documents: ", exception)
            }
    }

    private inner  class ListItemSelectedListener: AdapterView.OnItemClickListener {
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

    fun onButtonClick2MakeDialog(view: View) {
        val selectgeneration = findViewById<Spinner>(R.id.spSelectGeneration).selectedItem.toString()
        val selectradioid = findViewById<RadioGroup>(R.id.rgSex).checkedRadioButtonId
        if (selectradioid == -1) {
            Toast.makeText(this@MakeVoiceSelect, "性別を入力してくれ",
                Toast.LENGTH_LONG).show()
            return
        }
        if (_title.isEmpty()) {
            Toast.makeText(this@MakeVoiceSelect, "作成するワードを選択してくれ",
                Toast.LENGTH_LONG).show()
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

                val dialogFragment = MakeConfirmDialogFragment()
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
                dialogFragment.show(supportFragmentManager, "MakeConfirmDialogFragment")
            }
            .addOnFailureListener (OnFailureListener { e ->
                Log.w(TAG, "Error", e)
            })

    }

}