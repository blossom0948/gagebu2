package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.padding
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.mutableStateOf
import com.moasseum.app.data.ManageSettingSearchResult
import com.moasseum.app.ui.screens.ManageNaturalSearch
import com.moasseum.app.ui.screens.ManageSearchEntry
import com.moasseum.app.ui.screens.ManageSearchCatalog
import com.moasseum.app.ui.theme.MoasseumTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManageNaturalSearchInteractionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun selectingAResultKeepsSearchOpenUntilExplicitOpenAction() {
        var openedCount = 0
        var aiSearchCount = 0
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ManageNaturalSearch(
                        entries = listOf(
                            ManageSearchEntry(
                                id = "budget",
                                title = "월 예산 설정",
                                description = "목표와 이번 예산 기간의 사용액",
                                aliases = listOf("생활비", "얼마 남았어"),
                                onSelect = { openedCount++ },
                            ),
                            ManageSearchEntry(
                                id = "category-budgets",
                                title = "카테고리별 예산",
                                description = "식비·교통비 등 항목마다 한도 설정",
                                aliases = listOf("예산", "식비 한도", "카테고리 한도"),
                                onSelect = {},
                            ),
                        ),
                        aiAvailable = true,
                        onAiSearch = { _, _ ->
                            aiSearchCount++
                            Result.success(ManageSettingSearchResult(listOf("budget", "category-budgets"), "이번 달 생활비 예산 잔액 확인"))
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("manage_natural_search_input").performTextInput("생활비")
        composeRule.onNodeWithText("월 예산 설정").performClick()
        composeRule.onNodeWithText("설정 열기").assertExists()
        assertEquals(0, openedCount)
        assertEquals(0, aiSearchCount)

        composeRule.onNodeWithText("AI로 의미 검색").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { aiSearchCount == 1 }
        composeRule.onNodeWithText("AI 검색 완료").assertIsNotEnabled().performClick()
        assertEquals(1, aiSearchCount)
        composeRule.onNodeWithText("이렇게 이해했어요 · 이번 달 생활비 예산 잔액 확인").assertExists()
        composeRule.waitForIdle()
        val fullScreenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val contentScreenshot = Bitmap.createBitmap(
            fullScreenshot,
            0,
            0,
            fullScreenshot.width,
            (fullScreenshot.height * 0.48f).toInt(),
        )
        val bytes = ByteArrayOutputStream().use { output ->
            contentScreenshot.compress(Bitmap.CompressFormat.JPEG, 84, output)
            output.toByteArray()
        }
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        base64.chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_MANAGE_SCREEN|$index|$chunk")
        }
        composeRule.onNodeWithText("설정 열기").performClick()
        assertEquals(1, openedCount)
    }

    @Test fun localResultsRemainUsableWhenAiSearchFails() {
        var openedCount = 0
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                ManageNaturalSearch(
                    entries = listOf(
                        ManageSearchEntry("budget", "월 예산 설정", "목표와 사용액", listOf("생활비", "얼마 남았어")) { openedCount++ },
                    ),
                    aiAvailable = true,
                    onAiSearch = { _, _ -> Result.failure(IllegalStateException("offline")) },
                )
            }
        }

        composeRule.onNodeWithTag("manage_natural_search_input").performTextInput("생활비가 얼마나 남았어?")
        composeRule.onNodeWithText("AI로 의미 검색").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("AI 연결을 완료하지 못해 키워드 결과를 보여줘요.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("월 예산 설정").assertExists()
        composeRule.onNodeWithText("월 예산 설정").performClick()
        composeRule.onNodeWithText("설정 열기").performClick()
        assertEquals(1, openedCount)
    }

    @Test fun localSearchWorksWithoutLoginAndAiRemainsDisabled() {
        var aiSearchCount = 0
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                ManageNaturalSearch(
                    entries = listOf(
                        ManageSearchEntry("budget", "월 예산 설정", "목표와 사용액", listOf("생활비")) {},
                    ),
                    aiAvailable = false,
                    onAiSearch = { _, _ ->
                        aiSearchCount++
                        Result.failure(IllegalStateException("must remain disabled"))
                    },
                )
            }
        }

        composeRule.onNodeWithTag("manage_natural_search_input").performTextInput("생활비")
        composeRule.onNodeWithText("AI 검색은 로그인 후 사용 가능").assertIsNotEnabled().performClick()
        composeRule.onNodeWithText("월 예산 설정").assertExists()
        assertEquals(0, aiSearchCount)
    }

    @Test fun directActionsAreClearlyNamedAndWaitForExplicitConfirmation() {
        var rolloverEnabled = false
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                ManageNaturalSearch(
                    entries = listOf(
                        ManageSearchEntry("rollover", "남은 예산 다음 달 이월", "현재 꺼짐", listOf("이월"), "이월 켜기") {
                            rolloverEnabled = true
                        },
                    ),
                    aiAvailable = false,
                    onAiSearch = { _, _ -> Result.failure(IllegalStateException("unused")) },
                )
            }
        }

        composeRule.onNodeWithTag("manage_natural_search_input").performTextInput("이월")
        composeRule.onNodeWithText("남은 예산 다음 달 이월").performClick()
        composeRule.onNodeWithText("이월 켜기").assertExists()
        assertEquals(false, rolloverEnabled)
        composeRule.onNodeWithText("이월 켜기").performClick()
        assertEquals(true, rolloverEnabled)
    }

    @Test fun recentFeatureReopensItsSearchResultAndRemembersOnlyAfterAction() {
        val recentIds = mutableStateOf(listOf("budget"))
        var rememberedId: String? = null
        var openedCount = 0
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ManageNaturalSearch(
                        entries = listOf(ManageSearchEntry("budget", "월 예산 설정", "목표와 사용액", listOf("생활비"), "예산 수정") { openedCount++ }),
                        aiAvailable = false,
                        onAiSearch = { _, _ -> Result.failure(IllegalStateException("unused")) },
                        recentIds = recentIds.value,
                        onRememberSelected = { rememberedId = it },
                        onClearRecent = { recentIds.value = emptyList() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 30.dp),
                    )
                }
            }
        }

        composeRule.onNodeWithText("최근 찾은 메뉴").assertExists()
        assertEquals(0, composeRule.onAllNodesWithText("자주 찾는 검색").fetchSemanticsNodes().size)
        composeRule.waitForIdle()
        val fullScreenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val screenshotCrop = Bitmap.createBitmap(
            fullScreenshot,
            0,
            0,
            fullScreenshot.width,
            (fullScreenshot.height * 0.22f).toInt().coerceAtLeast(1),
        )
        val captureDirectory = requireNotNull(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("captures"),
        )
        captureDirectory.mkdirs()
        val captureFile = File(captureDirectory, "manage_recent_search.jpg")
        captureFile.outputStream().use { output -> screenshotCrop.compress(Bitmap.CompressFormat.JPEG, 90, output) }
        Log.i("MOASSEUM_CAPTURE", "MANAGE_RECENT_SCREEN|${captureFile.absolutePath}")
        composeRule.onNodeWithTag("manage_recent_budget").performClick()
        composeRule.onNodeWithText("예산 수정").assertExists()
        assertEquals(null, rememberedId)
        assertEquals(0, openedCount)

        composeRule.onNodeWithText("예산 수정").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { rememberedId == "budget" }
        assertEquals(1, openedCount)
    }

    @Test fun recentFeatureIdsCanBeClearedWithoutSavingSearchText() {
        val recentIds = mutableStateOf(listOf("budget"))
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                ManageNaturalSearch(
                    entries = listOf(ManageSearchEntry("budget", "월 예산 설정", "목표와 사용액", listOf("생활비")) {}),
                    aiAvailable = false,
                    onAiSearch = { _, _ -> Result.failure(IllegalStateException("unused")) },
                    recentIds = recentIds.value,
                    onClearRecent = { recentIds.value = emptyList() },
                )
            }
        }

        composeRule.onNodeWithText("최근 찾은 메뉴").assertExists()
        composeRule.onNodeWithText("지우기").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("최근 찾은 메뉴").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test fun suggestedSearchShowsResultsBeforeAnyFeatureAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Screenshot capture is restricted to the isolated QA app." }
        var openedId: String? = null
        val entries = ManageSearchCatalog.entries.map { descriptor ->
            ManageSearchEntry(
                id = descriptor.id,
                title = descriptor.title,
                description = descriptor.description,
                aliases = descriptor.aliases,
                actionLabel = descriptor.actionLabel,
                onSelect = { openedId = descriptor.id },
            )
        }
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ManageNaturalSearch(
                        entries = entries,
                        aiAvailable = false,
                        onAiSearch = { _, _ -> Result.failure(IllegalStateException("unused")) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 30.dp),
                    )
                }
            }
        }

        composeRule.onNodeWithText("자주 찾는 검색").assertExists()
        composeRule.onNodeWithTag("manage_suggestion_card-usage").assertExists()
        composeRule.waitForIdle()
        val fullScreenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val screenshotCrop = Bitmap.createBitmap(
            fullScreenshot,
            0,
            0,
            fullScreenshot.width,
            (fullScreenshot.height * 0.24f).toInt().coerceAtLeast(1),
        )
        val screenshotBytes = ByteArrayOutputStream().use { output ->
            screenshotCrop.compress(Bitmap.CompressFormat.JPEG, 90, output)
            output.toByteArray()
        }
        Base64.encodeToString(screenshotBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_MANAGE_SUGGESTIONS|$index|$chunk")
        }
        val captureDirectory = requireNotNull(context.getExternalFilesDir("captures"))
        captureDirectory.mkdirs()
        val captureFile = File(captureDirectory, "manage_search_suggestions.jpg")
        captureFile.outputStream().use { output -> screenshotCrop.compress(Bitmap.CompressFormat.JPEG, 90, output) }
        Log.i("MOASSEUM_CAPTURE", "MANAGE_SUGGESTIONS_SCREEN|${captureFile.absolutePath}")

        composeRule.onNodeWithTag("manage_suggestion_card-usage").performClick()
        composeRule.onNodeWithText("결제일 기준 보기").assertExists()
        assertEquals(0, composeRule.onAllNodesWithText("선택한 기능").fetchSemanticsNodes().size)
        assertEquals(null, openedId)

        composeRule.onNodeWithText("결제일 기준 보기").performClick()
        composeRule.onNodeWithText("선택한 기능").assertExists()
        composeRule.onNodeWithText("카드 사용액 보기").performClick()
        assertEquals("card-usage", openedId)
    }
}
