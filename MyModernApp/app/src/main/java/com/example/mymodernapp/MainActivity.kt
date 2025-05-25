package com.example.mymodernapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.mymodernapp.ui.theme.MyModernAppTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val REMOTE_CONFIG_KEY_TARGET_PHONE = "target_sms_forward_number"

    private val smsPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS
    )

    private var allSmsPermissionsGranted by mutableStateOf(false)
    private var currentTargetPhoneNumber by mutableStateOf("")
    private var remoteConfigStatusMessage by mutableStateOf<String?>(null)
    private var showPermissionRationale by mutableStateOf(false)


    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            allSmsPermissionsGranted = permissions.entries.all { it.value }
            if (allSmsPermissionsGranted) {
                Log.d(TAG, "All SMS permissions granted.")
                Toast.makeText(this, "All SMS permissions granted!", Toast.LENGTH_SHORT).show()
                showPermissionRationale = false
            } else {
                Log.e(TAG, "Not all SMS permissions were granted.")
                Toast.makeText(this, "Some SMS permissions were denied. SMS forwarding may not work.", Toast.LENGTH_LONG).show()
                // Check if we should show rationale for any of the denied permissions
                // This logic checks if any permission was denied AND the system suggests not to show rationale (i.e., "Don't ask again" was selected)
                val permanentlyDenied = permissions.entries.any { (permission, granted) ->
                    !granted && !shouldShowRequestPermissionRationale(permission)
                }
                if (permanentlyDenied) {
                    showPermissionRationale = true // This will trigger UI to show "Open Settings" button
                } else {
                     showPermissionRationale = false
                }
                 permissions.entries.forEach { if (!it.value) Log.e(TAG, "${it.key} was denied.") }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestSmsPermissions() // Initial check
        setupRemoteConfig()

        currentTargetPhoneNumber = getPhoneNumberFromPrefs(this, "local_fallback") // Load initial with source

        setContent {
            MyModernAppTheme {
                LaunchedEffect(Unit) { // Re-fetch from prefs in case Remote Config updated it
                    currentTargetPhoneNumber = getPhoneNumberFromPrefs(this@MainActivity, "local_fallback")
                }

                SmsForwarderScreen(
                    allSmsPermissionsGranted = allSmsPermissionsGranted,
                    showPermissionRationale = showPermissionRationale,
                    onRequestPermissions = { checkAndRequestSmsPermissions() },
                    onOpenSettings = { openAppSettings() },
                    onSavePhoneNumber = { phoneNumber ->
                        savePhoneNumberToPrefs(this, phoneNumber, "local_user_set")
                    },
                    currentPhoneNumberDisplay = currentTargetPhoneNumber, // This now includes source
                    remoteConfigStatus = remoteConfigStatusMessage
                )
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun setupRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(REMOTE_CONFIG_KEY_TARGET_PHONE to "")) // Default to empty

        fetchRemoteConfigValues()
    }

    private fun fetchRemoteConfigValues() {
        remoteConfigStatusMessage = "Fetching remote config..."
        Firebase.remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d(TAG, "Remote Config params updated: $updated")
                    val remotePhoneNumber = Firebase.remoteConfig.getString(REMOTE_CONFIG_KEY_TARGET_PHONE)
                    if (remotePhoneNumber.isNotBlank()) {
                        Log.d(TAG, "Remote Config target phone number: $remotePhoneNumber")
                        savePhoneNumberToPrefs(this, remotePhoneNumber, "remote_config")
                        currentTargetPhoneNumber = "$remotePhoneNumber (Source: Remote)"
                        remoteConfigStatusMessage = "Remote config fetched: Using remote number."
                    } else {
                        Log.d(TAG, "Remote Config target phone number is blank. Using local fallback.")
                        currentTargetPhoneNumber = getPhoneNumberFromPrefs(this, "local_fallback_after_remote_blank")
                        remoteConfigStatusMessage = "Remote number not set. Using local fallback."
                    }
                } else {
                    Log.e(TAG, "Remote Config fetch failed", task.exception)
                    currentTargetPhoneNumber = getPhoneNumberFromPrefs(this, "local_fallback_after_remote_fail")
                    remoteConfigStatusMessage = "Remote config fetch failed. Using local fallback."
                    Toast.makeText(this, "Failed to fetch remote settings. Using local fallback.", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun checkAndRequestSmsPermissions() { // Made public for button access
        val permissionsToRequest = smsPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isEmpty()) {
            allSmsPermissionsGranted = true
            showPermissionRationale = false // Reset if permissions are now granted
            Log.d(TAG, "All SMS permissions already granted.")
        } else {
            allSmsPermissionsGranted = false // Explicitly set to false before requesting
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun savePhoneNumberToPrefs(context: Context, phoneNumber: String, source: String) {
        val prefs = context.getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("target_phone_number", phoneNumber)
            .putString("phone_number_source", source) // Store the source
            .apply()

        currentTargetPhoneNumber = "$phoneNumber (Source: ${source.replace("_", " ").capitalizeText()})" // Update UI state

        if (source == "local_user_set") {
            Toast.makeText(context, "Target phone number saved: $phoneNumber", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Target phone number saved to SharedPreferences by user: $phoneNumber")
        } else {
            Log.d(TAG, "Target phone number updated from $source and saved: $phoneNumber")
        }
    }

    private fun getPhoneNumberFromPrefs(context: Context, defaultSourceIfMissing: String): String {
        val prefs = context.getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
        val number = prefs.getString("target_phone_number", "") ?: ""
        val source = prefs.getString("phone_number_source", defaultSourceIfMissing) ?: defaultSourceIfMissing
        return if (number.isNotBlank()) "$number (Source: ${source.replace("_", " ").capitalizeText()})" else "Not set"
    }
}

// Helper extension function to capitalize the first letter of each word in a string, for display purposes.
fun String.capitalizeText(): String = split(" ").joinToString(" ") { word ->
    word.lowercase(Locale.getDefault()).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}.split("_").joinToString(" ") { word -> // Handle snake_case to Title Case
    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@Composable
fun SmsForwarderScreen(
    allSmsPermissionsGranted: Boolean,
    showPermissionRationale: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onSavePhoneNumber: (String) -> Unit,
    currentPhoneNumberDisplay: String, // Updated to be a display string
    remoteConfigStatus: String?
) {
    var targetPhoneNumberInput by remember { mutableStateOf("") } // Input field is separate
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Greeting("Android")
        Spacer(modifier = Modifier.height(16.dp))

        PermissionStatusText(allSmsPermissionsGranted, showPermissionRationale, onRequestPermissions, onOpenSettings)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Set Target Phone Number for SMS Forwarding:", fontWeight = FontWeight.Bold)
        Text("(Saved number is used if remote config is unavailable)", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = targetPhoneNumberInput,
            onValueChange = { targetPhoneNumberInput = it },
            label = { Text("Enter New Target Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("e.g., +11234567890") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (targetPhoneNumberInput.isNotBlank() && targetPhoneNumberInput.matches(Regex("^\\+?[0-9]{10,13}\$"))) {
                    onSavePhoneNumber(targetPhoneNumberInput)
                    targetPhoneNumberInput = "" // Clear input field after save
                } else {
                    Toast.makeText(context, "Please enter a valid phone number (e.g., +11234567890).", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Phone Number (Local Fallback)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Current active number: $currentPhoneNumberDisplay",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        remoteConfigStatus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
fun PermissionStatusText(
    allSmsPermissionsGranted: Boolean,
    showPermissionRationale: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (allSmsPermissionsGranted) "SMS Permissions: GRANTED" else "SMS Permissions: DENIED",
            color = if (allSmsPermissionsGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        if (!allSmsPermissionsGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Full SMS forwarding functionality requires all SMS permissions (Receive, Read, Send).")
            Spacer(modifier = Modifier.height(4.dp)) 
            if (showPermissionRationale) {
                 Text("Some permissions were permanently denied. You need to enable them in app settings to proceed.", fontStyle = FontStyle.Italic)
                Button(onClick = onOpenSettings) {
                    Text("Open App Settings")
                }
            } else {
                Button(onClick = onRequestPermissions) {
                    Text("Request SMS Permissions Again")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! SMS Forwarder App.",
        modifier = modifier
    )
}


@Preview(showBackground = true)
@Composable
fun DefaultPreviewWithPermissions() {
    MyModernAppTheme {
        SmsForwarderScreen(
            allSmsPermissionsGranted = true,
            showPermissionRationale = false,
            onRequestPermissions = {},
            onOpenSettings = {},
            onSavePhoneNumber = {},
            currentPhoneNumberDisplay = "+1234567890 (Source: Remote)",
            remoteConfigStatus = "Remote config fetched successfully."
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewPermissionsDenied() {
    MyModernAppTheme {
        SmsForwarderScreen(
            allSmsPermissionsGranted = false,
            showPermissionRationale = false,
            onRequestPermissions = {},
            onOpenSettings = {},
            onSavePhoneNumber = {},
            currentPhoneNumberDisplay = "Not Set",
            remoteConfigStatus = "Fetching remote config..."
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewPermissionsPermanentlyDenied() {
    MyModernAppTheme {
        SmsForwarderScreen(
            allSmsPermissionsGranted = false,
            showPermissionRationale = true, // This is the key for this preview
            onRequestPermissions = {},
            onOpenSettings = {},
            onSavePhoneNumber = {},
            currentPhoneNumberDisplay = "Not Set",
            remoteConfigStatus = "Fetch failed. Using local."
        )
    }
}
