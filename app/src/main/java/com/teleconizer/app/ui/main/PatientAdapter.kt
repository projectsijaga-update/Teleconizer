package com.teleconizer.app.ui.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.databinding.ItemPatientBinding

class PatientAdapter(private val onClick: (Patient) -> Unit) :
	ListAdapter<Patient, PatientAdapter.PatientViewHolder>(DIFF) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
		val binding = ItemPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return PatientViewHolder(binding)
	}

	override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
		val patient = getItem(position)
		android.util.Log.d("PatientAdapter", "Binding patient at position $position: ${patient.name} (${patient.status})")
		holder.bind(patient)
	}

	inner class PatientViewHolder(private val binding: ItemPatientBinding) : RecyclerView.ViewHolder(binding.root) {
		fun bind(patient: Patient) {
			android.util.Log.d("PatientAdapter", "Binding patient: ${patient.name} with status: ${patient.status}")
			binding.tvName.text = patient.name
			
			// Display status in uppercase (DANGER/SAFE)
			val displayStatus = patient.status.uppercase()
			binding.tvStatus.text = displayStatus
			
			// Check for danger status (DANGER, danger, JATUH, jatuh, bahaya)
			val isDanger = displayStatus == "DANGER" || 
			               patient.status.equals("danger", ignoreCase = true) || 
			               patient.status.equals("jatuh", ignoreCase = true) ||
			               patient.status.equals("bahaya", ignoreCase = true)
			val color = if (isDanger) Color.RED else Color.GREEN
			binding.tvStatus.setTextColor(color)
			binding.btnDetails.setOnClickListener { onClick(patient) }
		}
	}

	companion object {
		val DIFF = object : DiffUtil.ItemCallback<Patient>() {
			override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean {
				val isSame = oldItem.id == newItem.id
				android.util.Log.d("PatientAdapter", "areItemsTheSame: $isSame for ${oldItem.name} vs ${newItem.name}")
				return isSame
			}
			override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean {
				val isSame = oldItem == newItem
				android.util.Log.d("PatientAdapter", "areContentsTheSame: $isSame for ${oldItem.name} vs ${newItem.name}")
				return isSame
			}
		}
	}
}


