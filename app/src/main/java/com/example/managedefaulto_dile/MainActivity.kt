package com.example.managedefaulto_dile

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var telephonyManager: TelephonyManager
    private var callStateCallback: CallStateListener? = null

    private lateinit var prefs: SharedPreferences
    private lateinit var statusTextView: TextView
    private lateinit var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("MainActivity", "Call Screening role granted.")
        } else {
            Log.d("MainActivity", "Call Screening role not granted.")
        }
    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callStateCallback = CallStateListener(this)
            telephonyManager.registerTelephonyCallback(mainExecutor, callStateCallback!!)
        }

        requestRole()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 101)
        }

        // SharedPreferences & Status TextView
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        statusTextView = findViewById(R.id.statusTextView)

        // Initial UI state
        updateServiceStatusUI(prefs.getBoolean("serviceStatus", false))

        // Listen for service status changes
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == "serviceStatus") {
                val isActive = sharedPrefs.getBoolean("serviceStatus", false)
                updateServiceStatusUI(isActive)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        // Input field & save button
        val inputField = findViewById<EditText>(R.id.editTextMessage)
        val saveButton = findViewById<Button>(R.id.btnSave)
        inputField.setText(prefs.getString("sendMessage", ""))
        saveButton.setOnClickListener {
            prefs.edit().putString("sendMessage", inputField.text.toString()).apply()
            Toast.makeText(this, "Message saved successfully", Toast.LENGTH_LONG).show()
        }

        // Toggle service button
        findViewById<Button>(R.id.btnToggleService).setOnClickListener {
            val serviceStatus = prefs.getBoolean("serviceStatus", false)
            val intent = Intent(this, MyForegroundService::class.java)
            intent.putExtra(MyForegroundService.EXTRA_MESSAGE, "Foreground service running")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!serviceStatus) startForegroundService(intent)
                else stopService(intent)
            } else {
                if (!serviceStatus) startService(intent)
                else stopService(intent)
            }

            prefs.edit { putBoolean("serviceStatus", !serviceStatus) }
        }
    }

    private fun updateServiceStatusUI(isActive: Boolean) {
        if (isActive) {
            statusTextView.text = "Service is Active"
            statusTextView.setTextColor(Color.GREEN)
        } else {
            statusTextView.text = "Service is Inactive"
            statusTextView.setTextColor(Color.RED)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callStateCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        }
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun requestRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            val isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
            val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            if (isRoleAvailable && !isRoleHeld) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleRequestLauncher.launch(intent)
            }
        }
    }
}
