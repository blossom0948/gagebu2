package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.ui.screens.BudgetDialog
import com.moasseum.app.ui.theme.MoasseumTheme
import java.io.ByteArrayOutputStream
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManageBudgetDialogScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun quickBudgetPresetsUpdateTheAmountAndRemainSelectable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Budget screenshot capture is restricted to the isolated QA app." }
        val saved = AtomicReference<String?>(null)
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                var showDialog by remember { mutableStateOf(true) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (showDialog) {
                            BudgetDialog(
                                month = YearMonth.of(2026, 10),
                                initialValue = "",
                                onDismiss = { showDialog = false },
                                onSave = { saved.set(it); showDialog = false },
                            )
                        }
                    }
                }
            }
        }

        listOf("10만", "30만", "50만", "100만", "300만").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeRule.onNodeWithText("30만").performClick()
        composeRule.onNodeWithText("30만").assertIsSelected()
        composeRule.waitForIdle()
        val bitmap = composeRule.onNode(isDialog()).captureToImage().asAndroidBitmap()
        val imageBytes = ByteArrayOutputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
            output.toByteArray()
        }
        bitmap.recycle()
        assertTrue("The budget preset dialog capture was not created.", imageBytes.size > 10_000)
        Base64.encodeToString(imageBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_BUDGET_DIALOG|$index|$chunk")
        }

        composeRule.onNodeWithText("저장").performClick()
        composeRule.waitUntil(5_000) { saved.get() != null }
        composeRule.waitForIdle()
        assertEquals("300000", saved.get())
    }
}
