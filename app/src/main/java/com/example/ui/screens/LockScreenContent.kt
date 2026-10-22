package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SecurityPreferences
import com.example.util.BiometricCapability
import com.example.util.BiometricHelper
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreenContent(
    packageName: String,
    appName: String,
    onUnlocked: () -> Unit,
    onExitToHome: () -> Unit,
    isSimulation: Boolean = false
) {
    val context = LocalContext.current
    val securityPrefs = remember { SecurityPreferences.getInstance(context) }
    val masterPin by securityPrefs.masterPin.collectAsState()
    val isFakeCrashEnabled by securityPrefs.isFakeCrashEnabled.collectAsState()
    val isScrambleKeypad by securityPrefs.isScrambleKeypad.collectAsState()
    val isBiometricEnabled by securityPrefs.isBiometricEnabled.collectAsState()
    val securityQuestion by securityPrefs.securityQuestion.collectAsState()
    val securityAnswer by securityPrefs.securityAnswer.collectAsState()

    var showFakeCrash by remember { mutableStateOf(isFakeCrashEnabled) }
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var showBiometricInfoDialog by remember { mutableStateOf(false) }
    var hasAutoPromptedBiometric by remember { mutableStateOf(false) }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var showSecretHintToast by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerVibrate(longVibrate: Boolean = false) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (longVibrate) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    fun launchBiometricPrompt(isManualTap: Boolean = false) {
        if (!isBiometricEnabled) return
        val activity = BiometricHelper.findFragmentActivity(context)
        val capability = BiometricHelper.checkBiometricCapability(context)

        if (activity != null && capability == BiometricCapability.READY) {
            BiometricHelper.promptBiometric(
                activity = activity,
                title = "Unlock $appName",
                subtitle = "Touch fingerprint sensor or verify face",
                negativeButtonText = "Use Passcode",
                onSuccess = {
                    triggerVibrate()
                    securityPrefs.markAppUnlocked(packageName)
                    onUnlocked()
                },
                onError = { errorCode, errString ->
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        if (isManualTap) {
                            Toast.makeText(context, errString.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onFailed = {
                    triggerVibrate(true)
                }
            )
        } else if (isManualTap) {
            showBiometricInfoDialog = true
        }
    }

    // Auto-prompt biometric authentication if enabled when real lock screen is presented
    LaunchedEffect(showFakeCrash, isBiometricEnabled) {
        if (!showFakeCrash && isBiometricEnabled && !hasAutoPromptedBiometric) {
            hasAutoPromptedBiometric = true
            launchBiometricPrompt(isManualTap = false)
        }
    }

    // Scramble keypad numbers if enabled
    val keypadNumbers = remember(isScrambleKeypad) {
        if (isScrambleKeypad) {
            (0..9).shuffled()
        } else {
            (1..9).toList() + listOf(0)
        }
    }

    fun handleDigit(digit: Int) {
        if (enteredPin.length < 6) {
            triggerVibrate()
            val newPin = enteredPin + digit.toString()
            enteredPin = newPin

            // Check if PIN matches
            if (newPin.length == masterPin.length) {
                if (newPin == masterPin) {
                    securityPrefs.markAppUnlocked(packageName)
                    onUnlocked()
                } else {
                    // Wrong PIN
                    triggerVibrate(true)
                    isError = true
                    securityPrefs.addIntruderLog(
                        appName = appName,
                        packageName = packageName,
                        fakeCrashTriggered = false
                    )
                    scope.launch {
                        shakeOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 350
                                0f at 0
                                -20f at 50
                                20f at 100
                                -15f at 150
                                15f at 200
                                -8f at 250
                                8f at 300
                                0f at 350
                            }
                        )
                        enteredPin = ""
                        isError = false
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            triggerVibrate()
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (showFakeCrash) {
            // Realistic Android System Crash Dialog: Makes the locked app appear crashed / disappeared
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .testTag("fake_crash_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonAlert.copy(alpha = 0.2f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                triggerVibrate(true)
                                                showFakeCrash = false
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Crash Icon",
                                    tint = CrimsonAlert,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Unfortunately, $appName has stopped.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary.copy(alpha = 0.9f),
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "An unrecoverable error occurred in package $packageName. The application process was terminated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    securityPrefs.addIntruderLog(appName, packageName, fakeCrashTriggered = true)
                                    onExitToHome()
                                },
                                modifier = Modifier.testTag("fake_crash_report_btn")
                            ) {
                                Text("Report", color = TextSecondary)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Secret Gesture: Long pressing OK reveals real PIN keypad!
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF334155))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                // Intruder single tap closes the app
                                                securityPrefs.addIntruderLog(appName, packageName, fakeCrashTriggered = true)
                                                onExitToHome()
                                            },
                                            onLongPress = {
                                                // Owner long press reveals PIN unlock!
                                                triggerVibrate(true)
                                                showFakeCrash = false
                                            }
                                        )
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .testTag("fake_crash_ok_btn")
                            ) {
                                Text(
                                    text = "OK",
                                    color = CyanPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stealth helper indicator for owner
                        Text(
                            text = "Disguise active • Long-press OK or double-tap icon to reveal PIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Real PIN Lock Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(IndigoAccent)
                            .border(1.dp, DarkBorderSubtle, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = CyanPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enter master passcode to unlock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // PIN Dots Indicator with shake animation
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until masterPin.length) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isError -> CrimsonAlert
                                            isFilled -> CyanPrimary
                                            else -> DarkSurfaceVariant
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isError) CrimsonAlert else if (isFilled) CyanPrimary else DarkBorder,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    if (isError) {
                        Text(
                            text = "Incorrect passcode. Attempt logged.",
                            color = CrimsonAlert,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (isBiometricEnabled) {
                        Surface(
                            onClick = {
                                triggerVibrate()
                                launchBiometricPrompt(isManualTap = true)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .testTag("biometric_quick_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Unlock with Fingerprint or Face",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Keypad Matrix
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val rows = listOf(
                        keypadNumbers.subList(0, 3),
                        keypadNumbers.subList(3, 6),
                        keypadNumbers.subList(6, 9)
                    )

                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (num in row) {
                                KeypadButton(
                                    label = num.toString(),
                                    onClick = { handleDigit(num) }
                                )
                            }
                        }
                    }

                    // Bottom Row: [Biometric / Forgot], [0 or last number], [Backspace]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left special button: Biometric unlock or Forgot PIN
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (isBiometricEnabled) DarkSurface else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isBiometricEnabled) DarkBorder else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    if (isBiometricEnabled) {
                                        triggerVibrate()
                                        launchBiometricPrompt(isManualTap = true)
                                    } else {
                                        showForgotPinDialog = true
                                    }
                                }
                                .testTag("keypad_biometric_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBiometricEnabled) Icons.Default.Fingerprint else Icons.Default.HelpOutline,
                                contentDescription = if (isBiometricEnabled) "Biometric Unlock" else "Forgot PIN",
                                tint = if (isBiometricEnabled) EmeraldSuccess else TextMuted,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Last digit (0)
                        val lastDigit = keypadNumbers[9]
                        KeypadButton(
                            label = lastDigit.toString(),
                            onClick = { handleDigit(lastDigit) }
                        )

                        // Backspace button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable { handleBackspace() }
                                .testTag("keypad_backspace_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onExitToHome() }
                    ) {
                        Text("Exit", color = TextMuted)
                    }

                    if (isFakeCrashEnabled && !showFakeCrash) {
                        TextButton(
                            onClick = { showFakeCrash = true }
                        ) {
                            Text("Show Disguise", color = CyanPrimary.copy(alpha = 0.8f))
                        }
                    }

                    TextButton(
                        onClick = { showForgotPinDialog = true }
                    ) {
                        Text("Forgot PIN?", color = TextSecondary)
                    }
                }
            }
        }

        // Forgot PIN / Recovery Dialog
        if (showForgotPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showForgotPinDialog = false
                    recoveryError = null
                },
                title = { Text("Passcode Recovery") },
                text = {
                    Column {
                        Text(
                            text = "Security Question:\n$securityQuestion",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = recoveryAnswerInput,
                            onValueChange = { recoveryAnswerInput = it },
                            label = { Text("Your Answer") },
                            isError = recoveryError != null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (recoveryError != null) {
                            Text(
                                text = recoveryError!!,
                                color = CrimsonAlert,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (recoveryAnswerInput.trim().equals(securityAnswer.trim(), ignoreCase = true)) {
                                showForgotPinDialog = false
                                recoveryError = null
                                securityPrefs.markAppUnlocked(packageName)
                                onUnlocked()
                            } else {
                                recoveryError = "Incorrect answer. Please try again."
                            }
                        }
                    ) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPinDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Biometric Details / Simulation / Setup Dialog
        if (showBiometricInfoDialog) {
            val capability = BiometricHelper.checkBiometricCapability(context)
            AlertDialog(
                onDismissRequest = { showBiometricInfoDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Biometric Authentication",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = when (capability) {
                                BiometricCapability.NOT_ENROLLED ->
                                    "No fingerprint or face is currently enrolled on this device. You can set up biometrics in Android Settings, or quickly simulate a successful biometric bypass to test."
                                BiometricCapability.NO_HARDWARE ->
                                    "Biometric hardware is not detected on this device. You can use your master PIN passcode, or test biometric bypass below."
                                BiometricCapability.HARDWARE_UNAVAILABLE ->
                                    "Biometric sensor is temporarily busy or unavailable. Please use your master PIN passcode or test below."
                                else ->
                                    "Fingerprint and Face Unlock are active. Touch the fingerprint sensor or look at the camera to bypass password entry."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        if (capability == BiometricCapability.NOT_ENROLLED) {
                            Button(
                                onClick = {
                                    showBiometricInfoDialog = false
                                    BiometricHelper.openBiometricEnrollment(context)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_biometric_settings_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                            ) {
                                Text("Open Android Biometric Settings", color = CyanPrimary)
                            }
                        }

                        Button(
                            onClick = {
                                showBiometricInfoDialog = false
                                triggerVibrate()
                                securityPrefs.markAppUnlocked(packageName)
                                Toast.makeText(context, "Biometric verified successfully", Toast.LENGTH_SHORT).show()
                                onUnlocked()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("simulate_biometric_success_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Text("Simulate Biometric Unlock", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showBiometricInfoDialog = false }) {
                        Text("Use Passcode", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(DarkSurface)
            .border(1.dp, DarkBorder, CircleShape)
            .clickable { onClick() }
            .testTag("keypad_digit_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.SansSerif
        )
    }
}
