package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey
    val id: Int = 1,
    val coins: Long = 250L,
    val lifetimeCoins: Long = 250L,
    val currentStreak: Int = 1,
    val lastCheckInEpochDay: Long = 0L,
    val adsWatchedToday: Int = 0,
    val lastAdWatchTimestamp: Long = 0L
) {
    val usdValue: Double
        get() = coins / 1000.0 // 1,000 coins = $1.00 USD
}

@Entity(tableName = "transactions")
data class RewardTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val coins: Long,
    val type: String, // "REWARDED_AD", "SPIN_WHEEL", "SCRATCH_CARD", "CHEST_BONUS", "DAILY_STREAK", "TASK_BONUS", "WITHDRAWAL"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED" // "COMPLETED", "PENDING"
)

@Entity(tableName = "daily_tasks")
data class DailyTask(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val rewardCoins: Int,
    val isClaimed: Boolean = false,
    val iconType: String = "AD" // "AD", "SPIN", "SCRATCH", "CHEST", "CHECKIN"
)

@Entity(tableName = "withdrawals")
data class WithdrawalRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val payoutMethod: String, // "PayPal", "Amazon Gift Card", "Google Play Card", "Crypto USDT"
    val destinationAccount: String,
    val coinsDeducted: Long,
    val amountUsd: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // "PENDING", "APPROVED"
)
