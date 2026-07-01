package com.pesalytics.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.pesalytics.domain.model.SubscriptionTier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class SubscriptionManager(private val context: Context) : PurchasesUpdatedListener, BillingClientStateListener {

    private val prefs = context.getSharedPreferences("pesa_subscription", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var reconnectDelay = 1_000L

    private val _state = MutableStateFlow(loadStateFromPrefs())
    val state: StateFlow<SubscriptionState> = _state

    private val _trialJustStarted = MutableStateFlow(false)
    val trialJustStarted: StateFlow<Boolean> = _trialJustStarted

    fun consumeTrialStartedEvent() { _trialJustStarted.value = false }

    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    // ── Play Billing connection ──────────────────────────────────────────

    fun connect() {
        if (!billingClient.isReady) billingClient.startConnection(this)
    }

    fun disconnect() {
        billingClient.endConnection()  // stop incoming events before cancelling inflight coroutines
        scope.cancel()
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        reconnectDelay = 1_000L
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            scope.launch { queryProducts(); syncPurchases() }
        }
    }

    override fun onBillingServiceDisconnected() {
        scope.launch {
            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceAtMost(32_000L)
            connect()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            scope.launch { processPurchases(purchases.orEmpty()) }
        }
    }

    private suspend fun queryProducts() {
        val subsResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                BillingConfig.ALL_SUBS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
                }
            ).build()
        )
        val inappResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                BillingConfig.ALL_INAPP.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it).setProductType(BillingClient.ProductType.INAPP).build()
                }
            ).build()
        )
        _products.value = (subsResult.productDetailsList.orEmpty() + inappResult.productDetailsList.orEmpty())
            .associateBy { it.productId }
    }

    suspend fun syncPurchases() {
        val subs = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        val inapp = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        processPurchases(subs.purchasesList + inapp.purchasesList)
    }

    private suspend fun processPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!purchase.isAcknowledged) {
                val ack = billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                )
                if (ack.responseCode != BillingClient.BillingResponseCode.OK) continue
            }
            val tier = tierFromProducts(purchase.products) ?: continue
            val expiryMs = purchase.purchaseTime + expiryDurationMs(tier)
            savePlayBillingState(tier, purchase.purchaseToken, expiryMs)
            _state.value = loadStateFromPrefs()
        }
    }

    private fun tierFromProducts(products: List<String>): SubscriptionTier? = when {
        BillingConfig.SKU_LIFETIME  in products -> SubscriptionTier.PREMIUM_LIFETIME
        BillingConfig.SKU_YEARLY    in products -> SubscriptionTier.PREMIUM_YEARLY
        BillingConfig.SKU_QUARTERLY in products -> SubscriptionTier.PREMIUM_QUARTERLY
        BillingConfig.SKU_MONTHLY   in products -> SubscriptionTier.PREMIUM_MONTHLY
        else -> null
    }

    private fun expiryDurationMs(tier: SubscriptionTier): Long = when (tier) {
        SubscriptionTier.PREMIUM_MONTHLY   -> TimeUnit.DAYS.toMillis(31)
        SubscriptionTier.PREMIUM_QUARTERLY -> TimeUnit.DAYS.toMillis(92)
        SubscriptionTier.PREMIUM_YEARLY    -> TimeUnit.DAYS.toMillis(366)
        SubscriptionTier.PREMIUM_LIFETIME  -> Long.MAX_VALUE / 2
        else -> 0L
    }

    fun launchBillingFlow(activity: Activity, sku: String, offerToken: String? = null): BillingResult {
        val productDetails = _products.value[sku] ?: return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE).build()
        val params = if (offerToken != null) {
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails).setOfferToken(offerToken).build())
        } else {
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails).build())
        }
        return billingClient.launchBillingFlow(activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(params).build())
    }

    // ── State persistence ─────────────────────────────────────────────────

    private fun loadStateFromPrefs(): SubscriptionState {
        val now = System.currentTimeMillis()
        val tierName = prefs.getString("tier", SubscriptionTier.FREE.name) ?: SubscriptionTier.FREE.name
        val tier = runCatching { SubscriptionTier.valueOf(tierName) }.getOrDefault(SubscriptionTier.FREE)

        if (tier == SubscriptionTier.PREMIUM_LIFETIME) {
            return SubscriptionState(tier = tier, expiryMs = Long.MAX_VALUE / 2)
        }
        val expiryMs = prefs.getLong("expiry_ms", 0L)
        if (tier.isPremium && expiryMs > now) {
            return SubscriptionState(tier = tier, expiryMs = expiryMs)
        }
        val referralExpiry = prefs.getLong("referral_expiry_ms", 0L)
        if (referralExpiry > now) {
            return SubscriptionState(tier = SubscriptionTier.PREMIUM_MONTHLY, expiryMs = referralExpiry, source = "referral")
        }
        val trialStart = prefs.getLong("trial_start_ms", 0L)
        val trialDuration = TimeUnit.DAYS.toMillis(14)
        if (trialStart > 0L && (now - trialStart) < trialDuration) {
            val remaining = TimeUnit.MILLISECONDS.toDays(trialDuration - (now - trialStart)).toInt().coerceAtLeast(1)
            return SubscriptionState(tier = SubscriptionTier.TRIAL, expiryMs = trialStart + trialDuration, trialDaysRemaining = remaining)
        }
        // One-time migration: prior versions wrote is_premium=true to a different prefs file.
        // Elevate those users to PREMIUM_LIFETIME so they don't lose access after an update.
        val legacyPrefs = context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
        if (legacyPrefs.getBoolean("is_premium", false)) {
            prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_LIFETIME.name)
                .putLong("expiry_ms", Long.MAX_VALUE / 2)
                .apply()
            legacyPrefs.edit().remove("is_premium").apply()
            return SubscriptionState(tier = SubscriptionTier.PREMIUM_LIFETIME, expiryMs = Long.MAX_VALUE / 2)
        }
        return SubscriptionState(tier = SubscriptionTier.FREE)
    }

    private fun savePlayBillingState(tier: SubscriptionTier, token: String, expiryMs: Long) {
        prefs.edit().putString("tier", tier.name).putLong("expiry_ms", expiryMs)
            .putString("purchase_token", token).apply()
    }

    fun startTrialIfNotStarted() {
        if (prefs.getLong("trial_start_ms", 0L) == 0L) {
            prefs.edit().putLong("trial_start_ms", System.currentTimeMillis()).apply()
            _state.value = loadStateFromPrefs()
            _trialJustStarted.value = true
        }
    }

    fun grantReferralBonus() {
        prefs.edit().putLong("referral_expiry_ms", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)).apply()
        _state.value = loadStateFromPrefs()
    }

    fun grantFromPromo(grant: PromoGrant) {
        val now = System.currentTimeMillis()
        when (grant) {
            is PromoGrant.Lifetime -> prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_LIFETIME.name)
                .putLong("expiry_ms", Long.MAX_VALUE / 2).apply()
            is PromoGrant.Monthly -> prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_MONTHLY.name)
                .putLong("expiry_ms", now + TimeUnit.DAYS.toMillis(31)).apply()
            is PromoGrant.Quarterly -> prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_QUARTERLY.name)
                .putLong("expiry_ms", now + TimeUnit.DAYS.toMillis(92)).apply()
            is PromoGrant.Yearly -> prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_YEARLY.name)
                .putLong("expiry_ms", now + TimeUnit.DAYS.toMillis(366)).apply()
            is PromoGrant.Trial14Days -> prefs.edit()
                .putLong("trial_start_ms", now).apply()
        }
        _state.value = loadStateFromPrefs()
    }

    // ── Promo code redemption ─────────────────────────────────────────────

    private val codeRegistry: Map<String, PromoCodeEntry> = mapOf(
        // ── LIFETIME (10) ────────────────────────────────────────────────────
        "dbb15f74e4f1862504f87db6aa2e3025710e5188c993e565d450364bd0aaa827" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-01"),
        "f7857049dac7d7ab0be2eea0f69ef409605f4ff3ce47b1742d78f75c9608bfb1" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-02"),
        "8abf2b2ed730fa169ef47cece1a0401dd1ef3a597462bf521e95874124dd4d8c" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-03"),
        "5ee503983d4ecd48c310b988b65313d5156f213eea3ae30e632d0e0e33307268" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-04"),
        "30d07721ab2bcb899fcd9d6049e9578468539986e32cb2523e3c7d8a90f2d93a" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-05"),
        "9afc1f0c96a0268cd3c994c946dc0b5897bb3d597449e878b44b02c73cd7b4e1" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-06"),
        "434fa6e0005ecbf897317410405936a93562c7c436b2b786b41c981f2a9f3758" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-07"),
        "506dcbb43d4e8a350c8fc853fd5de98406bdf42bfb98cdde3fe4e5bab705d1f1" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-08"),
        "2add4d6519f0847fbbfad5e54d1667b117c774d2965098cdd722719148db215d" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-09"),
        "765c72032a0144d2cd167b279ca54bf81b0a124a6a1e025569a7547c85312e4a" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-10"),
        // ── YEARLY (20) ──────────────────────────────────────────────────────
        "98218eba905b3de75d5800df2e9cbad5ac215403e8a5693437cd91cc7ef893de" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-01"),
        "cf9571688cfffa378e7b57ec8762afe58ad90468bd90861108a5530049a7dbef" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-02"),
        "f56bcb4800bf7f6e4ad31129af0d3dac9e12f8df44af99bd518d41fb9e474bf6" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-03"),
        "b79ac9229f8a1818f5c1ec1bf073aaeca9fe7e111d5f8b05366f8da2ed209301" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-04"),
        "7de2a23bcdcc54bb7c91012b45adde749da88dbcb5cb5678ac725622ed319efc" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-05"),
        "726de9f9a391ef9c4c163f15cbefa87196e3eabdb5acd543276f14aa67ecd8f3" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-06"),
        "b138baea6e2231273af0735754faebcc4560df7311ed73f6da34958c5aa24d79" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-07"),
        "e5bf75943057bf94172f1ae1b7272da98a61071a06340f876c415879d2ce1f25" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-08"),
        "914d1f57510d78f8cb52bdc134ddc87c795de222385303c945fdca17a9c7467a" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-09"),
        "4917f661012180de3612f82edd1de7203a8964efb8789863a491846a336e5865" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-10"),
        "7eaa708db3eb128e6564c066ef01594e642aaf43b67a05fa2028c6c845342af9" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-11"),
        "8411c4041c21855a8b3076ac9f284f9b33aa2cbd573dfa088bd839ffbc0dbf75" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-12"),
        "97807f0219cfbbeb46f12e7f48590fd32ffaba1d705b921fc49af6163fc3e5a6" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-13"),
        "25d0085b3fb722f63396af8a216e5474c4dc2a72fc77a765295c08b24c0f9d04" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-14"),
        "eb6ea38372aeeb5d984ac071b9c530df9825c03afba2749d80448650f651d35f" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-15"),
        "53522f7d24499f8cfc0caf5921e1e40b2ad8ae484294fe660b1478325097a1df" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-16"),
        "e250d81c9e6b08b6fcffcb182688814f986429c7c56c7debc9615954a248cd34" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-17"),
        "5d065f9e7a76e92270c5c4b22098d11c07401005d6772c448a6e16da7f7e1948" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-18"),
        "eba1756195a41b46a4e5b788c16768571e68049eb834877abe92f87b9133d769" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-19"),
        "472eab547ba7e9d9a5c6fb6af9339a60fe7c1a8c187ea16b837e15843747a6b2" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-20"),
        // ── QUARTERLY (20) ───────────────────────────────────────────────────
        "b14d0204c0020eb4af9c6250743c7e0def6dbd7f18ebb31542c8dc0e08bbdffd" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-01"),
        "f5397a5e0b1dacb54ce756fdaba40c9ab5c88b895525a05523440c01442a51d7" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-02"),
        "1f04a490012a258ee725d948688c85bf96c5278c22caa045d1e71d13b52d3bdc" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-03"),
        "048a10f9359d5637e98ca3ba7058b82a67020f926e0ebbd1f1d56f3a76f2312b" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-04"),
        "1d4bc74f920325eb10b24db1064ee2c3d4db26db8898053bde07383816d231f0" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-05"),
        "f7f50cbae23aab68a9650db62de849d546a73e8cc71aba0596dd2a2f9ef020ff" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-06"),
        "3575fedef4c9d13d39f3b1f017430524cf763ce38afafcb334405401bd0d8e8e" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-07"),
        "55346b42144ed978bb3e99b7f826b53e25d61017ed9c1d6755b6e71c53c5a382" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-08"),
        "57b7b952ee00a8dc2765ca8b4b65e023da117b545bc63377f407ea02ddcb341c" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-09"),
        "0be0ced2c58568f1bc30460f950ed65866a9113c951a2f766643c6d88a93641c" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-10"),
        "1d2749a938feeb89200df0fed4d1e3b245b3fb72132f1d499d6dcb9d31256ec3" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-11"),
        "36494c81b5cc7311b9e79796011a5744ee77024c9bcfd9de130bf60722f1a479" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-12"),
        "4fd53ec6b41828081559bb52ce1f536667145cf25136e909e3ddd624f018e98f" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-13"),
        "635f6160d207c586086873cc5d3ead04bc32740a26b6aa72172618b1bae9fb06" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-14"),
        "62d826534f7cd9e034dcb2773949490e81b3ee6f0c3d77c1ef0a088582207f61" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-15"),
        "eae5b805b9fe0552fe706546560cde7316a6c341988e0f68c58db56033098b15" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-16"),
        "724d4f8e70f4079a6a7831b006e87e4a52dbfc2633debddc3c8e9ba788d8651f" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-17"),
        "b3f484f5a850af964b21b9c6080d72fd9e64c227e7cadc8289857aff78f9d3a3" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-18"),
        "334c6eb303380227e7d1e2058b1732e5ffe588f6c74cae878eee1996f1067b87" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-19"),
        "23fe5f8abc5f627aa529dd281c23132b4480a8b30f4c7b8566015ec4d84f5b64" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-20"),
        // ── MONTHLY (50) ─────────────────────────────────────────────────────
        "f9173735bd5bee95b9cfb5035c98ebbefe39589544afab90ddae2993d716cc36" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-01"),
        "4b40252cefd084b598f96ae444c28b504fc3350e7f82adbc69bc41de0e85f858" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-02"),
        "75842b023b96ccb58de39df8f0f4e0f195589eb714512e9ede97c97aa831ebe7" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-03"),
        "d8e9ac93a19d0fb3ea0e56922f6daf7dca52e67d41997e18637af991ed6b54e7" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-04"),
        "8120e9c37f4418f06dccab50efd2d6ab01c4c7cb4d968b6c9826524a7902fd63" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-05"),
        "23949163a14ca8bc322dc1f62e2df73d44add58e5189a1ec107bfee2fe26d21d" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-06"),
        "1dec94a271ae97d52f75bbf2ba07fc2b25c5a894fe6c2ca2e50e6418d4a1f365" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-07"),
        "e24473336c6046d414f8e4022f744ba5f208b869069435c73363142f2965695d" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-08"),
        "d47f2eb0a9cc53b1d29d5a67d539da5f3426bea2330ace0a295b1c856af46d97" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-09"),
        "654162b422ec8a82dd4207cea58c2982dbf3c0b69de903085786401adc448630" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-10"),
        "e2583935f4f17ec76602277b0c5c6553dce2f934c8a568f9adeebd132ea50e98" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-11"),
        "d5f8d4791f4c206ef7c07421c53cf5875f83d091ea71724be516507964672441" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-12"),
        "1bd472988a23fc4d39dbbf229465575e14756b27d80f8b42e42d087976a4b43f" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-13"),
        "275c210ae9af8029f01dd9dfbb08928f65b1e15726d3d6c24dee22f453f6af44" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-14"),
        "89286f97b19a2ae91afa167ceb9a95acb2f9ca8a9996241eda5b9fe68ad2a328" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-15"),
        "99eb289b382f3c2fca0c05c8cbfe46a98f68253db9dd374383e75502ef0f1924" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-16"),
        "15c1c3e1ded5f845f0d757367b041362f03df77feadb9736643207560ecb72df" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-17"),
        "19ba90e3ce2ba103902d57c5321b4a99830b65941469eb9acd2c2d7025caa898" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-18"),
        "bc60eb8e12668e8119816b44a1d9ac30e6c71daf61e506415d9b6e2af0f5ce09" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-19"),
        "07b661f106eb7f4d933d86642173b0e33eeff26b2e2b31e9f9eb250a607cabca" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-20"),
        "27d912893020fb7e314966f75715c1cdf9258b106366438fb85225615461caab" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-21"),
        "859b317944ef7b17058499840dc055959cc0fe4f84c4216f1fd3f5fedb0ead2d" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-22"),
        "835ca620c05233b6eed2725e1eeae80e69a7f012af153e499349cd7df0b61637" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-23"),
        "f594cea0d6d9823342511d9eb243d7fffb46a0860eac1931d54813e43a8c4fcf" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-24"),
        "3b8b7f7cff8c88ea4b0189d3bb9590537fd82393d4eadf05e5e856f8232756f9" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-25"),
        "4a36bfabe825d632738ecac883343fe6af9d11688c3ff94462bb5f9dd465133e" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-26"),
        "13a5dfbcfbb5491657bc97819f20558f546674c727ee44b73c44129538b98ed2" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-27"),
        "364492e18fdcdea7e68323458bef399c45d90d469f2f264c55529c300285721d" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-28"),
        "dd784dbe2394179f3e0130fd6d3a8f14d6c12a8da6c88c8fcfd879a6a931ff01" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-29"),
        "3aaa25c1b3f345ca1c8d43c97c4fd8aebed7df703126c04a74bfaa2be20a0cef" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-30"),
        "54f2a95300b8afede989c766612c3e75e5a47d9999f9c9a35509b10b63aaf794" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-31"),
        "5226dbf3a3066d363596464ccca335cb128ffc0c5a1e6f71deaf7246d45bafc0" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-32"),
        "6bd190b95e0ebf2df7aa3dc9cb56c21d8b04b218ff53dc924efb76403d8907e6" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-33"),
        "ded96acf4a373f97e8a7d0d60f3cc9e68701c19085f195347858f871fd1044e3" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-34"),
        "047e369d4c2bde1c73a7d27ab3cf83597d7ed504d62602529335d81bac2b5405" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-35"),
        "e86282d6933ee2bd82050976dd9a524767a994a238c1cbb6a38d2c5a9e68bea8" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-36"),
        "646825da0906f0318f87c42b64c5ae49467bcbc1ddf5b9f5bb8f4d61c56aaa60" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-37"),
        "b0d0169694a303593af79dd88543d74a54e1d4cd8ba3244238b72f7c5ee52465" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-38"),
        "998ff02263414f2b1534773e64a0c9d16b67f7d7600a60b74c63fa8ee7e2434a" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-39"),
        "c78356e4a654bd7c407f54a887c17f6210880e9cb407cb5d9059427618822b91" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-40"),
        "f8c05c48bc6cc5dcba42929e4ace529b38cb5efcbb74846c4e81468356d16a8c" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-41"),
        "45f8e28a0e1e189346447bb1901e2bbe4eeedd7b1969b9c4e25a9d2a52e8f3ab" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-42"),
        "2b45a44bb8d7d4ccdba1e35ace4448b605d2a3d37a39dfc5f95ceccd44847d99" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-43"),
        "e1d5d9967b7eb33f856a70cb639e696402f18555d8261b7136f6642f8337d9ee" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-44"),
        "b970b82b448e073edf24b2e5e6f6ac615342d4f8c5f0d0f786600e6723cbe376" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-45"),
        "163882051bf14203c0f3511acc483b9c60c8c85949d4f3bdb2f4a514a148587f" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-46"),
        "1d1a537048cdacaab17c13bc669a338635fd26a2fa152299345b92b4277e3c73" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-47"),
        "98089903398115f669714b3b7c08124a1acfa260f765181159bd80de55ef293a" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-48"),
        "43107651059327eb6126c00778d3a67d8edf99e8d29d46c240a26df6d4b93c7d" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-49"),
        "6bc073c9318655592768b77f3bf71b4c5e8a5396a530a9e978a53a21f4950fcc" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-50"),
    )

    private val MAX_EARLYBIRD = 300
    private val EARLYBIRD_WINDOW_DAYS = 90L

    fun redeemPromoCode(rawCode: String): PromoResult {
        val code = rawCode.trim().uppercase()

        if (code == "EARLYBIRD") {
            val alreadyRedeemed = prefs.getBoolean("earlybird_redeemed", false)
            if (alreadyRedeemed) return PromoResult.AlreadyRedeemed

            val count = prefs.getInt("earlybird_count", 0)
            val daysSince = (System.currentTimeMillis() - BillingConfig.PLAY_STORE_LAUNCH_MS) / TimeUnit.DAYS.toMillis(1)
            val sunset = count >= MAX_EARLYBIRD || daysSince >= EARLYBIRD_WINDOW_DAYS

            return if (sunset) {
                grantFromPromo(PromoGrant.Trial14Days)
                PromoResult.EarlybirdSunset
            } else {
                prefs.edit().putInt("earlybird_count", count + 1).putBoolean("earlybird_redeemed", true).apply()
                grantFromPromo(PromoGrant.Lifetime)
                PromoResult.EarlybirdLifetime
            }
        }

        val hash = sha256(code)
        val result: PromoResult = synchronized(this) {
            val redeemed = prefs.getStringSet("redeemed_codes", emptySet()) ?: emptySet()
            if (hash in redeemed) return@synchronized PromoResult.AlreadyRedeemed
            val entry = codeRegistry[hash] ?: return@synchronized PromoResult.Invalid
            prefs.edit().putStringSet("redeemed_codes", redeemed + hash).apply()
            PromoResult.Success(entry.grant, entry.label)
        }
        if (result is PromoResult.Success) grantFromPromo(result.grant)
        return result
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
