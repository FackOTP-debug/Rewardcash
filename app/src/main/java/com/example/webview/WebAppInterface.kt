package com.example.webview

import android.app.Activity
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.example.viewmodel.RewardViewModel
import org.json.JSONObject

class WebAppInterface(
    private val activity: Activity,
    private val viewModel: RewardViewModel
) {

    @JavascriptInterface
    fun showRewardedAd(source: String, multiplier: Int, bonusCoins: Int) {
        activity.runOnUiThread {
            viewModel.watchRewardedAd(
                activity = activity,
                source = source,
                bonusCoins = (bonusCoins * multiplier).coerceAtLeast(100)
            )
        }
    }

    @JavascriptInterface
    fun getUserBalance(): String {
        val wallet = viewModel.walletState.value
        val json = JSONObject()
        json.put("coins", wallet?.coins ?: 500L)
        json.put("usd", wallet?.usdValue ?: 0.50)
        json.put("streak", wallet?.currentStreak ?: 1)
        return json.toString()
    }

    @JavascriptInterface
    fun recordGameReward(gameType: String, coins: Int) {
        activity.runOnUiThread {
            viewModel.addGameReward(
                title = when (gameType) {
                    "LUCKY_SPIN" -> "Lucky Wheel Win"
                    "SCRATCH_CARD" -> "Scratch Card Prize"
                    "TURBO_MINER" -> "Turbo Miner Harvest"
                    else -> "Game Hub Win"
                },
                coins = coins.toLong(),
                type = gameType
            )
        }
    }

    @JavascriptInterface
    fun triggerHaptic(effectType: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    activity.getSystemService(Activity.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = when (effectType) {
                    "light" -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    "heavy" -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    else -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = activity.getSystemService(Activity.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {
            // Ignore if vibrator not available
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
