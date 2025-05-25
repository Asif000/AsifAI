package com.example.mymodernapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
// import android.widget.Toast // Toasts in BroadcastReceiver are discouraged
import androidx.core.content.ContextCompat
// Firebase specific imports are not directly needed here if MainActivity handles updating SharedPreferences

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"
    private var targetPhoneNumber: String? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "SEND_SMS permission not granted. Cannot forward SMS.")
                // Toast.makeText(context, "SEND_SMS permission needed to forward messages.", Toast.LENGTH_LONG).show() // Avoid Toast in Receiver
                return
            }

            loadTargetPhoneNumber(context) // Load the number from SharedPreferences (which MainActivity updates from Remote Config)

            if (targetPhoneNumber.isNullOrEmpty()) {
                Log.e(TAG, "Target phone number is not set (checked SharedPreferences). Cannot forward SMS.")
                // Toast.makeText(context, "Target phone number not set.", Toast.LENGTH_LONG).show() // Avoid Toast in Receiver
                return
            }

            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in smsMessages) {
                val senderNum = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.displayMessageBody
                Log.d(TAG, "Received SMS from: $senderNum - Body: $messageBody")

                val forwardedMessage = "Fwd from $senderNum: $messageBody"
                try {
                    val smsManager = context.getSystemService(SmsManager::class.java)
                    smsManager.sendTextMessage(targetPhoneNumber, null, forwardedMessage, null, null)
                    Log.d(TAG, "SMS forwarded to $targetPhoneNumber")
                    // Toast.makeText(context, "SMS from $senderNum forwarded.", Toast.LENGTH_SHORT).show() // Avoid Toast in Receiver
                } catch (e: Exception) {
                    Log.e(TAG, "Error forwarding SMS: ${e.message}", e)
                    // Toast.makeText(context, "Error forwarding SMS: ${e.message}", Toast.LENGTH_LONG).show() // Avoid Toast in Receiver
                }
            }
        }
    }

    // This method now effectively gets the number that MainActivity has kept up-to-date
    // with Firebase Remote Config (by saving it to SharedPreferences).
    private fun loadTargetPhoneNumber(context: Context) {
        val prefs = context.getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
        targetPhoneNumber = prefs.getString("target_phone_number", null) // Default to null if not found
        if (targetPhoneNumber.isNullOrEmpty()) {
             Log.w(TAG, "Target phone number not found in SharedPreferences. User needs to set it up in the app, or Remote Config hasn't populated it yet.")
        } else {
             Log.i(TAG, "Loaded target phone number from SharedPreferences: $targetPhoneNumber")
        }
    }
}
