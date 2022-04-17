package com.github.danishjamal104.notes.ui.fragment.note.adapter

interface DialogAction: LabelActionListener {

    fun createLabel(labelName: String)

}