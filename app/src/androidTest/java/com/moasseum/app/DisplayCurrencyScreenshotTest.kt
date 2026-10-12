package com.moasseum.app

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.domain.DisplayCurrencySetting
import com.moasseum.app.ui.screens.DisplayCurrencyDialog
import com.moasseum.app.ui.theme.MoasseumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DisplayCurrencyScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun captureActualCurrencyPickerForReleaseNotes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Release capture is restricted to the isolated QA app." }
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    DisplayCurrencyDialog(
                        current = DisplayCurrencySetting(code = "USD"),
                        onDismiss = {},
                        onSave = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag("display_currency_settings_dialog").assertExists()
        composeRule.onNodeWithText("미국 달러").assertExists()
        composeRule.waitForIdle()
        val bitmap = composeRule.onNodeWithTag("display_currency_settings_dialog")
            .captureToImage().asAndroidBitmap()
        val file = File(context.getExternalFilesDir(null), "moasseum-display-currency-qa.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(file.length() > 10_000) { "The currency picker screenshot was not written." }
        if (InstrumentationRegistry.getArguments().getString("captureHold") == "true") {
            Thread.sleep(30_000)
        }
    }
}
