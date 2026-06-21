package com.example

import android.os.Bundle
import android.widget.Toast
import android.Manifest
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ui.SmsDashboardMain
import com.example.ui.SmsViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
  
  private val smsViewModel: SmsViewModel by viewModels()
  private val isAuthenticated = mutableStateOf(false)
  
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val allGranted = permissions.entries.all { it.value }
    if (!allGranted) {
      Toast.makeText(this, "SMS Permissions are required for the app to function properly", Toast.LENGTH_LONG).show()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val prefs = getSharedPreferences("sms_backup_prefs", android.content.Context.MODE_PRIVATE)
    val isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)

    if (isBiometricEnabled) {
        authenticate()
    } else {
        isAuthenticated.value = true
    }

    setContent {
      MyApplicationTheme {
        if (isAuthenticated.value) {
          SmsDashboardMain(
            viewModel = smsViewModel,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          LockedScreen { authenticate() }
        }
      }
    }
  }

  private fun authenticate() {
    val executor = ContextCompat.getMainExecutor(this)
    val biometricPrompt = BiometricPrompt(this, executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          isAuthenticated.value = true
        }

        override fun onAuthenticationFailed() {
          super.onAuthenticationFailed()
          Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
        }
      })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Unlock Safe Backup")
      .setSubtitle("Authenticate to access your forwarding rules")
      .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
      .build()

    val biometricManager = BiometricManager.from(this)
    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
      BiometricManager.BIOMETRIC_SUCCESS -> {
        biometricPrompt.authenticate(promptInfo)
      }
      else -> {
        // Fallback to unlocked if the device has no secure lock screen configured
        isAuthenticated.value = true
      }
    }
  }

  override fun onResume() {
      super.onResume()
      lifecycleScope.launch {
          com.example.receiver.SyncEngine.runCatchUpSync(this@MainActivity)
      }
      val prefs = getSharedPreferences("sms_backup_prefs", android.content.Context.MODE_PRIVATE)
      val isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
      if (isBiometricEnabled && !isAuthenticated.value) {
          authenticate()
      }
  }

  override fun onStop() {
      super.onStop()
      val prefs = getSharedPreferences("sms_backup_prefs", android.content.Context.MODE_PRIVATE)
      if (prefs.getBoolean("biometric_enabled", false)) {
          isAuthenticated.value = false
      }
  }
}

@Composable
fun LockedScreen(onUnlockClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("App is locked", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please authenticate to view your dashboard", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onUnlockClick) {
                Text("Tap to Unlock")
            }
        }
    }
}
