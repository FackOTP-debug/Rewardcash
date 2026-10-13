package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Wallet
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RewardCash", appName)
  }

  @Test
  fun `verify wallet coin to usd conversion`() {
    val wallet = Wallet(coins = 5000L)
    assertEquals(5.0, wallet.usdValue, 0.001)
  }
}
