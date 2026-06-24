package com.pesalytics.data.billing

object BillingConfig {
    const val SKU_MONTHLY   = "pesalytics_premium_monthly"    // KES 299/month
    const val SKU_QUARTERLY = "pesalytics_premium_quarterly"  // KES 699/3 months
    const val SKU_YEARLY    = "pesalytics_premium_yearly"     // KES 2,000/year
    const val SKU_LIFETIME  = "pesalytics_premium_lifetime"   // KES 9,999 once

    val ALL_SUBS  = listOf(SKU_MONTHLY, SKU_QUARTERLY, SKU_YEARLY)
    val ALL_INAPP = listOf(SKU_LIFETIME)

    // v1.0.0 Play Store release date — anchors the EARLYBIRD 90-day sunset window.
    const val PLAY_STORE_LAUNCH_MS = 1748044800000L  // 2026-05-24 00:00 UTC
}
