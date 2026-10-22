package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_lock_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MASTER_PIN = "master_pin"
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_LOCKED_PACKAGES = "locked_packages"
        private const val KEY_DISGUISE_CALCULATOR = "disguise_calculator"
        private const val KEY_DISGUISE_THEME = "disguise_theme"
        private const val KEY_FAKE_CRASH_ENABLED = "fake_crash_enabled"
        private const val KEY_FAKE_CRASH_GESTURE = "fake_crash_gesture"
        private const val KEY_SCRAMBLE_KEYPAD = "scramble_keypad"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_INTRUDER_LOGS = "intruder_logs"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER = "security_answer"
        private const val KEY_RELOCK_DELAY = "relock_delay"

        @Volatile
        private var INSTANCE: SecurityPreferences? = null

        fun getInstance(context: Context): SecurityPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _masterPin = MutableStateFlow(prefs.getString(KEY_MASTER_PIN, "1234") ?: "1234")
    val masterPin: StateFlow<String> = _masterPin.asStateFlow()

    private val _isProtectionEnabled = MutableStateFlow(prefs.getBoolean(KEY_PROTECTION_ENABLED, true))
    val isProtectionEnabled: StateFlow<Boolean> = _isProtectionEnabled.asStateFlow()

    private val _lockedPackages = MutableStateFlow(
        prefs.getStringSet(KEY_LOCKED_PACKAGES, setOf(
            "com.android.settings",
            "com.google.android.apps.photos",
            "com.whatsapp",
            "com.instagram.android"
        ))?.toSet() ?: emptySet()
    )
    val lockedPackages: StateFlow<Set<String>> = _lockedPackages.asStateFlow()

    private val _isCalculatorDisguiseEnabled = MutableStateFlow(prefs.getBoolean(KEY_DISGUISE_CALCULATOR, false))
    val isCalculatorDisguiseEnabled: StateFlow<Boolean> = _isCalculatorDisguiseEnabled.asStateFlow()

    private val _disguiseTheme = MutableStateFlow(prefs.getString(KEY_DISGUISE_THEME, "Calculator") ?: "Calculator")
    val disguiseTheme: StateFlow<String> = _disguiseTheme.asStateFlow()

    private val _isFakeCrashEnabled = MutableStateFlow(prefs.getBoolean(KEY_FAKE_CRASH_ENABLED, true))
    val isFakeCrashEnabled: StateFlow<Boolean> = _isFakeCrashEnabled.asStateFlow()

    private val _fakeCrashGesture = MutableStateFlow(prefs.getString(KEY_FAKE_CRASH_GESTURE, "long_press_ok") ?: "long_press_ok")
    val fakeCrashGesture: StateFlow<String> = _fakeCrashGesture.asStateFlow()

    private val _isScrambleKeypad = MutableStateFlow(prefs.getBoolean(KEY_SCRAMRAMBLE(), false))
    val isScrambleKeypad: StateFlow<Boolean> = _isScrambleKeypad.asStateFlow()

    private fun KEY_SCRAMRAMBLE() = KEY_SCRAMBLE_KEYPAD

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _intruderLogs = MutableStateFlow(loadIntruderLogs())
    val intruderLogs: StateFlow<List<IntruderLog>> = _intruderLogs.asStateFlow()

    private val _securityQuestion = MutableStateFlow(prefs.getString(KEY_SECURITY_QUESTION, "What is your favorite color?") ?: "What is your favorite color?")
    val securityQuestion: StateFlow<String> = _securityQuestion.asStateFlow()

    private val _securityAnswer = MutableStateFlow(prefs.getString(KEY_SECURITY_ANSWER, "blue") ?: "blue")
    val securityAnswer: StateFlow<String> = _securityAnswer.asStateFlow()

    // Temporary in-memory unlocked packages timestamp map
    private val sessionUnlockedPackages = mutableMapOf<String, Long>()

    fun setMasterPin(newPin: String) {
        prefs.edit().putString(KEY_MASTER_PIN, newPin).apply()
        _masterPin.value = newPin
    }

    fun setProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
        _isProtectionEnabled.value = enabled
    }

    fun toggleAppLock(packageName: String): Boolean {
        val current = _lockedPackages.value.toMutableSet()
        val willBeLocked: Boolean
        if (current.contains(packageName)) {
            current.remove(packageName)
            willBeLocked = false
        } else {
            current.add(packageName)
            willBeLocked = true
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedPackages.value = current
        return willBeLocked
    }

    fun setPackageLocked(packageName: String, locked: Boolean) {
        val current = _lockedPackages.value.toMutableSet()
        if (locked) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedPackages.value = current
    }

    fun lockAll(packageNames: List<String>) {
        val current = _lockedPackages.value.toMutableSet()
        current.addAll(packageNames)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedPackages.value = current
    }

    fun unlockAll() {
        val empty = emptySet<String>()
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, empty).apply()
        _lockedPackages.value = empty
    }

    fun isAppLocked(packageName: String): Boolean {
        if (!_isProtectionEnabled.value) return false
        if (!_lockedPackages.value.contains(packageName)) return false

        // Check session unlock
        val lastUnlocked = sessionUnlockedPackages[packageName] ?: 0L
        val now = System.currentTimeMillis()
        val timeoutMs = 60_000L // 1 minute grace period or configurable
        if (now - lastUnlocked < timeoutMs) {
            return false
        }
        return true
    }

    fun markAppUnlocked(packageName: String) {
        sessionUnlockedPackages[packageName] = System.currentTimeMillis()
    }

    fun setCalculatorDisguise(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISGUISE_CALCULATOR, enabled).apply()
        _isCalculatorDisguiseEnabled.value = enabled
    }

    fun setDisguiseTheme(theme: String) {
        prefs.edit().putString(KEY_DISGUISE_THEME, theme).apply()
        _disguiseTheme.value = theme
    }

    fun setFakeCrashEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FAKE_CRASH_ENABLED, enabled).apply()
        _isFakeCrashEnabled.value = enabled
    }

    fun setFakeCrashGesture(gesture: String) {
        prefs.edit().putString(KEY_FAKE_CRASH_GESTURE, gesture).apply()
        _fakeCrashGesture.value = gesture
    }

    fun setScrambleKeypad(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCRAMBLE_KEYPAD, enabled).apply()
        _isScrambleKeypad.value = enabled
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setSecurityQuestion(question: String, answer: String) {
        prefs.edit()
            .putString(KEY_SECURITY_QUESTION, question)
            .putString(KEY_SECURITY_ANSWER, answer)
            .apply()
        _securityQuestion.value = question
        _securityAnswer.value = answer
    }

    fun addIntruderLog(appName: String, packageName: String, fakeCrashTriggered: Boolean) {
        val currentLogs = _intruderLogs.value.toMutableList()
        val newLog = IntruderLog(
            appName = appName,
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            fakeCrashTriggered = fakeCrashTriggered
        )
        currentLogs.add(0, newLog)
        // Keep max 50 logs
        val trimmed = if (currentLogs.size > 50) currentLogs.subList(0, 50) else currentLogs
        saveIntruderLogs(trimmed)
        _intruderLogs.value = trimmed
    }

    fun clearIntruderLogs() {
        saveIntruderLogs(emptyList())
        _intruderLogs.value = emptyList()
    }

    private fun loadIntruderLogs(): List<IntruderLog> {
        val jsonStr = prefs.getString(KEY_INTRUDER_LOGS, null) ?: return emptyList()
        val list = mutableListOf<IntruderLog>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    IntruderLog(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        appName = obj.optString("appName", "Unknown"),
                        packageName = obj.optString("packageName", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        attempts = obj.optInt("attempts", 1),
                        fakeCrashTriggered = obj.optBoolean("fakeCrashTriggered", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveIntruderLogs(logs: List<IntruderLog>) {
        val arr = JSONArray()
        for (log in logs) {
            val obj = JSONObject().apply {
                put("id", log.id)
                put("appName", log.appName)
                put("packageName", log.packageName)
                put("timestamp", log.timestamp)
                put("attempts", log.attempts)
                put("fakeCrashTriggered", log.fakeCrashTriggered)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_INTRUDER_LOGS, arr.toString()).apply()
    }
}
