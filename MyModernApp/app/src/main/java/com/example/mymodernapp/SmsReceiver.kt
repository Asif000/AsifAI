package com.example.mymodernapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage as TelephonySmsMessage // Renamed to avoid conflict
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver_Debug" // Changed tag for focused debugging
    private var targetPhoneNumber: String? = null

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "--------------------------------------------------------------")
        Log.d(TAG, "New SMS Intent Received. Action: ${intent.action}")

        // Log Intent details
        Log.d(TAG, "Intent Details - Action: ${intent.action}, Type: ${intent.type}, Scheme: ${intent.scheme}, Flags: ${intent.flags}")
        val extras = intent.extras
        if (extras != null) {
            Log.d(TAG, "Intent Extras Bundle:")
            for (key in extras.keySet()) {
                val value = extras.get(key)
                Log.d(TAG, "  Key: $key, Value: $value, ValueType: ${value?.javaClass?.name ?: "null"}")
                if (key == "pdus" && value is Array<*>) {
                    value.forEachIndexed { index, pdu ->
                        Log.d(TAG, "    PDU $index: $pdu (Type: ${pdu?.javaClass?.name})")
                    }
                }
            }
        } else {
            Log.d(TAG, "Intent Extras Bundle is null.")
        }

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action ||
            "android.provider.Telephony.SMS_DELIVER" == intent.action) { // SMS_DELIVER is another action for received SMS

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "SEND_SMS permission not granted. Cannot forward SMS.")
                return
            }

            loadTargetPhoneNumber(context)

            if (targetPhoneNumber.isNullOrEmpty()) {
                Log.e(TAG, "Target phone number is not set (checked SharedPreferences). Cannot forward SMS.")
                return
            }

            val messages: Array<TelephonySmsMessage?> = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d(TAG, "Number of SmsMessage objects extracted: ${messages.size}")

            if (messages.isEmpty()) {
                Log.w(TAG, "No SMS messages extracted from intent. PDU parsing might have failed or intent was empty.")
                // Attempt to extract PDUs manually if getMessagesFromIntent fails
                val pdus = extras?.get("pdus") as? Array<Any>
                val format = extras?.getString("format")
                if (pdus != null && format != null) {
                    Log.d(TAG, "Attempting manual PDU parsing. Found ${pdus.size} PDUs with format $format.")
                    for (pdu in pdus) {
                        try {
                            val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                TelephonySmsMessage.createFromPdu(pdu as ByteArray, format)
                            } else {
                                TelephonySmsMessage.createFromPdu(pdu as ByteArray)
                            }
                            logSmsMessageDetails(smsMessage, "Manually Parsed")
                            // (Consider adding forwarding logic here too if manual parsing is the only way)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error manually parsing PDU: $pdu", e)
                        }
                    }
                } else {
                     Log.w(TAG, "PDUs or format extra not found for manual parsing.")
                }
            }

            for ((index, smsMessage) in messages.withIndex()) {
                if (smsMessage == null) {
                    Log.w(TAG, "SmsMessage object at index $index is null.")
                    continue
                }
                logSmsMessageDetails(smsMessage, "Message $index")

                // Forwarding logic (existing)
                val senderNum = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.displayMessageBody // or messageBody for raw data
                
                if (senderNum.isNullOrEmpty() && messageBody.isNullOrEmpty()) {
                    Log.w(TAG, "Both senderNum and messageBody are null or empty for Message $index. Skipping forwarding for this message part.")
                    continue
                }
                Log.d(TAG, "Message $index - Extracted Sender: $senderNum, Body: $messageBody")


                val forwardedMessage = "Fwd from $senderNum: $messageBody"
                try {
                    val smsManager = context.getSystemService(SmsManager::class.java)
                    smsManager.sendTextMessage(targetPhoneNumber, null, forwardedMessage, null, null)
                    Log.i(TAG, "SMS from $senderNum forwarded to $targetPhoneNumber")
                } catch (e: Exception) {
                    Log.e(TAG, "Error forwarding SMS from $senderNum: ${e.message}", e)
                }
            }
        } else {
            Log.w(TAG, "Received intent with unhandled action: ${intent.action}")
        }
        Log.d(TAG, "--------------------------------------------------------------")
    }

    private fun logSmsMessageDetails(smsMessage: TelephonySmsMessage, messageLabel: String) {
        Log.d(TAG, "Details for $messageLabel:")
        try {
            Log.d(TAG, "  Display Originating Address: ${smsMessage.displayOriginatingAddress}")
            Log.d(TAG, "  Originating Address: ${smsMessage.originatingAddress}")
            Log.d(TAG, "  Display Message Body: ${smsMessage.displayMessageBody}")
            Log.d(TAG, "  Message Body (raw): ${smsMessage.messageBody}")
            Log.d(TAG, "  Service Center Address: ${smsMessage.serviceCenterAddress}")
            Log.d(TAG, "  Timestamp Millis: ${smsMessage.timestampMillis}")
            Log.d(TAG, "  Protocol Identifier (TP-PID): ${smsMessage.protocolIdentifier}")
            Log.d(TAG, "  Status on ICC: ${smsMessage.statusOnIcc}")
            Log.d(TAG, "  Status: ${smsMessage.status}")
            Log.d(TAG, "  Is Email: ${smsMessage.isEmail}")
            Log.d(TAG, "  Is Reply Path Present: ${smsMessage.isReplyPathPresent}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28+
                Log.d(TAG, "  Message Class: ${smsMessage.messageClass}")
            }
            // Log more fields if necessary, e.g., userData, pdu
            // Log.d(TAG, "  PDU: ${smsMessage.pdu?.joinToString(",")}")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging details for $messageLabel", e)
        }
    }

    private fun loadTargetPhoneNumber(context: Context) {
        val prefs = context.getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
        targetPhoneNumber = prefs.getString("target_phone_number", null)
        if (targetPhoneNumber.isNullOrEmpty()) {
            Log.w(TAG, "Target phone number not found in SharedPreferences during loadTargetPhoneNumber.")
        } else {
            Log.i(TAG, "Loaded target phone number from SharedPreferences: $targetPhoneNumber")
        }
    }
}
