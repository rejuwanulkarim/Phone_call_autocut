package com.example.managedefaulto_dile


import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("MainActivity", "Call Screening role granted.")
            // You can optionally update the UI or a status flag here
        } else {
            Log.d("MainActivity", "Call Screening role not granted.")
            // Inform the user that the feature won't work without the role
        }
    }

    private lateinit var telephonyManager: TelephonyManager
    private var callStateCallback: CallStateListener? = null

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

//        ==========================>

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callStateCallback = CallStateListener(this)
            telephonyManager.registerTelephonyCallback(mainExecutor, callStateCallback!!)
        }


//        =================================>


        requestRole()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 101)
        }


//      Start block of   set data in local storage===================>
        val prefs = this.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

//      Stop block of   set data in local storage===================>
        val statusTextView = findViewById<TextView>(R.id.statusTextView)

        if (prefs.getBoolean("serviceStatus", false)) {
            // Active state
            statusTextView.text = "Service is Active"
            statusTextView.setTextColor(Color.GREEN) // or use ContextCompat.getColor for resources
        } else {
            // Inactive state
            statusTextView.text = "Service is Inactive"
            statusTextView.setTextColor(Color.RED) // or a color from your resources
        }


//        get input values============================>

        val inputField = findViewById<EditText>(R.id.editTextMessage)
        val saveButton = findViewById<Button>(R.id.btnSave)

// Fetch saved value when app starts
        val savedMessage = prefs.getString("sendMessage", "")
        inputField.setText(savedMessage)

// Save value when button is clicked
        saveButton.setOnClickListener {
            val inputVal = inputField.text.toString() // get current value
            prefs.edit().putString("sendMessage", inputVal).apply()
            Toast.makeText(this,"Meassage saved successfully", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnToggleService).setOnClickListener {
            val serviceStatus = prefs.getBoolean("serviceStatus", false) // read current status

            val intent = Intent(this, MyForegroundService::class.java)
            intent.putExtra(MyForegroundService.EXTRA_MESSAGE, "Foreground service running")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!serviceStatus) {
                    statusTextView.text = "Service is Active"
                    statusTextView.setTextColor(Color.GREEN)

                    startForegroundService(intent)
                } else {
                    statusTextView.text = "Service is Inactive"
                    statusTextView.setTextColor(Color.RED)
                    stopService(intent)
                }
            } else {
                if (!serviceStatus) {
                    statusTextView.text = "Service is Active"
                    statusTextView.setTextColor(Color.GREEN)
                    startService(intent)
                } else {
                    statusTextView.text = "Service is Inactive"
                    statusTextView.setTextColor(Color.RED)
                    stopService(intent)
                }
            }

            // toggle the status in SharedPreferences
            prefs.edit { putBoolean("serviceStatus", !serviceStatus) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callStateCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        }
    }

        private fun requestRole() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
                val isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

                if (isRoleAvailable && !isRoleHeld) {
                    val intent =
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleRequestLauncher.launch(intent)
                }
            }
        }
    }
