package com.moasseum.app

import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
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
import java.util.concurrent.atomic.AtomicInteger
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

    @Test fun homeTabsSwitchAndAiPopupExpandsAndCollapsesAsOneSurface() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Home tab interaction tests are restricted to the isolated QA app." }
        val aiAnalysisClicks = AtomicInteger()

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    HomeScreen(
                        uiState = LedgerUiState(
                            YearMonth.now(),
                            listOf(transactionFor(LocalDate.now(), 12_000L, "FOOD", 92L)),
                            budgetAmount = 250_000,
                        ),
                        aiAnalysisState = SpendingAnalysisState.Idle,
                        onGenerateAiAnalysis = { aiAnalysisClicks.incrementAndGet() },
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
                    )
                }
            }
        }

        composeRule.onNodeWithTag("home_tab_summary").assertIsSelected()
        composeRule.onNodeWithTag("home_tab_insights").performClick().assertIsSelected()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_tab_content_insights").assertExists()
        composeRule.onNodeWithText("예산 주기 요약").assertExists()
        composeRule.onNodeWithTag("home_tab_content_summary").assertDoesNotExist()

        composeRule.onNodeWithTag("home_tab_report").performClick().assertIsSelected()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_tab_content_report").assertExists()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("home_ai_assistant_button").performClick()
        composeRule.mainClock.advanceTimeBy(120)
        composeRule.onNodeWithTag("home_ai_launch_transition").assertExists()
        composeRule.onAllNodesWithTag("home_ai_assistant_sheet").assertCountEquals(1)
        val morphBytes = composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            ByteArrayOutputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
                output.toByteArray().also { bitmap.recycle() }
            }
        }
        assertTrue("The AI button-to-popup transition screenshot was not created.", morphBytes.size > 10_000)
        Base64.encodeToString(morphBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_HOME_AI_MORPH|$index|$chunk")
        }
        composeRule.mainClock.advanceTimeBy(420)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        captureAndLogRoot("MSS_HOME_AI_EXPANDED")
        composeRule.onNodeWithTag("home_ai_assistant_sheet").assertExists()
        composeRule.onAllNodesWithTag("home_ai_assistant_sheet").assertCountEquals(1)
        composeRule.onNodeWithTag("home_ai_sheet_content").assertExists()
        composeRule.onNodeWithTag("home_ai_launch_orb").assertExists()
        composeRule.onNodeWithText("AI 소비 코치").assertExists()
        composeRule.onNodeWithText("AI 분석 만들기").assertExists()
        val lastSuggestionBounds = composeRule.onNodeWithText("가장 많이 쓴 항목은?").getBoundsInRoot()
        val sheetBounds = composeRule.onNodeWithTag("home_ai_sheet_content").getBoundsInRoot()
        assertTrue("AI prompt suggestions should wrap without clipping at the sheet edge.", lastSuggestionBounds.right <= sheetBounds.right)

        val bytes = composeRule.onNodeWithTag("home_ai_sheet_content").captureToImage().asAndroidBitmap().let { bitmap ->
            ByteArrayOutputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
                output.toByteArray().also { bitmap.recycle() }
            }
        }
        assertTrue("The AI popup screenshot was not created.", bytes.size > 10_000)
        Base64.encodeToString(bytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_HOME_AI_SHEET|$index|$chunk")
        }
        composeRule.onNodeWithText("AI 분석 만들기").performClick()
        composeRule.runOnIdle { assertEquals(1, aiAnalysisClicks.get()) }
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription("AI 창 닫기").performClick()
        composeRule.mainClock.advanceTimeBy(140)
        composeRule.onNodeWithTag("home_ai_assistant_sheet").assertExists()
        captureAndLogRoot("MSS_HOME_AI_COLLAPSE")
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_ai_assistant_sheet").assertDoesNotExist()

        composeRule.onNodeWithTag("home_ai_assistant_button").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("home_ai_assistant_sheet").assertCountEquals(1)
        composeRule.onNodeWithTag("home_ai_popup_scrim").performTouchInput {
            click(Offset(2f, 2f))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_ai_assistant_sheet").assertDoesNotExist()
    }

    private fun captureAndLogRoot(tag: String) {
        val bytes = composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            ByteArrayOutputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
                output.toByteArray().also { bitmap.recycle() }
            }
        }
        assertTrue("The $tag screenshot was not created.", bytes.size > 10_000)
        Base64.encodeToString(bytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "$tag|$index|$chunk")
        }
    }

    @Test fun expandedFoldDashboardPairsCategoryAndRecentCardsAtEqualWidths() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Expanded dashboard tests are restricted to the isolated QA app." }
        val today = LocalDate.now()
        val transaction = transactionFor(today, 12_000L, "FOOD", 91L)
        val expandedConfiguration = Configuration().apply {
            screenWidthDp = 690
            screenHeightDp = 900
        }

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(LocalConfiguration provides expandedConfiguration) {
                        Box(Modifier.requiredWidth(690.dp).fillMaxHeight()) {
                            HomeScreen(
                                uiState = LedgerUiState(YearMonth.now(), listOf(transaction), budgetAmount = 250_000),
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
                                homeDashboardCards = HomeDashboardCards.available,
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("home_summary_secondary_cards_wide").performScrollTo().assertExists()
        composeRule.onNodeWithTag("home_category_spending_card").assertExists()
        composeRule.onNodeWithTag("home_recent_transactions_card").assertExists()
        val categoryBounds = composeRule.onNodeWithTag("home_category_spending_card").getUnclippedBoundsInRoot()
        val recentBounds = composeRule.onNodeWithTag("home_recent_transactions_card").getUnclippedBoundsInRoot()
        val categoryWidth = categoryBounds.right - categoryBounds.left
        val recentWidth = recentBounds.right - recentBounds.left
        assertTrue("Both inner-fold dashboard columns must have usable width.", categoryWidth > 300.dp && recentWidth > 300.dp)
        assertTrue(
            "Category and recent cards should share the available width evenly.",
            kotlin.math.abs((categoryWidth - recentWidth).value) < 12f,
        )
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
