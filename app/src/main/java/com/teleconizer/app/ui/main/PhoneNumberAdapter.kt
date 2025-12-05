package com.teleconizer.app.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.teleconizer.app.R

class PhoneNumberAdapter(
	private val contacts: MutableList<String>,
	private val onEdit: (String, Int) -> Unit,
	private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<PhoneNumberAdapter.ViewHolder>() {

	private val numbers: MutableList<String> = contacts
	private var selectedPosition: Int = RecyclerView.NO_POSITION

	fun submitList(newNumbers: List<String>) {
		numbers.clear()
		numbers.addAll(newNumbers)
		notifyDataSetChanged()
		if (numbers.isEmpty()) {
			setSelectedPosition(RecyclerView.NO_POSITION)
		}
	}

	fun getSelectedNumber(): String? {
		return if (selectedPosition in numbers.indices) numbers[selectedPosition] else null
	}

	private fun setSelectedPosition(position: Int) {
		val previous = selectedPosition
		selectedPosition = position
		if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
		if (position != RecyclerView.NO_POSITION) notifyItemChanged(position)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_phone_number, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(numbers[position], position == selectedPosition)
	}

	override fun getItemCount(): Int = numbers.size

	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
		private val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
		private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)

		fun bind(number: String, selected: Boolean) {
			tvNumber.text = number
			val ctx = itemView.context
			val bgColor = if (selected) R.color.primary else android.R.color.transparent
			itemView.setBackgroundColor(ContextCompat.getColor(ctx, bgColor))
			ivEdit.setOnClickListener { onEdit(number, bindingAdapterPosition) }
			ivDelete.setOnClickListener { onDelete(bindingAdapterPosition) }
		}
	}
}
