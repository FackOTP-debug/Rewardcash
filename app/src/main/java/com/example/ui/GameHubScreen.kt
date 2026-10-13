package com.example.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.admob.AdStatus
import com.example.viewmodel.RewardViewModel
import com.example.webview.WebAppInterface

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameHubScreen(
    viewModel: RewardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    val wallet by viewModel.walletState.collectAsState()
    val adStatus by viewModel.adStatus.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isLoadingPage by remember { mutableStateOf(true) }

    // Synchronize native balance to WebView whenever Room wallet changes
    DisposableEffect(wallet?.coins) {
        val coins = wallet?.coins ?: 500L
        val usd = wallet?.usdValue ?: 0.50
        webViewInstance?.post {
            webViewInstance?.evaluateJavascript(
                "if (typeof window.updateNativeBalance === 'function') { window.updateNativeBalance($coins, $usd); }",
                null
            )
        }
        onDispose { }
    }

    // Synchronize AdMob status to WebView whenever AdMob state changes
    DisposableEffect(adStatus) {
        val (text, isReady) = when (adStatus) {
            is AdStatus.Ready -> "AdMob Ready (+Bonus)" to true
            is AdStatus.Loading -> "Ad Loading..." to false
            is AdStatus.Cooldown -> "Ad Cooldown (${(adStatus as AdStatus.Cooldown).secondsRemaining}s)" to false
            is AdStatus.Showing -> "Video Playing" to false
            else -> "AdMob Online" to true
        }
        webViewInstance?.post {
            webViewInstance?.evaluateJavascript(
                "if (typeof window.setAdStatus === 'function') { window.setAdStatus('$text', $isReady); }",
                null
            )
        }
        onDispose { }
    }

    // Handle back button inside WebView
    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    // Register active webview in ViewModel for Ad completed callbacks
    DisposableEffect(webViewInstance) {
        viewModel.registerActiveWebView(webViewInstance)
        onDispose {
            viewModel.registerActiveWebView(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_hub_screen")
    ) {
        // Native Mini Header Bar above WebView
        Surface(
            color = Color(0xFF0F172A),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎮 Game Hub",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFB300).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🪙 ${wallet?.coins ?: 500}",
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdStatusBadge(status = adStatus)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reload Games",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (isLoadingPage && webProgress < 1f) {
            LinearProgressIndicator(
                progress = { webProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFFFFB300),
                trackColor = Color(0xFF1E293B)
            )
        }

        // Android WebView Embedding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0B132B))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = false
                            displayZoomControls = false
                        }

                        // Attach the JavaScript Bridge interface
                        addJavascriptInterface(
                            WebAppInterface(activity, viewModel),
                            "AndroidBridge"
                        )

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress / 100f
                                if (newProgress >= 100) {
                                    isLoadingPage = false
                                }
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoadingPage = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoadingPage = false
                                // Sync initial balance to WebView
                                val c = viewModel.walletState.value?.coins ?: 500L
                                val u = viewModel.walletState.value?.usdValue ?: 0.50
                                evaluateJavascript("window.updateNativeBalance($c, $u);", null)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                isLoadingPage = false
                            }
                        }

                        loadUrl("file:///android_asset/reward_hub/index.html")
                        webViewInstance = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reward_hub_webview")
            )
        }
    }
}
