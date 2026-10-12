package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.ui.screens.ManageExpandableSection
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.ui.theme.MoasseumTheme
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManageExpandableSectionScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun advancedSettingsStayReachableButDoNotCrowdTheManageMenu() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Manage section capture is restricted to the isolated QA app." }
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    Column(Modifier.padding(14.dp)) {
                        var expanded by remember { mutableStateOf(false) }
                        ManageExpandableSection(
                            expanded = expanded,
                            onToggle = { expanded = !expanded },
                            reduceMotion = true,
                        ) {
                            Surface(color = LocalFinanceColors.current.surfaceRaised) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("전체 백업 내보내기", style = MaterialTheme.typography.bodyMedium)
                                    Text("로그인·계정", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("앱과 데이터").assertExists()
        composeRule.onNodeWithText("백업 · 계정 · 화면 설정").assertExists()
        composeRule.onNodeWithText("전체 백업 내보내기").assertDoesNotExist()
        composeRule.waitForIdle()
        val full = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val cropped = Bitmap.createBitmap(full, 0, 0, full.width, minOf(full.height, 250))
        val imageBytes = ByteArrayOutputStream().use { output ->
            assertTrue(cropped.compress(Bitmap.CompressFormat.JPEG, 90, output))
            output.toByteArray()
        }
        assertTrue(imageBytes.size > 5_000)
        Base64.encodeToString(imageBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_MANAGE_APP_DATA|$index|$chunk")
        }
        cropped.recycle()
        full.recycle()

        composeRule.onNodeWithTag("manage_app_data_toggle").performClick()
        composeRule.onNodeWithText("전체 백업 내보내기").assertExists()
        composeRule.onNodeWithText("로그인·계정").assertExists()
        composeRule.onNodeWithTag("manage_app_data_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("전체 백업 내보내기").assertDoesNotExist()
    }
}
