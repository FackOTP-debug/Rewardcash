package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM wallet WHERE id = 1 LIMIT 1")
    fun getWallet(): Flow<Wallet?>

    @Query("SELECT * FROM wallet WHERE id = 1 LIMIT 1")
    suspend fun getWalletDirect(): Wallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: Wallet)

    @Update
    suspend fun updateWallet(wallet: Wallet)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 50")
    fun getRecentTransactions(): Flow<List<RewardTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: RewardTransaction)

    @Query("SELECT * FROM daily_tasks")
    fun getAllTasks(): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): DailyTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DailyTask>)

    @Update
    suspend fun updateTask(task: DailyTask)

    @Query("SELECT * FROM withdrawals ORDER BY timestamp DESC")
    fun getWithdrawals(): Flow<List<WithdrawalRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalRequest)

    @Transaction
    suspend fun addRewardCoins(title: String, coins: Long, type: String) {
        val currentWallet = getWalletDirect() ?: Wallet()
        val updatedWallet = currentWallet.copy(
            coins = currentWallet.coins + coins,
            lifetimeCoins = currentWallet.lifetimeCoins + coins
        )
        insertWallet(updatedWallet)
        insertTransaction(
            RewardTransaction(
                title = title,
                coins = coins,
                type = type
            )
        )
    }

    @Transaction
    suspend fun incrementTaskProgress(taskType: String, delta: Int = 1) {
        val tasks = listOf("task_ad", "task_spin", "task_scratch", "task_chest")
        for (id in tasks) {
            val task = getTaskById(id)
            if (task != null && !task.isClaimed && task.iconType.equals(taskType, ignoreCase = true)) {
                val newProgress = (task.currentProgress + delta).coerceAtMost(task.targetProgress)
                updateTask(task.copy(currentProgress = newProgress))
            }
        }
    }
}
