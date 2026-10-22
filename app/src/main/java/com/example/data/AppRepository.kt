package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // Common sensible apps to ensure high-fidelity testing even on bare emulators
    private val standardAppCatalog = listOf(
        AppInfo(
            packageName = "com.whatsapp",
            name = "WhatsApp",
            isSystemApp = false,
            isSensitive = true,
            category = "Social"
        ),
        AppInfo(
            packageName = "com.google.android.apps.photos",
            name = "Google Photos",
            isSystemApp = true,
            isSensitive = true,
            category = "Gallery"
        ),
        AppInfo(
            packageName = "com.instagram.android",
            name = "Instagram",
            isSystemApp = false,
            isSensitive = true,
            category = "Social"
        ),
        AppInfo(
            packageName = "com.android.settings",
            name = "Settings",
            isSystemApp = true,
            isSensitive = true,
            category = "System"
        ),
        AppInfo(
            packageName = "com.google.android.gm",
            name = "Gmail",
            isSystemApp = true,
            isSensitive = true,
            category = "Messaging"
        ),
        AppInfo(
            packageName = "com.android.chrome",
            name = "Chrome",
            isSystemApp = true,
            isSensitive = false,
            category = "Browser"
        ),
        AppInfo(
            packageName = "com.google.android.apps.messaging",
            name = "Messages",
            isSystemApp = true,
            isSensitive = true,
            category = "Messaging"
        ),
        AppInfo(
            packageName = "com.paypal.android.p2pmobile",
            name = "PayPal",
            isSystemApp = false,
            isSensitive = true,
            category = "Banking"
        ),
        AppInfo(
            packageName = "org.telegram.messenger",
            name = "Telegram",
            isSystemApp = false,
            isSensitive = true,
            category = "Social"
        ),
        AppInfo(
            packageName = "com.google.android.youtube",
            name = "YouTube",
            isSystemApp = false,
            isSensitive = false,
            category = "Media"
        ),
        AppInfo(
            packageName = "com.android.camera2",
            name = "Camera",
            isSystemApp = true,
            isSensitive = true,
            category = "Media"
        ),
        AppInfo(
            packageName = "com.google.android.keep",
            name = "Keep Notes",
            isSystemApp = false,
            isSensitive = true,
            category = "Personal"
        )
    )

    fun getInstalledAndCategorizedApps(lockedSet: Set<String>): List<AppInfo> {
        val appMap = mutableMapOf<String, AppInfo>()

        // 1. Add baseline catalog
        for (item in standardAppCatalog) {
            appMap[item.packageName] = item.copy(isLocked = lockedSet.contains(item.packageName))
        }

        // 2. Query real installed launcher apps from device PackageManager
        try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            for (ri in resolveInfos) {
                val pkgName = ri.activityInfo.packageName
                if (pkgName == context.packageName) continue // Skip this app locker itself

                val appName = ri.loadLabel(packageManager).toString()
                val isSys = try {
                    val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (_: Exception) {
                    false
                }

                val category = determineCategory(pkgName, appName, isSys)
                val isSensitive = isSensitiveCategory(category, pkgName)

                appMap[pkgName] = AppInfo(
                    packageName = pkgName,
                    name = appName,
                    isLocked = lockedSet.contains(pkgName),
                    isSystemApp = isSys,
                    isSensitive = isSensitive,
                    category = category
                )
            }
        } catch (_: Exception) {
            // Fallback gracefully to catalog
        }

        return appMap.values.sortedWith(
            compareByDescending<AppInfo> { it.isLocked }
                .thenByDescending { it.isSensitive }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun determineCategory(pkg: String, name: String, isSystem: Boolean): String {
        val lowerPkg = pkg.lowercase()
        val lowerName = name.lowercase()

        return when {
            lowerPkg.contains("bank") || lowerPkg.contains("pay") || lowerPkg.contains("wallet") ||
                    lowerName.contains("bank") || lowerName.contains("pay") -> "Banking"

            lowerPkg.contains("photo") || lowerPkg.contains("gallery") || lowerPkg.contains("camera") ||
                    lowerName.contains("photo") || lowerName.contains("gallery") -> "Gallery"

            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("signal") ||
                    lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
                    lowerPkg.contains("tiktok") || lowerPkg.contains("snapchat") || lowerPkg.contains("social") -> "Social"

            lowerPkg.contains("message") || lowerPkg.contains("sms") || lowerPkg.contains("chat") ||
                    lowerPkg.contains("mail") || lowerPkg.contains("gmail") -> "Messaging"

            lowerPkg.contains("chrome") || lowerPkg.contains("browser") || lowerPkg.contains("firefox") ||
                    lowerPkg.contains("opera") -> "Browser"

            lowerPkg.contains("settings") || isSystem -> "System"

            lowerPkg.contains("youtube") || lowerPkg.contains("music") || lowerPkg.contains("spotify") ||
                    lowerPkg.contains("media") || lowerPkg.contains("video") -> "Media"

            else -> "General"
        }
    }

    private fun isSensitiveCategory(category: String, pkg: String): Boolean {
        return category in listOf("Banking", "Gallery", "Social", "Messaging") ||
                pkg.contains("settings")
    }
}
