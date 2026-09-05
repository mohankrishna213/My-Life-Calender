package com.mohanbuilds.focus.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationPreferencesTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Start each test from a clean preference file (default state).
        context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `enabled defaults to false`() {
        assertFalse(NotificationPreferences.isEnabled(context))
    }

    @Test
    fun `setEnabled true persists`() {
        NotificationPreferences.setEnabled(context, true)
        assertTrue(NotificationPreferences.isEnabled(context))
    }

    @Test
    fun `setEnabled false after true persists`() {
        NotificationPreferences.setEnabled(context, true)
        NotificationPreferences.setEnabled(context, false)
        assertFalse(NotificationPreferences.isEnabled(context))
    }
}