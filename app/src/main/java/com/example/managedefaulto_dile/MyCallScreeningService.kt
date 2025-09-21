package com.example.managedefaulto_dile

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.SmsManager

class MyCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle.schemeSpecificPart

        // Get service status from SharedPreferences
        val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val serviceStatus = prefs.getBoolean("serviceStatus", false)
        val sendMessage = prefs.getString("sendMessage", "Please call me after some time")
        // Block call if service is active
        val response = CallResponse.Builder()
            .setDisallowCall(serviceStatus)
            .setRejectCall(serviceStatus) // reject call if blocking
            .setSkipNotification(serviceStatus)
            .build()
        respondToCall(callDetails, response)

        // Send SMS if call is blocked
        if (serviceStatus) {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                sendMessage,
                null,
                null
            )
        }
    }
}
