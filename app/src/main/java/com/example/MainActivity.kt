package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SecurityPreferences
import com.example.service.AppLockService
import com.example.ui.screens.AppLockerHomeScreen
import com.example.ui.screens.CalculatorDisguiseScreen
import com.example.ui.screens.IntruderLogsScreen
import com.example.ui.screens.LockScreenContent
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.screens.StealthDisguiseScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.PurpleLightContainer
import com.example.ui.theme.PurpleOnContainer
import com.example.ui.theme.IndigoAccent
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PermissionHelper

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Auto-start protection service if enabled and permissions granted
        val prefs = SecurityPreferences.getInstance(this)
        if (prefs.isProtectionEnabled.value && PermissionHelper.hasUsageStatsPermission(this)) {
            AppLockService.start(this)
        }

        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

sealed class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    object Apps : NavTab("Apps", Icons.Default.Apps, "nav_tab_apps")
    object Stealth : NavTab("Stealth", Icons.Default.VisibilityOff, "nav_tab_stealth")
    object Logs : NavTab("Alerts", Icons.Default.Warning, "nav_tab_logs")
    object Settings : NavTab("Security", Icons.Default.Settings, "nav_tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val securityPrefs = remember { SecurityPreferences.getInstance(context) }
    val isCalculatorDisguiseEnabled by securityPrefs.isCalculatorDisguiseEnabled.collectAsState()
    val disguiseTheme by securityPrefs.disguiseTheme.collectAsState()

    var isVaultUnlockedInSession by remember { mutableStateOf(!isCalculatorDisguiseEnabled) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Simulation states
    var simulatingLockTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isTestingCalculatorPreview by remember { mutableStateOf(false) }

    val tabs = listOf(NavTab.Apps, NavTab.Stealth, NavTab.Logs, NavTab.Settings)

    // Check if we should display the stealth Calculator Disguise
    if ((isCalculatorDisguiseEnabled && !isVaultUnlockedInSession) || isTestingCalculatorPreview) {
        CalculatorDisguiseScreen(
            onVaultUnlocked = {
                isVaultUnlockedInSession = true
                isTestingCalculatorPreview = false
            }
        )
        return
    }

    // Check if simulating a lock screen
    simulatingLockTarget?.let { (pkg, name) ->
        BackHandler {
            simulatingLockTarget = null
        }
        LockScreenContent(
            packageName = pkg,
            appName = name,
            isSimulation = true,
            onUnlocked = {
                simulatingLockTarget = null
            },
            onExitToHome = {
                simulatingLockTarget = null
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (selectedTab) {
                                0 -> "Cloak"
                                1 -> "Disguise & Stealth"
                                2 -> "Security Logs"
                                else -> "Settings"
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "SYSTEM SECURED",
                            color = CyanPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle))
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkBackground,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, DarkBorder))
                    .testTag("bottom_nav_bar"),
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.testTag("${tab.tag}_icon")
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleOnContainer,
                            selectedTextColor = CyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PurpleLightContainer
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabContent"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> AppLockerHomeScreen(
                        onTestAppLock = { pkg, name ->
                            simulatingLockTarget = Pair(pkg, name)
                        },
                        onNavigateToStealth = {
                            selectedTab = 1
                        }
                    )
                    1 -> StealthDisguiseScreen(
                        onPreviewCalculatorDisguise = {
                            isTestingCalculatorPreview = true
                        },
                        onPreviewFakeCrash = {
                            simulatingLockTarget = Pair("com.whatsapp", "WhatsApp")
                        }
                    )
                    2 -> IntruderLogsScreen()
                    3 -> SecuritySettingsScreen()
                }
            }
        }
    }
}
