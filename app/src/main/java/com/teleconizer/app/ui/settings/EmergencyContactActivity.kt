package com.teleconizer.app.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.teleconizer.app.databinding.ActivityEmergencyContactBinding

class EmergencyContactActivity : AppCompatActivity() {

	private lateinit var binding: ActivityEmergencyContactBinding
	private lateinit var prefs: SharedPreferences

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityEmergencyContactBinding.inflate(layoutInflater)
		setContentView(binding.root)

		prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
		binding.etName.setText(prefs.getString(KEY_NAME, ""))
		binding.etPhone.setText(prefs.getString(KEY_PHONE, ""))

		updateDeleteVisibility()
		updateCallEnabled()

		binding.btnSave.setOnClickListener {
			val name = binding.etName.text.toString().trim()
			val phone = binding.etPhone.text.toString().trim()
			if (name.isEmpty() || phone.isEmpty()) {
				Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}
			// Save single fields for backward compatibility
			prefs.edit()
				.putString(KEY_NAME, name)
				.putString(KEY_PHONE, phone)
				.apply()
			// Append phone to shared multi-number set used by patient detail
			val numbers = prefs.getStringSet("numbers", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
			numbers.add(phone)
			prefs.edit().putStringSet("numbers", numbers).apply()
			Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
			updateDeleteVisibility()
			updateCallEnabled()
		}

		binding.btnDelete.setOnClickListener {
			prefs.edit()
				.remove(KEY_NAME)
				.remove(KEY_PHONE)
				.remove(KEY_PHONE_LIST)
				.apply()
			binding.etName.setText("")
			binding.etPhone.setText("")
			Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
			updateDeleteVisibility()
			updateCallEnabled()
		}

		binding.btnCall.setOnClickListener {
			val phone = prefs.getString(KEY_PHONE, null)
			if (phone.isNullOrBlank()) {
				Toast.makeText(this, "No number saved", Toast.LENGTH_SHORT).show()
			} else {
				val intent = Intent(Intent.ACTION_DIAL)
				intent.data = Uri.parse("tel:$phone")
				startActivity(intent)
			}
		}
	}

	private fun updateDeleteVisibility() {
		val hasContact = prefs.contains(KEY_NAME) || prefs.contains(KEY_PHONE) || prefs.contains(KEY_PHONE_LIST)
		binding.btnDelete.visibility = if (hasContact) View.VISIBLE else View.GONE
	}

	private fun updateCallEnabled() {
		val phone = prefs.getString(KEY_PHONE, null)
		val list = prefs.getStringSet(KEY_PHONE_LIST, emptySet())
		binding.btnCall.isEnabled = !phone.isNullOrBlank() || !list.isNullOrEmpty()
	}

	companion object {
		private const val PREFS_NAME = "teleconizer_prefs"
		private const val KEY_NAME = "emergency_name"
		private const val KEY_PHONE = "emergency_phone"
		private const val KEY_PHONE_LIST = "emergency_phone_list"
	}
}


