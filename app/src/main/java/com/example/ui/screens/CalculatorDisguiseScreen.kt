package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SecurityPreferences
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CalculatorDisguiseScreen(
    onVaultUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val securityPrefs = remember { SecurityPreferences.getInstance(context) }
    val masterPin by securityPrefs.masterPin.collectAsState()

    var displayText by remember { mutableStateOf("0") }
    var expressionHistory by remember { mutableStateOf("") }
    var pendingOperator by remember { mutableStateOf<String?>(null) }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var resetOnNextDigit by remember { mutableStateOf(false) }

    var showStealthGuideDialog by remember { mutableStateOf(false) }

    fun triggerVibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    fun onNumber(digit: String) {
        triggerVibrate()
        if (displayText == "0" || resetOnNextDigit) {
            displayText = digit
            resetOnNextDigit = false
        } else {
            displayText += digit
        }
    }

    fun onDecimal() {
        triggerVibrate()
        if (resetOnNextDigit) {
            displayText = "0."
            resetOnNextDigit = false
        } else if (!displayText.contains(".")) {
            displayText += "."
        }
    }

    fun onOperator(op: String) {
        triggerVibrate()
        val currentVal = displayText.toDoubleOrNull() ?: 0.0
        operand1 = currentVal
        pendingOperator = op
        expressionHistory = "$displayText $op"
        resetOnNextDigit = true
    }

    fun onEquals() {
        triggerVibrate()

        // SECRET UNLOCK CHECK: If the user typed their master PIN and pressed '=', unlock vault!
        if (displayText == masterPin || expressionHistory == masterPin) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (_: Exception) {}
            onVaultUnlocked()
            return
        }

        // Otherwise perform normal calculator math
        val op = pendingOperator
        val op1 = operand1
        val op2 = displayText.toDoubleOrNull()

        if (op != null && op1 != null && op2 != null) {
            val result = when (op) {
                "+" -> op1 + op2
                "−" -> op1 - op2
                "×" -> op1 * op2
                "÷" -> if (op2 != 0.0) op1 / op2 else Double.NaN
                else -> op2
            }
            expressionHistory = "$op1 $op $op2 ="
            displayText = if (result.isNaN()) {
                "Error"
            } else if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                String.format("%.4f", result).trimEnd('0').trimEnd('.')
            }
            operand1 = null
            pendingOperator = null
            resetOnNextDigit = true
        }
    }

    fun onClear() {
        triggerVibrate()
        displayText = "0"
        expressionHistory = ""
        operand1 = null
        pendingOperator = null
        resetOnNextDigit = false
    }

    fun onToggleSign() {
        triggerVibrate()
        if (displayText != "0" && displayText != "Error") {
            displayText = if (displayText.startsWith("-")) displayText.substring(1) else "-$displayText"
        }
    }

    fun onPercent() {
        triggerVibrate()
        val num = displayText.toDoubleOrNull() ?: return
        val res = num / 100.0
        displayText = if (res == res.toLong().toDouble()) res.toLong().toString() else res.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("calculator_disguise_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Subtle Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Calculator",
                color = TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showStealthGuideDialog = true
                            }
                        )
                    }
                    .testTag("calculator_stealth_title")
            )

            // Discreet button to help user switch to vault or view stealth instructions
            IconButton(
                onClick = { showStealthGuideDialog = true },
                modifier = Modifier.testTag("calculator_help_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Disguise Help",
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Display Screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expressionHistory,
                fontSize = 20.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayText,
                fontSize = if (displayText.length > 8) 38.sp else 54.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.testTag("calculator_display_text")
            )
        }

        // Calculator Buttons Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: AC, +/-, %, ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton(label = "AC", colorType = CalcBtnType.Function, modifier = Modifier.weight(1f)) { onClear() }
                CalcButton(label = "+/−", colorType = CalcBtnType.Function, modifier = Modifier.weight(1f)) { onToggleSign() }
                CalcButton(label = "%", colorType = CalcBtnType.Function, modifier = Modifier.weight(1f)) { onPercent() }
                CalcButton(label = "÷", colorType = CalcBtnType.Operator, modifier = Modifier.weight(1f)) { onOperator("÷") }
            }

            // Row 2: 7, 8, 9, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton(label = "7", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("7") }
                CalcButton(label = "8", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("8") }
                CalcButton(label = "9", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("9") }
                CalcButton(label = "×", colorType = CalcBtnType.Operator, modifier = Modifier.weight(1f)) { onOperator("×") }
            }

            // Row 3: 4, 5, 6, −
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton(label = "4", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("4") }
                CalcButton(label = "5", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("5") }
                CalcButton(label = "6", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("6") }
                CalcButton(label = "−", colorType = CalcBtnType.Operator, modifier = Modifier.weight(1f)) { onOperator("−") }
            }

            // Row 4: 1, 2, 3, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton(label = "1", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("1") }
                CalcButton(label = "2", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("2") }
                CalcButton(label = "3", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onNumber("3") }
                CalcButton(label = "+", colorType = CalcBtnType.Operator, modifier = Modifier.weight(1f)) { onOperator("+") }
            }

            // Row 5: 0 (double width), ., =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalcButton(label = "0", colorType = CalcBtnType.Number, modifier = Modifier.weight(2f)) { onNumber("0") }
                CalcButton(label = ".", colorType = CalcBtnType.Number, modifier = Modifier.weight(1f)) { onDecimal() }
                CalcButton(label = "=", colorType = CalcBtnType.Equals, modifier = Modifier.weight(1f)) { onEquals() }
            }
        }
    }

    // Stealth Mode Instructions / Emergency Unlock Dialog
    if (showStealthGuideDialog) {
        AlertDialog(
            onDismissRequest = { showStealthGuideDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = CyanPrimary
                )
            },
            title = { Text("Stealth Disguise Mode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This app is currently disguised as a standard Calculator so no one knows you have an App Locker.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "How to unlock:\nType your master PIN ($masterPin) on the keypad and press '='.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You can disable this disguise anytime inside Security Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStealthGuideDialog = false
                        onVaultUnlocked()
                    },
                    modifier = Modifier.testTag("bypass_calculator_to_vault_btn")
                ) {
                    Text("Enter App Locker")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStealthGuideDialog = false }) {
                    Text("Keep Disguise")
                }
            }
        )
    }
}

private enum class CalcBtnType {
    Number, Function, Operator, Equals
}

@Composable
private fun CalcButton(
    label: String,
    colorType: CalcBtnType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when (colorType) {
        CalcBtnType.Number -> DarkSurface
        CalcBtnType.Function -> DarkSurfaceVariant
        CalcBtnType.Operator -> IndigoAccent
        CalcBtnType.Equals -> CyanPrimary
    }

    val textColor = when (colorType) {
        CalcBtnType.Equals -> DarkBackground
        CalcBtnType.Operator -> CyanPrimary
        else -> TextPrimary
    }

    val hasBorder = colorType == CalcBtnType.Number

    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .then(
                if (hasBorder) Modifier.border(1.dp, DarkBorderSubtle, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable { onClick() }
            .testTag("calc_btn_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 26.sp,
            fontWeight = if (colorType == CalcBtnType.Equals) FontWeight.ExtraBold else FontWeight.Medium,
            color = textColor
        )
    }
}
