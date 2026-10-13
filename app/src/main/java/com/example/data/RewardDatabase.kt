package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Wallet::class,
        RewardTransaction::class,
        DailyTask::class,
        WithdrawalRequest::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RewardDatabase : RoomDatabase() {
    abstract fun rewardDao(): RewardDao

    companion object {
        @Volatile
        private var INSTANCE: RewardDatabase? = null

        fun getInstance(context: Context): RewardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RewardDatabase::class.java,
                    "reward_cash_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(database: RewardDatabase) {
            val dao = database.rewardDao()
            // Initial Welcome Wallet
            dao.insertWallet(
                Wallet(
                    id = 1,
                    coins = 500L,
                    lifetimeCoins = 500L,
                    currentStreak = 1,
                    lastCheckInEpochDay = 0L
                )
            )
            // Welcome Bonus Transaction
            dao.insertTransaction(
                RewardTransaction(
                    title = "Welcome Bonus",
                    coins = 500L,
                    type = "WELCOME_BONUS"
                )
            )
            // Initial Daily Tasks
            val defaultTasks = listOf(
                DailyTask(
                    id = "task_ad",
                    title = "Watch Rewarded Videos",
                    description = "Watch 3 AdMob rewarded videos today",
                    currentProgress = 0,
                    targetProgress = 3,
                    rewardCoins = 300,
                    iconType = "AD"
                ),
                DailyTask(
                    id = "task_spin",
                    title = "Spin Fortune Wheel",
                    description = "Play Lucky Wheel 3 times in Game Hub",
                    currentProgress = 0,
                    targetProgress = 3,
                    rewardCoins = 150,
                    iconType = "SPIN"
                ),
                DailyTask(
                    id = "task_scratch",
                    title = "Lucky Scratch Cards",
                    description = "Scratch 2 lucky cards in Game Hub",
                    currentProgress = 0,
                    targetProgress = 2,
                    rewardCoins = 150,
                    iconType = "SCRATCH"
                ),
                DailyTask(
                    id = "task_chest",
                    title = "Unlock Mystery Chest",
                    description = "Open the Golden Treasure Chest",
                    currentProgress = 0,
                    targetProgress = 1,
                    rewardCoins = 200,
                    iconType = "CHEST"
                )
            )
            dao.insertTasks(defaultTasks)
        }
    }
}
