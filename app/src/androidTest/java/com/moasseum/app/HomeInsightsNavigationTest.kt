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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import com.moasseum.app.domain.formatWon
import com.moasseum.app.ui.screens.HomeScreen
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.ui.theme.MoasseumTheme
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
class HomeInsightsNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun insightsCardsOpenTheirMatchingManageTools() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Home insight capture is restricted to the isolated QA app." }
        val openedFeature = AtomicReference<String?>(null)

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
                        onOpenManageFeature = { openedFeature.set(it) },
                        onOpenHistory = {},
                        onOpenTransaction = {},
                        onOpenHelp = {},
                        onOpenNotifications = {},
                        onStartVoiceInput = {},
                        onPickReceipt = {},
                        onTakeReceipt = {},
                        displayName = "모아씀 QA",
                        homeDashboardCards = HomeDashboardCards.defaults,
                        noSpendChallenge = NoSpendChallengeSettings(),
                        activeFixedCostCount = 2,
                        activeFixedCostMonthlyAmount = 19_800,
                    )
                }
            }
        }

        composeRule.onNodeWithText("인사이트").performClick()
        composeRule.onNodeWithText("바로 쓰는 도구").assertExists()
        composeRule.onNodeWithText("구독·고정비 레이더").assertExists()
        composeRule.onNodeWithText("현재 2개 활성 · 월 ₩19,800").assertExists()
        composeRule.onNodeWithText("연말정산 도우미").assertExists()
        composeRule.onNodeWithText("챌린지 · 구독·고정비 · 연말정산").assertDoesNotExist()

        composeRule.waitForIdle()
        val full = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val visible = Bitmap.createBitmap(full, 0, 0, full.width, minOf(full.height, (full.width / 0.62f).toInt()))
        if (visible !== full) full.recycle()
        val encoded = ByteArrayOutputStream().use { output ->
            assertTrue(visible.compress(Bitmap.CompressFormat.JPEG, 88, output))
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
        visible.recycle()
        encoded.chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_INSIGHTS_TOOLS|$index|$chunk")
        }

        listOf(
            "insight_no_spend_shortcut" to "no-spend",
            "insight_fixed_costs_shortcut" to "fixed-costs",
            "insight_year_end_shortcut" to "year-end",
        ).forEach { (tag, expectedAction) ->
            openedFeature.set(null)
            composeRule.onNodeWithTag(tag).performScrollTo().performClick()
            composeRule.runOnIdle { assertEquals(expectedAction, openedFeature.get()) }
        }
    }

    @Test fun historicalInsightsSeparateCalendarMonthFromCompletedBudgetCycle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Home insight tests are restricted to the isolated QA app." }
        val month = YearMonth.now().minusMonths(2)
        val occurredAt = month.atDay(26).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val state = LedgerUiState(
            month = month,
            transactions = listOf(
                Transaction(501L, TransactionType.EXPENSE, 12_000L, occurredAt, "FOOD", "QA 식사", "", "카드", "QA"),
            ),
            budgetAmount = 50_000L,
            budgetPayday = 25,
        )

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

        composeRule.onNodeWithText("인사이트").performClick()
        composeRule.onNodeWithText("예산 주기 요약").assertExists()
        composeRule.onNodeWithText("${state.budgetCycle.label} 기준").assertExists()
        composeRule.onNodeWithText("${formatMonth(month)} 체크 포인트").assertExists()
        composeRule.onNodeWithText("달력 월 최다 지출").assertExists()
        composeRule.onNodeWithText("예산 기간 지출").assertExists()
        composeRule.onNodeWithText(formatWon(12_000L)).assertExists()
        composeRule.onNodeWithText("달력 월 저축률").assertExists()
        composeRule.onNodeWithText("월말 예상 지출").assertDoesNotExist()
        composeRule.onNodeWithText("이번 달 체크 포인트").assertDoesNotExist()
    }
}
