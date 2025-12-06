package com.teleconizer.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.teleconizer.app.databinding.ItemPhoneNumberBinding

class PhoneNumberAdapter(
    private var contacts: List<String>,
    private val onEdit: (String, Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<PhoneNumberAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPhoneNumberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhoneNumberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val number = contacts[position]
        
        // [PERBAIKAN] Menggunakan id 'tvPhoneNumber' sesuai file XML item_phone_number.xml
        holder.binding.tvPhoneNumber.text = number
        
        holder.binding.btnEdit.setOnClickListener {
            // Logika edit sederhana (bisa dikembangkan dengan dialog di activity)
            // Di sini kita trigger callback saja
            // Implementasi detail edit ada di Activity (showEditDialog)
            // Untuk sekarang callback mengirim data saat ini
            onEdit(number, position)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = contacts.size
}
