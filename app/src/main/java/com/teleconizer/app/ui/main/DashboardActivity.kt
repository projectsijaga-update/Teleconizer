package com.teleconizer.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.teleconizer.app.R
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.databinding.ActivityDashboardBinding
import com.teleconizer.app.service.AlarmService
import com.teleconizer.app.ui.settings.EmergencyContactActivity

class DashboardActivity : AppCompatActivity() {

	private lateinit var binding: ActivityDashboardBinding
	private lateinit var viewModel: DashboardViewModel
	private lateinit var adapter: PatientAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityDashboardBinding.inflate(layoutInflater)
		setContentView(binding.root)

		setSupportActionBar(binding.toolbar)

		viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
		
		setupRecycler()
		observePatients()
		observeAliceAlarmState() // ADDED - Observe Zainabun's alarm state
	}

	private fun setupRecycler() {
		adapter = PatientAdapter { patient: Patient ->
			openDetails(patient)
		}
		binding.recyclerPatients.layoutManager = LinearLayoutManager(this)
		binding.recyclerPatients.adapter = adapter
	}

	private fun observePatients() {
		viewModel.patients.observe(this) { list ->
			android.util.Log.d("DashboardActivity", "Received patient list update: ${list.size} patients")
			android.util.Log.d("DashboardActivity", "Patient names: ${list.map { it.name }}")
			adapter.submitList(list)
		}
	}
	
	// ADDED - Observe Zainabun's alarm state and trigger/stop alarm accordingly
	private fun observeAliceAlarmState() {
		viewModel.isAliceInDanger.observe(this) { isInDanger ->
			Log.d("AlarmUpdate", "Zainabun alarm state changed: isInDanger=$isInDanger") // ADDED
			if (isInDanger) {
				// Start alarm when Zainabun is in danger
				triggerEmergencyAlarm()
				Log.d("AlarmUpdate", "Emergency alarm STARTED for Zainabun") // ADDED
				Log.d("AlarmDebug", "triggered") // ADDED
			} else {
				// Stop alarm when Zainabun is safe
				stopEmergencyAlarm()
				Log.d("AlarmUpdate", "Emergency alarm STOPPED for Zainabun") // ADDED
				Log.d("AlarmDebug", "stopped") // ADDED
			}
		}
	}
	
	// ADDED - Trigger emergency alarm service
	private fun triggerEmergencyAlarm() {
		val intent = Intent(this, AlarmService::class.java)
		startService(intent)
		Toast.makeText(this, "DANGER DETECTED for Zainabun! Emergency alert activated!", Toast.LENGTH_LONG).show()
		Log.d("AlarmUpdate", "AlarmService started for Zainabun") // ADDED
		Log.d("AlarmDebug", "Alarm triggered") // ADDED
	}
	
	// ADDED - Stop emergency alarm service
	private fun stopEmergencyAlarm() {
		val intent = Intent(this, AlarmService::class.java)
		stopService(intent)
		Log.d("AlarmUpdate", "AlarmService stopped for Zainabun") // ADDED
		Log.d("AlarmDebug", "Alarm stopped") // ADDED
	}

	private fun openDetails(patient: Patient) {
		val intent = Intent(this, PatientDetailActivity::class.java)
		intent.putExtra(PatientDetailActivity.EXTRA_PATIENT, patient)
		startActivity(intent)
	}


	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu_dashboard, menu)
		
		// Apply blue text color to menu items
		val menuTextBlue = ContextCompat.getColor(this, R.color.menu_text_blue)
		for (i in 0 until menu.size()) {
			val menuItem = menu.getItem(i)
			if (menuItem.itemId == R.id.action_emergency_contact || menuItem.itemId == R.id.action_exit) {
				val title = menuItem.title ?: ""
				val spannableTitle = android.text.SpannableString(title)
				spannableTitle.setSpan(
					android.text.style.ForegroundColorSpan(menuTextBlue),
					0,
					title.length,
					android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				menuItem.title = spannableTitle
			}
		}
		
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_emergency_contact -> {
				startActivity(Intent(this, EmergencyContactActivity::class.java))
				true
			}
			R.id.action_exit -> {
				// Stop alarm before exiting
				stopEmergencyAlarm()
				finishAffinity()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}
	
	override fun onPause() {
		super.onPause()
		// Note: We don't stop alarm on pause because the service should continue
		// The alarm will be stopped only when status changes to safe
	}
	
	override fun onDestroy() {
		super.onDestroy()
		// Ensure alarm is stopped when activity is destroyed
		// This prevents vibration from persisting after app uninstall/close
		if (!isChangingConfigurations) {
			stopEmergencyAlarm()
			Log.d("AlarmUpdate", "DashboardActivity destroyed - alarm stopped")
		}
	}
}


