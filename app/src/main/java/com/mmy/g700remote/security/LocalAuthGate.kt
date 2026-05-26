package com.mmy.g700remote.security

import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class LocalAuthGate {
    private val executor: Executor = Executors.newSingleThreadExecutor()

    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = activity.getSystemService(BiometricManager::class.java)
        val canAuthenticate = manager?.canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Biometric or device credential authentication is not available")
            return
        }

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    activity.runOnUiThread { onSuccess() }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    activity.runOnUiThread { onError(errString?.toString() ?: "Authentication cancelled") }
                }

                override fun onAuthenticationFailed() {
                    activity.runOnUiThread { onError("Authentication failed") }
                }
            },
        )
    }
}
