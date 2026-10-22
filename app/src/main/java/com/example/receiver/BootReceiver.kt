package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.SecurityPreferences
import com.example.service.AppLockService
import com.example.util.PermissionHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = SecurityPreferences.getInstance(context)
            if (prefs.isProtectionEnabled.value && PermissionHelper.hasUsageStatsPermission(context)) {
                AppLockService.start(context)
            }
        }
    }
}
