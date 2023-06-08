package com.websarva.wings.android.voice2

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class SelectConfirmDialogFragment : DialogFragment() {

    private lateinit var sex : String
    private lateinit var generation : String
    private lateinit var title : String
    private  var duration = 0
    private var total = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = activity?.let {
            sex = arguments?.getString("sex", "") ?: ""
            generation = arguments?.getString("generation", "") ?: ""
            title = arguments?.getString("title", "") ?: ""
            duration = arguments?.getInt("duration", 0) ?: 0
            total = arguments?.getInt("total", 0) ?: 0

            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.dialog_title)
            builder.setMessage(getString(R.string.dialog_msg) + "\n"
                    +getString(R.string.dialog_msg_sex) + " " + sex + "\n"
                    + getString(R.string.dialog_msg_generation) + " " + generation + "\n"
                    + getString(R.string.dialog_msg_word) + title + "\n"
                    + getString(R.string.dialog_msg_duration) + duration
                    + getString(R.string.tv_duration_unit) + "\n"
                    + getString(R.string.dialog_msg_register) + total)
            builder.setPositiveButton(R.string.dialog_btn_ok_record, DialogButtonClickListener())
            builder.setNeutralButton(R.string.dialog_btn_ng, DialogButtonClickListener())
            builder.create()
        }
        return dialog ?: throw IllegalStateException("アクティビティがnullです")
    }

    private inner class DialogButtonClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, which: Int) {

            when(which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val intent = Intent(requireContext(), RecodeVoice::class.java).apply {
                        putExtra("sex", arguments?.getString("sex") ?: "")
                        putExtra("generation", arguments?.getString("generation") ?: "")
                        putExtra("title", arguments?.getString("title") ?: "")
                        putExtra("duration", arguments?.getInt("duration") ?: 0)

                    }
                    startActivity(intent)
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    dialog?.dismiss()
                }
            }
        }
    }
}