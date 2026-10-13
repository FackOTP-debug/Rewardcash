package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RewardTransaction
import com.example.data.WithdrawalRequest
import com.example.viewmodel.RewardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletScreen(
    viewModel: RewardViewModel,
    modifier: Modifier = Modifier
) {
    val wallet by viewModel.walletState.collectAsState()
    val withdrawals by viewModel.withdrawalsState.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()

    var selectedMethod by remember { mutableStateOf("PayPal") }
    var selectedCoinsAmount by remember { mutableLongStateOf(5000L) }
    var accountAddress by remember { mutableStateOf("") }

    val userCoins = wallet?.coins ?: 500L
    val minThresholdCoins = 5000L
    val progressToMin = (userCoins.toFloat() / minThresholdCoins).coerceIn(0f, 1f)
    val hasEnoughCoins = userCoins >= selectedCoinsAmount

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("wallet_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Overview Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_overview_card"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Available Redeemable Cash",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$${String.format("%.2f", wallet?.usdValue ?: 0.50)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00C853)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "USD (${userCoins.toLocaleString()} Coins)",
                            color = Color(0xFFFFB300),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress to $5.00 cashout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Threshold: $5.00 (5,000 coins)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${(progressToMin * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00C853)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressToMin },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00C853),
                        trackColor = Color(0xFF334155)
                    )
                }
            }
        }

        // Cash Out Form Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashout_form_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Request Withdrawal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Transfer coins directly to your verified payout account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "1. Select Payout Method",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val methods = listOf(
                        "PayPal" to "🅿️",
                        "Amazon Gift" to "🎁",
                        "Google Play" to "🎮",
                        "Crypto USDT" to "₮"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(methods) { (method, emoji) ->
                            val isSelected = selectedMethod == method
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFFFB300).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB300)) else null,
                                modifier = Modifier
                                    .clickable { selectedMethod = method }
                                    .testTag("method_${method.replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = method,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "2. Select Cash-Out Amount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val amounts = listOf(
                        5000L to "$5.00",
                        10000L to "$10.00",
                        25000L to "$25.00",
                        50000L to "$50.00"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amounts.forEach { (coinAmount, usdStr) ->
                            val isSelected = selectedCoinsAmount == coinAmount
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF00C853).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00C853)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCoinsAmount = coinAmount }
                                    .testTag("amount_${coinAmount}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = usdStr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${coinAmount / 1000}k 🪙",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "3. Recipient Account / Email",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = accountAddress,
                        onValueChange = { accountAddress = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_address_input"),
                        placeholder = {
                            Text(
                                if (selectedMethod == "Crypto USDT") "Enter USDT Address (TRC20)"
                                else "Enter your payout email address"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.submitWithdrawal(
                                payoutMethod = selectedMethod,
                                destinationAccount = accountAddress,
                                coinsAmount = selectedCoinsAmount
                            )
                            accountAddress = ""
                        },
                        enabled = hasEnoughCoins && accountAddress.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_withdrawal_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        )
                    ) {
                        Text(
                            text = if (hasEnoughCoins) "Redeem $$${selectedCoinsAmount / 1000.0} USD Now"
                            else "Need ${(selectedCoinsAmount - userCoins).toLocaleString()} More Coins",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Payout Requests Section
        if (withdrawals.isNotEmpty()) {
            item {
                Text(
                    text = "Withdrawal Requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(withdrawals) { req ->
                WithdrawalItemCard(request = req)
            }
        }

        // Transaction History Section
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(transactions) { tx ->
            TransactionItemRow(transaction = tx)
        }
    }
}

@Composable
fun WithdrawalItemCard(request: WithdrawalRequest) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("withdrawal_item_${request.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${request.payoutMethod} - $${String.format("%.2f", request.amountUsd)} USD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "To: ${request.destinationAccount}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(request.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (request.status == "APPROVED") Color(0xFF00C853).copy(alpha = 0.15f)
                else Color(0xFFFFB300).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (request.status == "APPROVED") Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (request.status == "APPROVED") Color(0xFF00C853) else Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = request.status,
                        color = if (request.status == "APPROVED") Color(0xFF00C853) else Color(0xFFFFB300),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(transaction: RewardTransaction) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_${transaction.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (transaction.type) {
                    "REWARDED_AD" -> "🎬"
                    "LUCKY_SPIN" -> "🎡"
                    "SCRATCH_CARD" -> "🎴"
                    "CHEST_BONUS" -> "🎁"
                    "DAILY_STREAK" -> "🔥"
                    "WITHDRAWAL" -> "💸"
                    else -> "🪙"
                }
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = transaction.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formatTimestamp(transaction.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isPositive = transaction.coins > 0
            Text(
                text = if (isPositive) "+${transaction.coins}" else "${transaction.coins}",
                color = if (isPositive) Color(0xFF00C853) else Color(0xFFFF5252),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

fun Long.toLocaleString(): String {
    return java.text.NumberFormat.getNumberInstance(Locale.US).format(this)
}

fun formatTimestamp(epochMillis: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(epochMillis))
}
