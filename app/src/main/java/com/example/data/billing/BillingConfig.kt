package com.pesasense.data.billing

object BillingConfig {
    const val SKU_MONTHLY   = "pesasense_premium_monthly"    // KES 299/month
    const val SKU_QUARTERLY = "pesasense_premium_quarterly"  // KES 699/3 months
    const val SKU_YEARLY    = "pesasense_premium_yearly"     // KES 2,000/year
    const val SKU_LIFETIME  = "pesasense_premium_lifetime"   // KES 9,999 once

    val ALL_SUBS  = listOf(SKU_MONTHLY, SKU_QUARTERLY, SKU_YEARLY)
    val ALL_INAPP = listOf(SKU_LIFETIME)
}
