package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moasseum.app.data.ManageSettingSearchResult
import com.moasseum.app.ui.screens.ManageNaturalSearch
import com.moasseum.app.ui.screens.ManageSearchEntry
import com.moasseum.app.ui.screens.ManageSearchLauncher
import com.moasseum.app.ui.screens.ManageSearchPageHeader
import com.moasseum.app.ui.theme.MoasseumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class ManageSearchNavigationInteractionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun searchOpensAnIsolatedResultsScreenAndBackKeepsTheQuery() {
        val pageOpen = mutableStateOf(false)
        val query = mutableStateOf("")
        val entries = listOf(
            ManageSearchEntry("budget", "월 예산 설정", "목표와 사용액", listOf("생활비")) {},
        )

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (pageOpen.value) {
                            Column {
                                ManageSearchPageHeader(onBack = { pageOpen.value = false })
                                ManageNaturalSearch(
                                    entries = entries,
                                    aiAvailable = false,
                                    onAiSearch = { _, _ -> Result.success(ManageSettingSearchResult(emptyList(), "")) },
                                    queryValue = query.value,
                                    onQueryValueChange = { query.value = it },
                                    requestFocusOnStart = true,
                                )
                            }
                        } else {
                            Column {
                                Text("원래 관리 메뉴")
                                ManageSearchLauncher(onClick = { pageOpen.value = true })
                            }
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("원래 관리 메뉴").assertExists()
        composeRule.onNodeWithTag("manage_search_launcher").performClick()
        composeRule.onNodeWithText("원래 관리 메뉴").assertDoesNotExist()
        composeRule.onNodeWithText("설정 검색").assertExists()
        composeRule.onNodeWithTag("manage_natural_search_input").performTextInput("생활비")
        composeRule.onNodeWithText("월 예산 설정").assertExists()
        composeRule.waitForIdle()
        val fullScreenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val screenshot = Bitmap.createBitmap(
            fullScreenshot,
            0,
            0,
            fullScreenshot.width,
            minOf(fullScreenshot.height, 760),
        )
        fullScreenshot.recycle()
        val screenshotBytes = ByteArrayOutputStream().use { output ->
            check(screenshot.compress(android.graphics.Bitmap.CompressFormat.JPEG, 84, output))
            output.toByteArray()
        }
        screenshot.recycle()
        Base64.encodeToString(screenshotBytes, Base64.NO_WRAP).chunked(1_800).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_MANAGE_SEARCH_QA|$index|$chunk")
        }
        composeRule.onNodeWithContentDescription("관리로 돌아가기").performClick()
        composeRule.onNodeWithText("원래 관리 메뉴").assertExists()
        composeRule.onNodeWithTag("manage_search_launcher").performClick()
        composeRule.onNodeWithTag("manage_natural_search_input").assertExists()
        composeRule.onNodeWithText("월 예산 설정").assertExists()
    }
}
