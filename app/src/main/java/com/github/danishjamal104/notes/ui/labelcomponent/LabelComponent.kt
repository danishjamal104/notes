package com.github.danishjamal104.notes.ui.labelcomponent

import android.content.Context
import com.github.danishjamal104.notes.ui.fragment.note.adapter.DialogAction
import com.github.danishjamal104.notes.ui.fragment.note.adapter.LabelAdapter

interface LabelComponent {
    companion object {
        fun bind(context: Context, labelAdapter: LabelAdapter, dialogAction: DialogAction): LabelComponent {
            return LabelDialog(context, labelAdapter, dialogAction)
        }
    }
    fun show()
    fun hide()
    fun releaseFocus()
}