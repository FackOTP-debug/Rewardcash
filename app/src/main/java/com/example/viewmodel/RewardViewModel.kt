package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.admob.AdMobManager
import com.example.admob.AdStatus
import com.example.data.DailyTask
import com.example.data.RewardDatabase
import com.example.data.RewardTransaction
import com.example.data.Wallet
import com.example.data.WithdrawalRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class RewardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RewardDatabase.getInstance(application)
    private val dao = db.rewardDao()
    val adMobManager = AdMobManager.getInstance(application)

    val walletState: StateFlow<Wallet?> = dao.getWallet()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val transactionsState: StateFlow<List<RewardTransaction>> = dao.getRecentTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tasksState: StateFlow<List<DailyTask>> = dao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val withdrawalsState: StateFlow<List<WithdrawalRequest>> = dao.getWithdrawals()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val adStatus: StateFlow<AdStatus> = adMobManager.adStatus
    val totalAdsWatched: StateFlow<Int> = adMobManager.totalAdsWatched
    val isTestMode: StateFlow<Boolean> = adMobManager.isTestMode
    val customAdUnitId: StateFlow<String> = adMobManager.customAdUnitId
    val isSimulationEnabled: StateFlow<Boolean> = adMobManager.isSimulationEnabled

    private val _userFeedbackEvent = MutableSharedFlow<String>()
    val userFeedbackEvent: SharedFlow<String> = _userFeedbackEvent.asSharedFlow()

    private var activeWebViewRef: WeakReference<WebView>? = null

    fun registerActiveWebView(webView: WebView?) {
        activeWebViewRef = if (webView != null) WeakReference(webView) else null
    }

    fun watchRewardedAd(
        activity: Activity,
        source: String = "DAILY_REWARD",
        bonusCoins: Int = 100
    ) {
        adMobManager.showRewardedAd(
            activity = activity,
            source = source,
            onUserEarnedReward = { rewardItem ->
                viewModelScope.launch {
                    val earnedCoins = if (rewardItem.amount > 0 && rewardItem.amount != 1) {
                        rewardItem.amount.toLong()
                    } else {
                        bonusCoins.toLong()
                    }
                    val sourceTitle = when (source) {
                        "DAILY_REWARD" -> "Daily Rewarded Ad"
                        "SPIN_2X" -> "2X Lucky Wheel Bonus"
                        "SCRATCH_3X" -> "3X Scratch Card Bonus"
                        "MINER_SUPERCHARGE" -> "Miner Supercharge Bonus"
                        else -> "Rewarded Video Bonus ($source)"
                    }

                    dao.addRewardCoins(
                        title = sourceTitle,
                        coins = earnedCoins,
                        type = "REWARDED_AD"
                    )

                    dao.incrementTaskProgress("AD")

                    // Notify WebView if running
                    val updatedWallet = dao.getWalletDirect()
                    val newTotal = updatedWallet?.coins ?: 0L
                    activeWebViewRef?.get()?.post {
                        activeWebViewRef?.get()?.evaluateJavascript(
                            "window.onAdCompleted('$source', $earnedCoins, $newTotal);",
                            null
                        )
                    }

                    _userFeedbackEvent.emit("Awesome! Earned +$earnedCoins coins from AdMob video!")
                }
            },
            onAdDismissed = { earned ->
                if (!earned) {
                    activeWebViewRef?.get()?.post {
                        activeWebViewRef?.get()?.evaluateJavascript(
                            "window.onAdFailed('Ad closed before completion');",
                            null
                        )
                    }
                }
            },
            onAdFailed = { reason ->
                viewModelScope.launch {
                    activeWebViewRef?.get()?.post {
                        activeWebViewRef?.get()?.evaluateJavascript(
                            "window.onAdFailed('$reason');",
                            null
                        )
                    }
                    _userFeedbackEvent.emit(reason)
                }
            }
        )
    }

    fun addGameReward(title: String, coins: Long, type: String) {
        viewModelScope.launch {
            dao.addRewardCoins(title = title, coins = coins, type = type)
            when (type) {
                "LUCKY_SPIN" -> dao.incrementTaskProgress("SPIN")
                "SCRATCH_CARD" -> dao.incrementTaskProgress("SCRATCH")
                "CHEST_1", "CHEST_2", "CHEST_3" -> dao.incrementTaskProgress("CHEST")
            }
        }
    }

    fun claimDailyCheckIn() {
        viewModelScope.launch {
            val wallet = dao.getWalletDirect() ?: Wallet()
            val currentDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

            if (wallet.lastCheckInEpochDay == currentDay) {
                _userFeedbackEvent.emit("You already claimed today's check-in bonus! Come back tomorrow.")
                return@launch
            }

            val nextStreak = if (wallet.lastCheckInEpochDay == currentDay - 1) {
                wallet.currentStreak + 1
            } else {
                1
            }

            val rewardCoins = when (nextStreak % 7) {
                1 -> 100L
                2 -> 150L
                3 -> 200L
                4 -> 250L
                5 -> 300L
                6 -> 400L
                else -> 500L
            }

            val updatedWallet = wallet.copy(
                coins = wallet.coins + rewardCoins,
                lifetimeCoins = wallet.lifetimeCoins + rewardCoins,
                currentStreak = nextStreak,
                lastCheckInEpochDay = currentDay
            )

            dao.insertWallet(updatedWallet)
            dao.insertTransaction(
                RewardTransaction(
                    title = "Day $nextStreak Check-In Streak",
                    coins = rewardCoins,
                    type = "DAILY_STREAK"
                )
            )

            _userFeedbackEvent.emit("Claimed Day $nextStreak Bonus: +$rewardCoins coins!")
        }
    }

    fun claimTask(taskId: String) {
        viewModelScope.launch {
            val task = dao.getTaskById(taskId)
            if (task != null && !task.isClaimed && task.currentProgress >= task.targetProgress) {
                dao.updateTask(task.copy(isClaimed = true))
                dao.addRewardCoins(
                    title = "Task Completed: ${task.title}",
                    coins = task.rewardCoins.toLong(),
                    type = "TASK_BONUS"
                )
                _userFeedbackEvent.emit("Claimed task reward: +${task.rewardCoins} coins!")
            }
        }
    }

    fun submitWithdrawal(
        payoutMethod: String,
        destinationAccount: String,
        coinsAmount: Long
    ) {
        viewModelScope.launch {
            val wallet = dao.getWalletDirect() ?: Wallet()
            if (coinsAmount < 5000L) {
                _userFeedbackEvent.emit("Minimum withdrawal is 5,000 coins ($5.00 USD).")
                return@launch
            }
            if (wallet.coins < coinsAmount) {
                _userFeedbackEvent.emit("Insufficient balance. You have ${wallet.coins} coins.")
                return@launch
            }
            if (destinationAccount.isBlank()) {
                _userFeedbackEvent.emit("Please enter your payout account address or email.")
                return@launch
            }

            val usdAmount = coinsAmount / 1000.0
            val updatedWallet = wallet.copy(
                coins = wallet.coins - coinsAmount
            )
            dao.insertWallet(updatedWallet)

            val request = WithdrawalRequest(
                payoutMethod = payoutMethod,
                destinationAccount = destinationAccount.trim(),
                coinsDeducted = coinsAmount,
                amountUsd = usdAmount,
                status = "PENDING"
            )
            dao.insertWithdrawal(request)

            dao.insertTransaction(
                RewardTransaction(
                    title = "Withdrawal to $payoutMethod ($$usdAmount)",
                    coins = -coinsAmount,
                    type = "WITHDRAWAL",
                    status = "PENDING"
                )
            )

            _userFeedbackEvent.emit("Withdrawal request of $$usdAmount USD submitted successfully!")
        }
    }

    fun setTestMode(enabled: Boolean) {
        adMobManager.setTestMode(enabled)
    }

    fun setCustomAdUnitId(id: String) {
        adMobManager.setCustomAdUnitId(id)
    }

    fun setSimulationMode(enabled: Boolean) {
        adMobManager.setSimulationMode(enabled)
    }
}
