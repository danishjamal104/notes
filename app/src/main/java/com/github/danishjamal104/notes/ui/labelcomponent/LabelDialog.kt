package com.github.danishjamal104.notes.ui.labelcomponent

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.databinding.LabelLayoutBinding
import com.github.danishjamal104.notes.ui.fragment.note.adapter.DialogAction
import com.github.danishjamal104.notes.ui.fragment.note.adapter.LabelAdapter
import com.github.danishjamal104.notes.util.gone
import com.github.danishjamal104.notes.util.hideKeyboard
import com.github.danishjamal104.notes.util.visible

class LabelDialog(
    val context: Context,
    val labelAdapter: LabelAdapter,
    private val dialogAction: DialogAction
) : LabelComponent {

    private var dialog: Dialog = Dialog(context, R.style.Theme_Notes)

    @SuppressLint("InflateParams")
    private var _binding: LabelLayoutBinding = LabelLayoutBinding.bind(
        LayoutInflater.from(context).inflate(R.layout.label_layout, null)
    )
    private val binding get() = _binding

    init {
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.backButton.setOnClickListener { hide() }

        binding.labelName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                labelAdapter.filter(text)
                if(text.isEmpty()) {
                    binding.headerCreateLabel.gone()
                    return
                } else {
                    binding.headerCreateLabel.visible()
                }
                binding.createLabelText.text = context.getString(R.string.create) + "\"$text\""
            }

        })

        labelAdapter.labelActionListener = dialogAction

        binding.createLabelButton.setOnClickListener {
            dialogAction.createLabel(binding.labelName.text.toString().trim())
        }

        binding.labelList.layoutManager = LinearLayoutManager(context)
        binding.labelList.setHasFixedSize(false)
        binding.labelList.adapter = labelAdapter
    }

    override fun releaseFocus() {
        binding.labelName.text?.clear()
        binding.labelName.clearFocus()
        context.hideKeyboard(binding.labelName)
        binding.labelList.focusedChild?.let {
            context.hideKeyboard(it)
            it.clearFocus()
        }
    }

    override fun show() {
        dialog.show()
    }

    override fun hide() {
        dialog.dismiss()
    }


}