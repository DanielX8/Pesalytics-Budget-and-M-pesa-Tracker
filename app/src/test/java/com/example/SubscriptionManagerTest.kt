package com.pesasense

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pesasense.data.billing.SubscriptionManager
import com.pesasense.domain.model.SubscriptionTier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubscriptionManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pesa_subscription", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    // Bug #4: Cold migration — users who had premium via old pesa_prefs["is_premium"]
    // must not lose their status when the app is updated to the new SubscriptionManager.

    @Test
    fun `legacy pesa_prefs is_premium=true migrates to PREMIUM_LIFETIME on first load`() {
        context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_premium", true).commit()

        val manager = SubscriptionManager(context)

        assertTrue(
            "User with legacy pesa_prefs[is_premium=true] should be PREMIUM after migration",
            manager.state.value.isPremium
        )
    }

    @Test
    fun `legacy pesa_prefs is_premium=false results in FREE tier`() {
        context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_premium", false).commit()

        val manager = SubscriptionManager(context)

        assertFalse("User with legacy is_premium=false should remain FREE", manager.state.value.isPremium)
    }

    @Test
    fun `migration writes tier to pesa_subscription so it persists across restarts`() {
        context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_premium", true).commit()

        SubscriptionManager(context) // triggers migration

        val tier = context.getSharedPreferences("pesa_subscription", Context.MODE_PRIVATE)
            .getString("tier", null)
        assertEquals(
            "Migration should write tier to pesa_subscription for persistence",
            SubscriptionTier.PREMIUM_LIFETIME.name, tier
        )
    }

    // Bug #3: Trial clock — SubscriptionManager is the source of truth for trial state.
    // SettingsScreen must read from here, not from an independent install_date counter.

    @Test
    fun `trial gives 13 or 14 days remaining immediately after startTrialIfNotStarted`() {
        val manager = SubscriptionManager(context)
        manager.startTrialIfNotStarted()

        val daysRemaining = manager.state.value.trialDaysRemaining
        // toDays() truncates, so a freshly-started trial reports 13 or 14 days
        assertTrue(
            "Expected 13–14 days remaining immediately after trial start, got $daysRemaining",
            daysRemaining in 13..14
        )
    }

    @Test
    fun `trial is expired and returns FREE after 15 days`() {
        val fifteenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15)
        context.getSharedPreferences("pesa_subscription", Context.MODE_PRIVATE)
            .edit().putLong("trial_start_ms", fifteenDaysAgo).commit()

        val manager = SubscriptionManager(context)

        assertEquals(SubscriptionTier.FREE, manager.state.value.tier)
        assertEquals(0, manager.state.value.trialDaysRemaining)
    }

    @Test
    fun `trial is TRIAL tier and isPremium=true within the 14-day window`() {
        val twoDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
        context.getSharedPreferences("pesa_subscription", Context.MODE_PRIVATE)
            .edit().putLong("trial_start_ms", twoDaysAgo).commit()

        val manager = SubscriptionManager(context)

        assertEquals(SubscriptionTier.TRIAL, manager.state.value.tier)
        assertTrue(manager.state.value.isPremium)
        assertEquals(12, manager.state.value.trialDaysRemaining)
    }
}
