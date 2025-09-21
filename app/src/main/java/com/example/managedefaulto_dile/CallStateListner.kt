package com.example.managedefaulto_dile

import android.content.Context
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S) // API 31+
class CallStateListener(private val context: Context) : TelephonyCallback(),
    TelephonyCallback.CallStateListener {

    override fun onCallStateChanged(state: Int) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val serviceStatus = prefs.getBoolean("serviceStatus", false)

        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d("CallState", "Idle (no call)")
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d("CallState", "Incoming call ringing")

                if (serviceStatus) { // ✅ Only cut call if service is active
                    val telecomManager =
                        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val success = telecomManager.endCall()
                            Log.d("CallState", "Call hangup attempted: $success")

                            if (success) {
                                // Get custom SMS message
                                val sendMessage = prefs.getString(
                                    "sendMessage",
                                    "Please call me after some time"
                                )

                                // ⚠️ Phone number is NOT available here on Android 12+
                                // You need a BroadcastReceiver for incoming call number
                                val incomingNumber =
                                    prefs.getString("lastIncomingNumber", null)

                                Log.d("CallState",incomingNumber.toString());
                                if (!incomingNumber.isNullOrEmpty()) {
                                    val smsManager = SmsManager.getDefault()
                                    smsManager.sendTextMessage(
                                        incomingNumber,
                                        null,
                                        sendMessage,
                                        null,
                                        null
                                    )
                                    Log.d("CallState", "SMS sent to $incomingNumber")
                                } else {
                                    Log.w("CallState", "Incoming number unknown → SMS not sent")
                                }
                            }
                        } else {
                            Log.w("CallState", "endCall() requires API 28+")
                        }
                    } catch (e: SecurityException) {
                        Log.e("CallState", "Permission denied to end call", e)
                    } catch (e: Exception) {
                        Log.e("CallState", "Error ending call", e)
                    }
                } else {
                    Log.d("CallState", "Service inactive → call allowed")
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d("CallState", "Call answered or outgoing call")
            }
        }
    }
}
