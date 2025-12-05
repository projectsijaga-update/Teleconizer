package com.teleconizer.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.teleconizer.app.R
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.databinding.ActivityPatientDetailBinding
import com.teleconizer.app.ui.settings.EmergencyContactActivity
import android.content.SharedPreferences

class PatientDetailActivity : AppCompatActivity() {

	private lateinit var binding: ActivityPatientDetailBinding
	private lateinit var phoneAdapter: PhoneNumberAdapter
	private lateinit var contactList: MutableList<String>
	private var mediaPlayer: MediaPlayer? = null
	private var toneGenerator: ToneGenerator? = null
	private var isAlarmPlaying: Boolean = false
	private lateinit var prefs: SharedPreferences
	private lateinit var editor: SharedPreferences.Editor

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityPatientDetailBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Initialize SharedPreferences for phone numbers
		prefs = getSharedPreferences("phone_numbers", MODE_PRIVATE)
		editor = prefs.edit()

		setSupportActionBar(binding.toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val patient = intent.getParcelableExtra<Patient>(EXTRA_PATIENT)
		if (patient != null) {
			bindPatient(patient)
			if (patient.status.equals("danger", ignoreCase = true) || patient.status.equals("bahaya", ignoreCase = true)) {
				startAlarm()
			}
		}

		binding.btnBack.setOnClickListener {
			finish()
		}

		// Setup phone list
		val initialContacts: MutableSet<String> = prefs.getStringSet("contacts", mutableSetOf())!!.toMutableSet()
		contactList = initialContacts.toMutableList()
		phoneAdapter = PhoneNumberAdapter(
			contacts = contactList,
			onEdit = { number, index -> editContact(number, index) },
			onDelete = { index -> deleteContact(index) }
		)
		binding.rvPhoneNumbers.layoutManager = LinearLayoutManager(this)
		binding.rvPhoneNumbers.adapter = phoneAdapter

		// Populate contacts
		phoneAdapter.submitList(contactList)

		// Enable Call button if list is not empty
		binding.btnCall.isEnabled = binding.btnCall.isEnabled || contactList.isNotEmpty()

		// Stop alarm button
		binding.btnStopAlarm.setOnClickListener {
			stopAlarm()
		}

		// Add Contact button
		binding.btnAddContact.setOnClickListener {
			showAddContactDialog()
		}
	}

	private fun editContact(newNumber: String, index: Int) {
		if (index < 0) return
		if (index >= contactList.size) return
		val oldNumber = contactList[index]
		contactList[index] = newNumber
		// persist
		val set = contactList.toMutableSet()
		editor.putStringSet("contacts", set)
		editor.apply()
		phoneAdapter.submitList(contactList)
		phoneAdapter.notifyItemChanged(index)
	}

	private fun deleteContact(index: Int) {
		if (index < 0) return
		if (index >= contactList.size) return
		contactList.removeAt(index)
		// persist
		val set = contactList.toMutableSet()
		editor.putStringSet("contacts", set)
		editor.apply()
		phoneAdapter.submitList(contactList)
		phoneAdapter.notifyItemRemoved(index)
		binding.btnCall.isEnabled = binding.btnCall.isEnabled && contactList.isNotEmpty()
	}

	private fun showEditNumberDialog(oldNumber: String) {
		val input = EditText(this)
		input.setText(oldNumber)
		AlertDialog.Builder(this)
			.setTitle("Edit Number")
			.setView(input)
			.setPositiveButton("Save") { _, _ ->
				val newNumber = input.text.toString().trim()
				if (newNumber.isNotEmpty()) {
					val index = contactList.indexOf(oldNumber)
					if (index >= 0) {
						editContact(newNumber, index)
					}
				}
			}
			.setNegativeButton("Cancel", null)
			.show()
	}

	private fun deleteNumber(number: String) {
		val numbers: MutableSet<String> = prefs.getStringSet("contacts", mutableSetOf())!!.toMutableSet()
		if (numbers.remove(number)) {
			editor.putStringSet("contacts", numbers)
			editor.apply()
			val listNumbers: List<String> = numbers.toList()
			phoneAdapter.submitList(listNumbers)
			binding.btnCall.isEnabled = binding.btnCall.isEnabled && listNumbers.isNotEmpty()
		}
	}

