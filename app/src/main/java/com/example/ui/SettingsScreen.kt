package com.example.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admob.AdMobManager
import com.example.viewmodel.RewardViewModel

@Composable
fun SettingsScreen(
    viewModel: RewardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isTestMode by viewModel.isTestMode.collectAsState()
    val customUnitId by viewModel.customAdUnitId.collectAsState()
    val isSimulation by viewModel.isSimulationEnabled.collectAsState()
    val totalAdsWatched by viewModel.totalAdsWatched.collectAsState()
    val adStatus by viewModel.adStatus.collectAsState()

    var customInput by remember(customUnitId) { mutableStateOf(customUnitId) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "AdMob & App Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage Google Mobile Ads IDs, test mode, and compliance rules",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live Diagnostics Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admob_diagnostics_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AdMob Engine Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        AdStatusBadge(status = adStatus)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DiagnosticRow("Total Ads Watched", "$totalAdsWatched videos")
                    DiagnosticRow("Active Unit ID", if (isTestMode) "Google Sample Test Unit" else customUnitId.ifEmpty { "Sample Unit (Fallback)" })
                    DiagnosticRow("App ID in Manifest", "ca-app-pub-3940256099942544~3347511713")
                    DiagnosticRow("Cooldown Setting", "${AdMobManager.COOLDOWN_SECONDS}s between ads")

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.adMobManager.preloadRewardedAd(force = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preload Ad", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (activity != null) {
                                    viewModel.watchRewardedAd(
                                        activity = activity,
                                        source = "SETTINGS_TEST",
                                        bonusCoins = 100
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB300),
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Text("Test Show Ad", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Test Mode & Custom Unit ID Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admob_config_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Ad Unit ID Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Test mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Google Test Ads",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Recommended during development and verification to prevent invalid clicks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTestMode,
                            onCheckedChange = { viewModel.setTestMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00C853),
                                checkedTrackColor = Color(0xFF00C853).copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulation mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sandbox Simulation Mode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Simulates instant rewards if network is restricted or offline",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSimulation,
                            onCheckedChange = { viewModel.setSimulationMode(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Custom Production Rewarded Ad Unit ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_ad_unit_input"),
                        placeholder = { Text("ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.setCustomAdUnitId(customInput) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Apply Ad Unit ID")
                    }
                }
            }
        }

        // Compliance & Policy Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compliance_card"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AdMob Policy Compliance",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PolicyBullet("Clear Reward Value", "Users are explicitly informed before opting into any rewarded video ad.")
                    PolicyBullet("Anti-Abuse Cooldown", "15-second minimum interval between ad requests prevents accidental spam.")
                    PolicyBullet("No Forced Ads", "Rewarded ads are strictly user-initiated. No intrusive surprise popups.")
                    PolicyBullet("Seamless WebView Bridge", "Native @JavascriptInterface verifies ad completion before crediting web games.")
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PolicyBullet(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "• $title", color = Color(0xFF00C853), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = desc, color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp))
    }
}
