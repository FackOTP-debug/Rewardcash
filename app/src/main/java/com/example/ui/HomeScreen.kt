package com.example.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admob.AdStatus
import com.example.data.DailyTask
import com.example.viewmodel.RewardViewModel
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    viewModel: RewardViewModel,
    onNavigateToGameHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val wallet by viewModel.walletState.collectAsState()
    val tasks by viewModel.tasksState.collectAsState()
    val adStatus by viewModel.adStatus.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()

    val currentDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
    val hasCheckedInToday = wallet?.lastCheckInEpochDay == currentDay

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Balance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_balance_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1B263B),
                                    Color(0xFF0D1B2A)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🪙", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Wallet Balance",
                                    color = Color(0xFFCCC2DC),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFB300).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🔥 ${wallet?.currentStreak ?: 1} Day Streak",
                                    color = Color(0xFFFFB300),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${wallet?.coins ?: 500}",
                                color = Color(0xFFFFB300),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "COINS",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "≈ $${String.format("%.2f", wallet?.usdValue ?: 0.50)} USD",
                                color = Color(0xFF00C853),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Lifetime: ${wallet?.lifetimeCoins ?: 500} coins",
                                color = Color(0xFF90A4AE),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Primary AdMob Rewarded Action Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admob_action_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎬 Watch AdMob Rewarded Video",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Earn +100 Coins instantly per view",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        // Live Ad Status Badge
                        AdStatusBadge(status = adStatus)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val isCooldown = adStatus is AdStatus.Cooldown
                    val isLoading = adStatus is AdStatus.Loading
                    val cooldownSec = (adStatus as? AdStatus.Cooldown)?.secondsRemaining ?: 0

                    Button(
                        onClick = {
                            if (activity != null) {
                                viewModel.watchRewardedAd(
                                    activity = activity,
                                    source = "DAILY_REWARD",
                                    bonusCoins = 100
                                )
                            }
                        },
                        enabled = !isCooldown && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("watch_rewarded_ad_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color(0xFF0F172A),
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF64748B)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading AdMob Ad...", fontWeight = FontWeight.Bold)
                        } else if (isCooldown) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cooldown: ${cooldownSec}s remaining", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Watch Video for +100 Coins", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // 7-Day Daily Check-in Streak Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_streak_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Check-In Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Log in every day for growing multipliers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.claimDailyCheckIn() },
                            enabled = !hasCheckedInToday,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("claim_checkin_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00C853)
                            )
                        ) {
                            Text(if (hasCheckedInToday) "Claimed ✓" else "Claim Today")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val streak = wallet?.currentStreak ?: 1
                    val rewards = listOf(100, 150, 200, 250, 300, 400, 500)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(7) { index ->
                            val dayNum = index + 1
                            val isCompleted = dayNum < streak || (dayNum == streak && hasCheckedInToday)
                            val isCurrent = dayNum == streak && !hasCheckedInToday

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isCompleted -> Color(0xFF00C853).copy(alpha = 0.2f)
                                        isCurrent -> Color(0xFFFFB300).copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                ),
                                modifier = Modifier.width(66.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Day $dayNum",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isCompleted) "✓" else "🪙",
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "+${rewards[index]}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) Color(0xFF00C853) else Color(0xFFFFB300)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Web Game Hub Shortcut Banner
        item {
            Card(
                onClick = onNavigateToGameHub,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("game_hub_shortcut_card"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3B1F54)
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎮 Interactive HTML5 Game Hub",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Spin Fortune Wheel, Scratch Cards & Treasure Chests via WebView Bridge!",
                            color = Color(0xFFE9D5FF),
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = onNavigateToGameHub,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA855F7)
                        )
                    ) {
                        Text("Play Now")
                    }
                }
            }
        }

        // Daily Tasks Section
        item {
            Text(
                text = "Daily Quests & Challenges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(tasks) { task ->
            TaskItemCard(
                task = task,
                onClaim = { viewModel.claimTask(task.id) }
            )
        }
    }
}

@Composable
fun AdStatusBadge(status: AdStatus) {
    val (text, color) = when (status) {
        is AdStatus.Ready -> "AdMob Ready" to Color(0xFF00C853)
        is AdStatus.Loading -> "Loading..." to Color(0xFFFFB300)
        is AdStatus.Cooldown -> "Cooldown" to Color(0xFFFF5252)
        is AdStatus.Showing -> "Playing Ad" to Color(0xFF448AFF)
        is AdStatus.Completed -> "Rewarded" to Color(0xFF00C853)
        is AdStatus.Failed -> "Cached / Retry" to Color(0xFFFF9800)
        is AdStatus.Idle -> "Standing By" to Color(0xFF90A4AE)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TaskItemCard(
    task: DailyTask,
    onClaim: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFB300).copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val emoji = when (task.iconType) {
                        "AD" -> "🎬"
                        "SPIN" -> "🎡"
                        "SCRATCH" -> "🎴"
                        "CHEST" -> "🎁"
                        else -> "⭐"
                    }
                    Text(text = emoji, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (task.currentProgress.toFloat() / task.targetProgress).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00C853),
                    trackColor = Color(0xFFE0E0E0)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${task.currentProgress}/${task.targetProgress}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (task.isClaimed) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00C853).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Claimed ✓",
                        color = Color(0xFF00C853),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            } else {
                val canClaim = task.currentProgress >= task.targetProgress
                Button(
                    onClick = onClaim,
                    enabled = canClaim,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("claim_task_${task.id}")
                ) {
                    Text(
                        text = "+${task.rewardCoins}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