	private fun showAddContactDialog() {
		val input = EditText(this)
		input.hint = "Phone number"
		AlertDialog.Builder(this)
			.setTitle("Add Contact")
			.setView(input)
			.setPositiveButton("Save") { _, _ ->
				val number = input.text.toString().trim()
				if (number.isNotEmpty()) {
					contactList.add(number)
					val set = contactList.toMutableSet()
					editor.putStringSet("contacts", set)
					editor.apply()
					phoneAdapter.submitList(contactList)
					phoneAdapter.notifyItemInserted(contactList.size - 1)
					binding.btnCall.isEnabled = binding.btnCall.isEnabled || contactList.isNotEmpty()
				}
			}
			.setNegativeButton("Cancel", null)
			.show()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu_patient_detail, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> { finish(); true }
			R.id.action_back -> { finish(); true }
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun bindPatient(patient: Patient) {
		binding.toolbar.title = patient.name
		binding.tvStatus.text = patient.status.uppercase()
		val color = if (patient.status.equals("danger", ignoreCase = true) || patient.status.equals("DANGER", ignoreCase = true)) getColor(android.R.color.holo_red_dark) else getColor(android.R.color.holo_green_dark)
		binding.tvStatus.setTextColor(color)
		// Show N/A when coordinates are missing (treated as 0.0)
		val latDisplay = if (patient.latitude == 0.0) "N/A" else patient.latitude.toString()
		val lonDisplay = if (patient.longitude == 0.0) "N/A" else patient.longitude.toString()
		binding.tvCoordinates.text = "$latDisplay, $lonDisplay"
		binding.btnOpenInMaps.setOnClickListener {
			val uri = Uri.parse("geo:${patient.latitude},${patient.longitude}?q=${patient.latitude},${patient.longitude}")
			val intent = Intent(Intent.ACTION_VIEW, uri)
			intent.setPackage("com.google.android.apps.maps")
			startActivity(intent)
		}

		// Load emergency contact and set UI
		val prefs = getSharedPreferences("teleconizer_prefs", MODE_PRIVATE)
		val name = prefs.getString("emergency_name", getString(R.string.not_set))
		val phone = prefs.getString("emergency_phone", getString(R.string.not_set))
		binding.tvEmergencyName.text = getString(R.string.emergency_contact_display, name ?: getString(R.string.not_set), phone ?: getString(R.string.not_set))
		binding.tvEmergencyPhone.text = ""
		binding.btnCall.isEnabled = !phone.isNullOrBlank() && phone != getString(R.string.not_set)
		binding.btnCall.setOnClickListener {
			val contactPrefs = getSharedPreferences("phone_numbers", MODE_PRIVATE)
			val contacts: MutableSet<String> = contactPrefs.getStringSet("contacts", mutableSetOf())!!.toMutableSet()
			if (contacts.isEmpty()) {
				Toast.makeText(this, "Please add a contact first.", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}
			val items = contacts.toList().toTypedArray()
			AlertDialog.Builder(this)
				.setTitle("Select contact to call")
				.setItems(items) { _, which ->
					val numberToCall = items[which]
					if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
						val call = Intent(Intent.ACTION_CALL)
						call.data = Uri.parse("tel:$numberToCall")
						startActivity(call)
					} else {
						val dial = Intent(Intent.ACTION_DIAL)
						dial.data = Uri.parse("tel:$numberToCall")
						startActivity(dial)
					}
				}
				.show()
		}



		binding.btnEditContact?.setOnClickListener {
			startActivity(Intent(this, EmergencyContactActivity::class.java))
		}
		binding.btnDeleteContact?.setOnClickListener {
			prefs.edit()
				.remove("emergency_name")
				.remove("emergency_phone")
				.apply()
			binding.tvEmergencyName.text = getString(R.string.emergency_contact_display, getString(R.string.not_set), getString(R.string.not_set))
			binding.tvEmergencyPhone.text = ""
			binding.btnCall.isEnabled = false
		}
	}

	private fun startAlarm() {
		if (isAlarmPlaying) return
		isAlarmPlaying = true
		try {
			val resId = resources.getIdentifier("alarm_sound", "raw", packageName)
			if (resId != 0) {
				mediaPlayer = MediaPlayer.create(this, resId)
			} else {
				mediaPlayer = null
			}
			mediaPlayer?.apply {
				isLooping = true
				setOnErrorListener { _, _, _ ->
					stopAlarm()
					true
				}
				start()
			}
			if (mediaPlayer == null) throw IllegalStateException("Missing raw alarm resource")
		} catch (_: Exception) {
			// Fallback to tone generator if raw not available
			toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
			Thread {
				while (isAlarmPlaying) {
					toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
					try { Thread.sleep(1200) } catch (_: InterruptedException) {}
				}
			}.start()
		}
	}

	private fun stopAlarm() {
		isAlarmPlaying = false
		mediaPlayer?.let {
			try {
				if (it.isPlaying) it.stop()
			} catch (_: Exception) {}
			it.release()
		}
		mediaPlayer = null
		toneGenerator?.release()
		toneGenerator = null
	}

	override fun onPause() {
		super.onPause()
		stopAlarm()
	}

	override fun onDestroy() {
		super.onDestroy()
		stopAlarm()
	}

	companion object {
		const val EXTRA_PATIENT = "extra_patient"
	}
}


