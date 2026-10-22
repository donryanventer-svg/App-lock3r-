package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricCapability {
    READY,
    NOT_ENROLLED,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    SECURITY_UPDATE_REQUIRED,
    UNKNOWN
}

object BiometricHelper {

    private const val AUTHENTICATORS =
        Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK

    /**
     * Checks whether fingerprint or face authentication is available and enrolled on the device.
     */
    fun checkBiometricCapability(context: Context): BiometricCapability {
        return try {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.READY
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.HARDWARE_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.SECURITY_UPDATE_REQUIRED
                else -> BiometricCapability.UNKNOWN
            }
        } catch (e: Exception) {
            BiometricCapability.UNKNOWN
        }
    }

    /**
     * Checks if biometrics can be prompted (ready or can authenticate).
     */
    fun isBiometricReady(context: Context): Boolean {
        return checkBiometricCapability(context) == BiometricCapability.READY
    }

    /**
     * Finds the nearest FragmentActivity in the Context hierarchy.
     */
    fun findFragmentActivity(context: Context): FragmentActivity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is FragmentActivity) {
                return current
            }
            current = current.baseContext
        }
        return null
    }

    /**
     * Launches the system BiometricPrompt (Fingerprint or Face unlock).
     */
    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Biometric Unlock",
        subtitle: String = "Confirm fingerprint or face to bypass passcode",
        negativeButtonText: String = "Use PIN Passcode",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Opens Android System settings for biometric / fingerprint enrollment.
     */
    fun openBiometricEnrollment(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(enrollIntent)
            } else {
                val securityIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(securityIntent)
            }
        } catch (e: Exception) {
            try {
                val securityIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(securityIntent)
            } catch (_: Exception) {}
        }
    }
}
