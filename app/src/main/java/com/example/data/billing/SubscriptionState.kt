package com.pesalytics.data.billing

import com.pesalytics.domain.model.SubscriptionTier

data class SubscriptionState(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val expiryMs: Long = 0L,
    val trialDaysRemaining: Int = 0,
    val source: String = "play"
) {
    val isPremium: Boolean get() = tier.isPremium
    val isLifetime: Boolean get() = tier == SubscriptionTier.PREMIUM_LIFETIME
    val isTrial: Boolean get() = tier == SubscriptionTier.TRIAL
    val isFree: Boolean get() = tier == SubscriptionTier.FREE
}

sealed class PromoGrant {
    object Lifetime    : PromoGrant()
    object Monthly     : PromoGrant()
    object Quarterly   : PromoGrant()
    object Yearly      : PromoGrant()
    object Trial14Days : PromoGrant()
}

data class PromoCodeEntry(val grant: PromoGrant, val label: String)

sealed class PromoResult {
    data class Success(val grant: PromoGrant, val label: String) : PromoResult()
    object EarlybirdLifetime : PromoResult()
    object EarlybirdSunset   : PromoResult()
    object AlreadyRedeemed   : PromoResult()
    object Invalid           : PromoResult()
}
