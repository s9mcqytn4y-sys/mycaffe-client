package com.azuredevid.cassy.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * BiometricSecurityManager provides hardware-backed biometric authentication (Class 3 Strong)
 * with graceful fallback to tenant owner PIN.
 */
class BiometricSecurityManager(private val context: Context) {

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptOwnerAuth(
        activity: FragmentActivity,
        title: String = "Otorisasi Sensitif Pemilik",
        subtitle: String = "Gunakan sidik jari atau pemindai biometrik terdaftar",
        onSuccess: () -> Unit,
        onFallbackPIN: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!canAuthenticate()) {
            onFallbackPIN()
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    onFallbackPIN()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Biometric mismatch (e.g. unregistered finger)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Gunakan PIN Pemilik")
            .build()

        prompt.authenticate(promptInfo)
    }
}
