package com.example.admob

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AdStatus {
    object Idle : AdStatus()
    object Loading : AdStatus()
    data class Ready(val adUnitId: String) : AdStatus()
    data class Showing(val source: String) : AdStatus()
    data class Completed(val amount: Int, val type: String) : AdStatus()
    data class Failed(val reason: String) : AdStatus()
    data class Cooldown(val secondsRemaining: Int) : AdStatus()
}

class AdMobManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("admob_reward_prefs", Context.MODE_PRIVATE)

    private val _adStatus = MutableStateFlow<AdStatus>(AdStatus.Idle)
    val adStatus: StateFlow<AdStatus> = _adStatus.asStateFlow()

    private val _totalAdsWatched = MutableStateFlow(prefs.getInt("total_ads_watched", 0))
    val totalAdsWatched: StateFlow<Int> = _totalAdsWatched.asStateFlow()

    private val _isTestMode = MutableStateFlow(prefs.getBoolean("is_test_mode", true))
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    private val _customAdUnitId = MutableStateFlow(prefs.getString("custom_ad_unit_id", "") ?: "")
    val customAdUnitId: StateFlow<String> = _customAdUnitId.asStateFlow()

    private val _isSimulationEnabled = MutableStateFlow(prefs.getBoolean("simulation_mode", false))
    val isSimulationEnabled: StateFlow<Boolean> = _isSimulationEnabled.asStateFlow()

    private var currentRewardedAd: RewardedAd? = null
    private var isAdLoading = false
    private var cooldownTimer: CountDownTimer? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "AdMobManager"
        // Official Google Sample Ad Unit ID for Rewarded Ads
        const val GOOGLE_TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val COOLDOWN_SECONDS = 15

        @Volatile
        private var INSTANCE: AdMobManager? = null

        fun getInstance(context: Context): AdMobManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AdMobManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        initializeSdk()
    }

    fun initializeSdk() {
        if (!isInitialized) {
            try {
                MobileAds.initialize(appContext) { status ->
                    Log.d(TAG, "AdMob MobileAds initialized: $status")
                    isInitialized = true
                    preloadRewardedAd()
                }
            } catch (e: Exception) {
                Log.e(TAG, "AdMob initialization error", e)
            }
        }
    }

    fun getEffectiveAdUnitId(): String {
        val custom = _customAdUnitId.value.trim()
        return if (!_isTestMode.value && custom.isNotEmpty()) {
            custom
        } else {
            GOOGLE_TEST_REWARDED_AD_UNIT_ID
        }
    }

    fun preloadRewardedAd(force: Boolean = false) {
        if (_adStatus.value is AdStatus.Cooldown && !force) {
            return
        }

        if (currentRewardedAd != null && !force) {
            _adStatus.value = AdStatus.Ready(getEffectiveAdUnitId())
            return
        }

        if (isAdLoading) return

        isAdLoading = true
        _adStatus.value = AdStatus.Loading

        val adRequest = AdRequest.Builder().build()
        val adUnitId = getEffectiveAdUnitId()

        RewardedAd.load(
            appContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isAdLoading = false
                    currentRewardedAd = ad
                    _adStatus.value = AdStatus.Ready(adUnitId)
                    Log.d(TAG, "RewardedAd successfully loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoading = false
                    currentRewardedAd = null
                    val errorMsg = "Ad Load Failed: ${loadAdError.message} (code: ${loadAdError.code})"
                    Log.w(TAG, errorMsg)
                    _adStatus.value = AdStatus.Failed(errorMsg)
                }
            }
        )
    }

    /**
     * Shows the AdMob Rewarded Ad.
     * @param activity Context required to display full screen AdMob activity
     * @param source Identifies the feature triggering the ad (e.g., "DAILY_CLAIM", "SPIN_DOUBLE", "CHEST_UNLOCK")
     * @param onUserEarnedReward Callback invoked when user successfully finishes watching the video ad
     * @param onAdDismissed Callback when the ad dialog is closed
     * @param onAdFailed Callback when ad fails to show
     */
    fun showRewardedAd(
        activity: Activity,
        source: String,
        onUserEarnedReward: (RewardItem) -> Unit,
        onAdDismissed: (Boolean) -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        // Check cooldown
        if (_adStatus.value is AdStatus.Cooldown) {
            val cooldownSeconds = (_adStatus.value as AdStatus.Cooldown).secondsRemaining
            val msg = "Please wait $cooldownSeconds seconds before watching another rewarded ad."
            onAdFailed(msg)
            return
        }

        // If simulation mode enabled or ad not cached yet, allow graceful fallback
        if (currentRewardedAd == null) {
            if (_isSimulationEnabled.value) {
                // Simulate successful ad completion for local sandbox testing
                simulateRewardedAd(source, onUserEarnedReward, onAdDismissed)
                return
            } else {
                preloadRewardedAd()
                val msg = "Ad is still loading. Please try again in a few moments."
                _adStatus.value = AdStatus.Failed(msg)
                onAdFailed(msg)
                return
            }
        }

        val ad = currentRewardedAd
        var earnedReward = false
        var rewardedItemResult: RewardItem? = null

        _adStatus.value = AdStatus.Showing(source)

        ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed full screen content.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed full screen content.")
                currentRewardedAd = null
                _adStatus.value = AdStatus.Idle
                startCooldownTimer()
                preloadRewardedAd()
                onAdDismissed(earnedReward)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show full screen: ${adError.message}")
                currentRewardedAd = null
                val errorMsg = "Ad Failed: ${adError.message}"
                _adStatus.value = AdStatus.Failed(errorMsg)
                preloadRewardedAd()
                onAdFailed(errorMsg)
            }
        }

        ad?.show(activity) { rewardItem ->
            earnedReward = true
            rewardedItemResult = rewardItem
            recordAdImpression()
            _adStatus.value = AdStatus.Completed(rewardItem.amount, rewardItem.type)
            onUserEarnedReward(rewardItem)
        }
    }

    private fun simulateRewardedAd(
        source: String,
        onUserEarnedReward: (RewardItem) -> Unit,
        onAdDismissed: (Boolean) -> Unit
    ) {
        _adStatus.value = AdStatus.Showing(source)
        // Simulated Reward item
        val dummyItem = object : RewardItem {
            override fun getAmount(): Int = 100
            override fun getType(): String = "Coins"
        }
        recordAdImpression()
        _adStatus.value = AdStatus.Completed(100, "Coins")
        onUserEarnedReward(dummyItem)
        startCooldownTimer()
        onAdDismissed(true)
    }

    private fun recordAdImpression() {
        val count = _totalAdsWatched.value + 1
        _totalAdsWatched.value = count
        prefs.edit().putInt("total_ads_watched", count).apply()
    }

    private fun startCooldownTimer() {
        cooldownTimer?.cancel()
        cooldownTimer = object : CountDownTimer((COOLDOWN_SECONDS * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt()
                _adStatus.value = AdStatus.Cooldown(sec)
            }

            override fun onFinish() {
                _adStatus.value = AdStatus.Idle
                preloadRewardedAd()
            }
        }.start()
    }

    fun setTestMode(enabled: Boolean) {
        _isTestMode.value = enabled
        prefs.edit().putBoolean("is_test_mode", enabled).apply()
        currentRewardedAd = null
        preloadRewardedAd(force = true)
    }

    fun setCustomAdUnitId(adUnitId: String) {
        _customAdUnitId.value = adUnitId
        prefs.edit().putString("custom_ad_unit_id", adUnitId).apply()
        currentRewardedAd = null
        preloadRewardedAd(force = true)
    }

    fun setSimulationMode(enabled: Boolean) {
        _isSimulationEnabled.value = enabled
        prefs.edit().putBoolean("simulation_mode", enabled).apply()
    }
}
