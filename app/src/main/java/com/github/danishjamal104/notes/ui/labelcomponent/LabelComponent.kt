package com.github.danishjamal104.notes.ui.labelcomponent

import android.content.Context
import com.github.danishjamal104.notes.ui.fragment.note.adapter.LabelAdapter

interface LabelComponent {
    companion object {
        fun bind(context: Context, labelAdapter: LabelAdapter): LabelComponent {
            return LabelDialog(context, labelAdapter)
        }
    }
    fun show()
    fun hide()
}