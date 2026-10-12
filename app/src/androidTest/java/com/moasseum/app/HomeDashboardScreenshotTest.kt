package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.ui.screens.HomeScreen
import com.moasseum.app.ui.theme.MoasseumTheme
import java.io.ByteArrayOutputStream
import java.time.YearMonth
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeDashboardScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun defaultDashboardAvoidsDuplicatingTheDailySpendSummary() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Home screenshot capture is restricted to the isolated QA app." }

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    HomeScreen(
                        uiState = LedgerUiState(YearMonth.now(), budgetAmount = 250_000),
                        aiAnalysisState = SpendingAnalysisState.Idle,
                        onGenerateAiAnalysis = {},
                        aiQuestionState = SpendingQuestionState.Idle,
                        onAskAiQuestion = {},
                        categoryBudgets = emptyMap(),
                        onExportCsv = {},
                        onExportPdf = {},
                        onSelectMonth = {},
                        onAdd = {},
                        onOpenManage = {},
                        onOpenHistory = {},
                        onOpenTransaction = {},
                        onOpenHelp = {},
                        onOpenNotifications = {},
                        onStartVoiceInput = {},
                        onPickReceipt = {},
                        onTakeReceipt = {},
                        displayName = "모아씀 QA",
                        homeDashboardCards = HomeDashboardCards.defaults,
                    )
                }
            }
        }

        composeRule.onNodeWithText("오늘 지출").assertExists()
        composeRule.onNodeWithText("오늘 소비가 없어요").assertDoesNotExist()
        composeRule.waitForIdle()
        val full = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val visibleDashboard = Bitmap.createBitmap(
            full,
            0,
            0,
            full.width,
            minOf(full.height, (full.width / 0.62f).toInt()),
        )
        if (visibleDashboard !== full) full.recycle()
        val bytes = ByteArrayOutputStream().use { output ->
            assertTrue(visibleDashboard.compress(Bitmap.CompressFormat.JPEG, 88, output))
            output.toByteArray()
        }
        visibleDashboard.recycle()
        assertTrue("The Fold4 home dashboard capture was not created.", bytes.size > 10_000)
        Base64.encodeToString(bytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_HOME_DASHBOARD|$index|$chunk")
        }
    }
}
