package com.github.danishjamal104.notes.ui.fragment.note.adapter

import com.github.danishjamal104.notes.data.model.Label

interface LabelActionListener {

    fun deleteLabel(label: Label)

    fun updateLabelName(oldLabel: Label, newLabelName: String)

    fun updateLabelCheck(oldLabel: Label, checked: Boolean)
}