package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SecurityPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("App Locker", appName)
  }

  @Test
  fun `verify security preferences lock toggle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = SecurityPreferences.getInstance(context)
    val testPkg = "com.test.app"

    prefs.setPackageLocked(testPkg, true)
    assertTrue(prefs.lockedPackages.value.contains(testPkg))

    prefs.setPackageLocked(testPkg, false)
    assertTrue(!prefs.lockedPackages.value.contains(testPkg))
  }

  @Test
  fun `verify biometric preference default and toggle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = SecurityPreferences.getInstance(context)

    // Verify biometric preference is enabled by default
    assertTrue(prefs.isBiometricEnabled.value)

    prefs.setBiometricEnabled(false)
    assertTrue(!prefs.isBiometricEnabled.value)

    prefs.setBiometricEnabled(true)
    assertTrue(prefs.isBiometricEnabled.value)
  }
}

