package com.pesalytics.util

/**
 * Centralised external links and contact details. Update the URLs here once the
 * Pesalytics website is deployed — no other code changes needed.
 */
object AppLinks {
    // Served via GitHub Pages until the pesalytics.app custom domain is wired up —
    // swap these back to https://pesalytics.app/... once DNS is configured.
    const val PRIVACY_POLICY_URL = "https://danielx8.github.io/pesalytics-web/privacy-policy/"
    const val TERMS_OF_SERVICE_URL = "https://danielx8.github.io/pesalytics-web/terms-of-service/"

    // Play Store listing (uses the release applicationId).
    const val PLAY_STORE_ID = "com.pesalytics.xmqs"
    const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PLAY_STORE_ID"
    const val PLAY_STORE_MARKET_URI = "market://details?id=$PLAY_STORE_ID"

    // Buy Me a Soda tip page.
    const val TIP_JAR_URL = "https://buymesoda.com/IFFDNKRkT2ZcNVkYJ6CZHruvpF62"

    // Message shared via "Share App" / "Refer a Friend".
    const val SHARE_MESSAGE =
        "I'm using Pesalytics to turn my M-PESA SMS into a clean budget tracker — entirely on my phone, no sign-in. Check it out: $PLAY_STORE_WEB_URL"
}
