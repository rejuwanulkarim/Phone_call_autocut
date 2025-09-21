package com.example.managedefaulto_dile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                val state = bundle.getString(TelephonyManager.EXTRA_STATE)

                if (TelephonyManager.EXTRA_STATE_RINGING == state) {
                    val incomingNumber =
                        bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER)

                    Log.d("IncomingCallReceiver", "Incoming number: $incomingNumber")

                    // Save number to SharedPreferences
                    if (!incomingNumber.isNullOrEmpty()) {
                        val prefs =
                            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("lastIncomingNumber", incomingNumber).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IncomingCallReceiver", "Error in onReceive: ${e.message}")
        }
    }
}
