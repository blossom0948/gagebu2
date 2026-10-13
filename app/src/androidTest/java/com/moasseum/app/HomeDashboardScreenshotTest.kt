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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.NoSpendChallengeSettings
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.ui.screens.HomeScreen
import com.moasseum.app.ui.theme.MoasseumTheme
import com.moasseum.app.domain.formatWon
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
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

    @Test fun historicalDashboardLabelsItsPeriodAndHidesLiveTodayCards() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Home interaction tests are restricted to the isolated QA app." }

        val month = YearMonth.now().minusMonths(2)
        val state = LedgerUiState(month, budgetAmount = 250_000, budgetPayday = 25)
        val selectedMonth = AtomicReference<YearMonth?>()
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    HomeScreen(
                        uiState = state,
                        aiAnalysisState = SpendingAnalysisState.Idle,
                        onGenerateAiAnalysis = {},
                        aiQuestionState = SpendingQuestionState.Idle,
                        onAskAiQuestion = {},
                        categoryBudgets = emptyMap(),
                        onExportCsv = {},
                        onExportPdf = {},
                        onSelectMonth = { selectedMonth.set(it) },
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
                        homeDashboardCards = HomeDashboardCards.available,
                        noSpendChallenge = NoSpendChallengeSettings(month.atDay(1), goalDays = 7),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(
            "${formatMonth(month)}. 예산 기간 ${state.budgetCycle.label}. 누르면 이번 달로 이동",
        ).assertExists()
        composeRule.onNodeWithText("오늘 지출").assertDoesNotExist()
        composeRule.onNodeWithText("이번 주 지출").assertDoesNotExist()
        composeRule.onNodeWithText("무지출 챌린지").assertDoesNotExist()
        composeRule.onNodeWithText("현재 속도 기간 종료 예상", substring = true).assertDoesNotExist()
        composeRule.waitForIdle()
        val screenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val screenshotBytes = ByteArrayOutputStream().use { output ->
            assertTrue(screenshot.compress(Bitmap.CompressFormat.JPEG, 90, output))
            output.toByteArray()
        }
        screenshot.recycle()
        Base64.encodeToString(screenshotBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_HOME_HISTORICAL_PERIOD|$index|$chunk")
        }
        composeRule.onNodeWithTag("home_month_period_selector").performClick()
        composeRule.runOnIdle { assertEquals(YearMonth.now(), selectedMonth.get()) }
    }

    @Test fun overBudgetHomeShowsTheActualAmountExceeded() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Home interaction tests are restricted to the isolated QA app." }

        val openedManageFeature = AtomicReference<String?>()
        val today = LocalDate.now()
        val transaction = Transaction(
            id = 1L,
            type = TransactionType.EXPENSE,
            amount = 15_000L,
            occurredAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            categoryKey = "FOOD",
            merchant = "QA 식사",
            memo = "",
            paymentMethod = "카드",
            source = "MANUAL",
        )
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    HomeScreen(
                        uiState = LedgerUiState(YearMonth.now(), listOf(transaction), budgetAmount = 10_000L),
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
                        onOpenManageFeature = { openedManageFeature.set(it) },
                        onOpenHistory = {},
                        onOpenTransaction = {},
                        onOpenHelp = {},
                        onOpenNotifications = {},
                        onStartVoiceInput = {},
                        onPickReceipt = {},
                        onTakeReceipt = {},
                        displayName = "모아씀 QA",
                    )
                }
            }
        }

        composeRule.onNodeWithTag("home_budget_progress").assertExists()
        composeRule.onNodeWithText("예산 초과 ${formatWon(5_000L)}").assertExists()
        composeRule.onNodeWithText("남은 예산 ${formatWon(0L)}").assertDoesNotExist()
        composeRule.onNodeWithTag("home_budget_target_action").performClick()
        composeRule.runOnIdle { assertEquals("budget", openedManageFeature.get()) }
    }

    @Test fun calendarMonthCategoryTotalsAreClearlySeparateFromPaydayBudgetCycle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Home interaction tests are restricted to the isolated QA app." }

        // Use a completed calendar month so the fixture stays deterministic
        // even when the real date crosses the payday boundary during a run.
        val month = YearMonth.now().minusMonths(2)
        val transactions = listOf(
            transactionFor(month.minusMonths(1).atDay(28), 4_000L, "FOOD", 21L),
            transactionFor(month.atDay(5), 9_000L, "FOOD", 22L),
            transactionFor(month.atDay(26), 8_000L, "CAFE", 23L),
            transactionFor(month.plusMonths(1).atDay(5), 4_000L, "FOOD", 24L),
        )
        val state = LedgerUiState(month, transactions, budgetAmount = 30_000L, budgetPayday = 25)

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    HomeScreen(
                        uiState = state,
                        aiAnalysisState = SpendingAnalysisState.Idle,
                        onGenerateAiAnalysis = {},
                        aiQuestionState = SpendingQuestionState.Idle,
                        onAskAiQuestion = {},
                        categoryBudgets = mapOf("FOOD" to 20_000L),
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
                        homeDashboardCards = HomeDashboardCards.available,
                    )
                }
            }
        }

        composeRule.onNodeWithText("예산 기간 · ${state.budgetCycle.label}").assertExists()
        composeRule.onNodeWithText(formatWon(12_000L)).assertExists()
        composeRule.onNodeWithText("${formatMonth(month)} 카테고리 TOP").assertExists()
        composeRule.onNodeWithText("달력 월 기준").assertExists()
        composeRule.onNodeWithText("식비 월 한도 ${formatWon(20_000L)} 중 45% 사용").assertExists()
        composeRule.onNodeWithText(formatWon(9_000L)).assertExists()
        composeRule.onNodeWithText(formatWon(8_000L)).assertExists()
    }

    private fun transactionFor(date: LocalDate, amount: Long, category: String, id: Long) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = amount,
        occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        categoryKey = category,
        merchant = "QA 거래",
        memo = "",
        paymentMethod = "카드",
        source = "MANUAL",
    )
}
