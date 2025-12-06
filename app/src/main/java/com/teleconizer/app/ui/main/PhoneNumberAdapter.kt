package com.teleconizer.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.teleconizer.app.data.model.ContactModel
import com.teleconizer.app.databinding.ItemPhoneNumberBinding

class PhoneNumberAdapter(
    private var contacts: List<ContactModel>,
    private val onEdit: (ContactModel, Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<PhoneNumberAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPhoneNumberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhoneNumberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        
        holder.binding.tvContactName.text = contact.name
        holder.binding.tvPhoneNumber.text = contact.number
        
        holder.binding.btnEdit.setOnClickListener {
            onEdit(contact, position)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = contacts.size
}
