package com.pesalytics.util

/**
 * Centralised external links and contact details. Update the URLs here once the
 * Pesalytics website is deployed — no other code changes needed.
 */
object AppLinks {
    // TODO: replace with the live URLs once the website is deployed.
    const val PRIVACY_POLICY_URL = "https://Pesalytics.app/privacy"
    const val TERMS_OF_SERVICE_URL = "https://Pesalytics.app/terms"

    // Play Store listing (uses the release applicationId).
    const val PLAY_STORE_ID = "com.pesalytics.xmqs"
    const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PLAY_STORE_ID"
    const val PLAY_STORE_MARKET_URI = "market://details?id=$PLAY_STORE_ID"

    // Tip Jar — M-PESA number to send a tip to.
    const val TIP_JAR_MPESA = "0719713362"

    // Message shared via "Share App" / "Refer a Friend".
    const val SHARE_MESSAGE =
        "I'm using Pesalytics to turn my M-PESA SMS into a clean budget tracker — entirely on my phone, no sign-in. Check it out: $PLAY_STORE_WEB_URL"
}
