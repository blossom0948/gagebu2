package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.ui.screens.HomeScreen
import com.moasseum.app.ui.theme.MoasseumTheme
import java.time.YearMonth
import java.time.LocalDate
import java.time.ZoneId
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeReportNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun reportCanNavigateMonthsAndOpenBudgetSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Report interaction tests are restricted to the isolated QA app." }
        var month by mutableStateOf(YearMonth.of(2026, 10))
        val requestedFeature = AtomicReference<String?>(null)

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    HomeScreen(
                        uiState = LedgerUiState(month),
                        aiAnalysisState = SpendingAnalysisState.Idle,
                        onGenerateAiAnalysis = {},
                        aiQuestionState = SpendingQuestionState.Idle,
                        onAskAiQuestion = {},
                        categoryBudgets = emptyMap(),
                        onExportCsv = {},
                        onExportPdf = {},
                        onSelectMonth = { month = it },
                        onAdd = {},
                        onOpenManage = {},
                        onOpenManageFeature = { requestedFeature.set(it) },
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

        composeRule.onNodeWithText("리포트").performClick()
        composeRule.onNodeWithTag("report_month_label").assertExists()
        composeRule.onNodeWithText("2026년 10월").assertExists()
        composeRule.onNodeWithTag("report_set_budget").assertExists()

        composeRule.onNodeWithTag("report_previous_month").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2026년 9월").assertExists()
        composeRule.onNodeWithTag("report_next_month").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2026년 10월").assertExists()

        composeRule.onNodeWithTag("report_set_budget").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals("budget", requestedFeature.get()) }
    }

    @Test fun reportShowsBudgetPaceAndHidesTheSetupPromptWhenBudgetExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Report interaction tests are restricted to the isolated QA app." }
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    HomeScreen(
                        uiState = LedgerUiState(
                            YearMonth.of(2026, 10),
                            transactions = listOf(
                                Transaction(
                                    id = 1,
                                    type = TransactionType.EXPENSE,
                                    amount = 120_000,
                                    occurredAt = LocalDate.of(2026, 10, 10).atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli(),
                                    categoryKey = "FOOD",
                                    merchant = "QA 예시 거래",
                                    memo = "",
                                    paymentMethod = "체크카드",
                                    source = "QA",
                                ),
                                Transaction(
                                    id = 2,
                                    type = TransactionType.EXPENSE,
                                    amount = 30_000,
                                    occurredAt = LocalDate.of(2026, 9, 28).atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli(),
                                    categoryKey = "FOOD",
                                    merchant = "QA 예산 주기 예시",
                                    memo = "",
                                    paymentMethod = "현금",
                                    source = "QA",
                                ),
                            ),
                            budgetAmount = 300_000,
                            budgetPayday = 25,
                        ),
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

        composeRule.onNodeWithText("리포트").performClick()
        composeRule.onNodeWithTag("report_set_budget").assertDoesNotExist()
        composeRule.onNodeWithText("예산 주기 50% 사용").assertExists()
        composeRule.onNodeWithText("₩150,000 남음").assertExists()
        composeRule.onNodeWithText("달력 월 지출").assertExists()
        composeRule.onNodeWithText("달력 월 수입").assertExists()
        composeRule.onNodeWithText("예산 주기 잔액").assertExists()
        composeRule.onNodeWithText("달력 월 저축률").assertExists()
        composeRule.waitForIdle()
        val screenshot = composeRule.onNodeWithTag("report_month_budget_card").captureToImage().asAndroidBitmap()
        val imageFile = File(context.getExternalFilesDir(null), "moasseum-report-budget-qa.png")
        imageFile.outputStream().use { output ->
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        assertTrue(imageFile.length() > 10_000)
        val capture = ByteArrayOutputStream().use { output ->
            assertTrue(screenshot.compress(Bitmap.CompressFormat.JPEG, 90, output))
            output.toByteArray()
        }
        Base64.encodeToString(capture, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_REPORT_BUDGET|$index|$chunk")
        }
    }

    @Test fun reportShowsAnOverspendWithoutClampingTheAmountText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Report interaction tests are restricted to the isolated QA app." }
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    HomeScreen(
                        uiState = LedgerUiState(
                            YearMonth.of(2026, 10),
                            transactions = listOf(
                                Transaction(
                                    id = 1,
                                    type = TransactionType.EXPENSE,
                                    amount = 320_000,
                                    occurredAt = LocalDate.of(2026, 10, 10).atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli(),
                                    categoryKey = "FOOD",
                                    merchant = "QA 초과 지출",
                                    memo = "",
                                    paymentMethod = "체크카드",
                                    source = "QA",
                                ),
                            ),
                            budgetAmount = 300_000,
                            budgetPayday = 1,
                        ),
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

        composeRule.onNodeWithText("리포트").performClick()
        composeRule.onNodeWithText("예산 주기 107% 사용").assertExists()
        composeRule.onNodeWithText("₩20,000 초과").assertExists()
    }
}
