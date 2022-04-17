package com.github.danishjamal104.notes.ui.fragment.note.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.databinding.LabelListItemBinding
import com.github.danishjamal104.notes.util.gone
import com.github.danishjamal104.notes.util.visible

class LabelAdapter
constructor(val context: Context): RecyclerView.Adapter<LabelAdapter.LabelViewHolder>(){

    class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val _binding = LabelListItemBinding.bind(itemView)
        val binding get() = _binding

    }

    private var backupData = mutableListOf<Label>()
    private val data = mutableListOf<Label>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.label_list_item, parent, false)
        return LabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val item = data[position]
        val binding = holder.binding

        binding.labelName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                if(text == item.value) {
                    binding.saveButton.gone()
                } else {
                    binding.saveButton.visible()
                }
                if(text.isEmpty()) {
                    // todo change the icon to delete
                    //binding.saveButton.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_delete_outline_24))
                }
            }

        })

        binding.checkbox.isChecked = item.checked
        binding.labelName.setText(item.value)
    }

    override fun getItemCount() = data.size

    fun addLabels(labels: List<Label>, updateBackup: Boolean = false) {
        data.addAll(labels)
        if(updateBackup) {
            backupData.addAll(data)
        }
        notifyDataSetChanged()
    }

    fun filter(labelName: String) {
        data.clear()
        if(labelName.isEmpty()) {
            addLabels(backupData)
            return
        }
        addLabels(backupData.filter {
            it.value.lowercase().startsWith(labelName.lowercase())
        })

    }

    fun updateCheckState(labelId: Int, checked: Boolean) {
        val label = data.filter {
            it.id == labelId
        }[0]
        label.checked = checked
        notifyItemChanged(data.indexOf(label))
    }

}