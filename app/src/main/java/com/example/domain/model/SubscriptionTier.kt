package com.pesalytics.domain.model

enum class SubscriptionTier {
    FREE,
    TRIAL,
    PREMIUM_MONTHLY,
    PREMIUM_QUARTERLY,
    PREMIUM_YEARLY,
    PREMIUM_LIFETIME;

    val isPremium: Boolean get() = this != FREE
}
