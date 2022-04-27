package com.github.danishjamal104.notes.ui.fragment.note.adapter

import android.annotation.SuppressLint
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
import com.github.danishjamal104.notes.util.setSrc
import com.github.danishjamal104.notes.util.visible

@SuppressLint("NotifyDataSetChanged")
class LabelAdapter
constructor(val context: Context): RecyclerView.Adapter<LabelAdapter.LabelViewHolder>(){

    class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val _binding = LabelListItemBinding.bind(itemView)
        val binding get() = _binding

    }

    private var backupData = mutableListOf<Label>()
    private val data = mutableListOf<Label>()

    var labelActionListener: LabelActionListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.label_list_item, parent, false)
        return LabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val item = data[position]
        val binding = holder.binding

        val updateClickListener = View.OnClickListener {
            val newLabelName = binding.labelName.text.toString().trim()
            labelActionListener?.updateLabelName(item, newLabelName)
        }

        val deleteClickListener = View.OnClickListener {
            labelActionListener?.deleteLabel(item)
        }


        binding.labelName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                binding.saveButton.setSrc(context, R.drawable.ic_baseline_check_24)
                if(text == item.value) {
                    binding.saveButton.gone()
                } else {
                    binding.saveButton.visible()
                    binding.saveButton.setOnClickListener(updateClickListener)
                }
                if(text.isEmpty()) {
                    binding.saveButton.setSrc(context, R.drawable.ic_baseline_delete_outline_24)
                    binding.saveButton.setOnClickListener(deleteClickListener)
                }
            }

        })

        binding.checkbox.setOnCheckedChangeListener { _, bool ->
            if(item.checked == bool) {return@setOnCheckedChangeListener}
            labelActionListener?.updateLabelCheck(item, bool)
        }

        binding.checkbox.isChecked = item.checked
        binding.labelName.setText(item.value)
    }

    override fun getItemCount() = data.size

    fun setData(labels: List<Label>) {
        data.clear()
        backupData.clear()
        data.addAll(labels)
        backupData.addAll(labels)
        notifyDataSetChanged()
    }

    private fun addLabels(labels: List<Label>, updateBackup: Boolean = false) {
        data.addAll(labels)
        if(updateBackup) {
            backupData.addAll(data)
        }
        notifyDataSetChanged()
    }

    fun add(label: Label) {
        data.add(label)
        backupData.add(label)
        notifyItemInserted(itemCount)
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

    fun updateLabel(labelId: Int, value: String = "", checked: Boolean? = null) {
        val position = getPosition(labelId)
        val label = data[position]
        if(value != "") {
            label.value = value
        }
        if(checked != null) {
            label.checked = checked
        }
        notifyItemChanged(position)
    }

    fun deleteLabel(labelId: Int) {
        val idx = getPosition(labelId)
        data.removeAt(idx)
        backupData.clear()
        backupData.addAll(data)
        notifyItemRemoved(idx)
    }

    private fun getPosition(labelId: Int):Int {
        val label = data.filter {
            it.id == labelId
        }[0]
        return data.indexOf(label)
    }

    fun getCheckedLabels(): List<Label> {
        return data.filter { it.checked }
    }
}