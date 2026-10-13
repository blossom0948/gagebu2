package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.ui.components.AddMode
import com.moasseum.app.ui.components.AddTransactionSheet
import com.moasseum.app.ui.theme.MoasseumTheme
import java.io.File
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class QuickAddSheetInteractionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun quickAddOpensWithoutKeyboardAndModeChangesReachTheRequestedForm() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Quick-add UI tests are restricted to the isolated QA app." }
        val mode = mutableStateOf(AddMode.MENU)
        val shareOnSave = mutableStateOf(false)

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ModalBottomSheet(
                        onDismissRequest = {},
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        Surface(
                            modifier = Modifier.testTag("quick_add_sheet_capture_root"),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            AddTransactionSheet(
                                mode = mode.value,
                                paymentMethods = listOf("토스뱅크 체크카드"),
                                accounts = emptyList(),
                                canShareOnSave = true,
                                shareOnSave = shareOnSave.value,
                                onShareOnSaveChange = { shareOnSave.value = it },
                                onModeChange = { mode.value = it },
                                onDismiss = {},
                                onSave = { _, _, _, _, _, _, _, _, _, _ -> false },
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("quick_add_input").assertIsNotFocused()
        composeRule.onNodeWithText("개인").assertExists()
        composeRule.onNodeWithText("AI").assertExists()

        val screenshot = composeRule.onNodeWithTag("quick_add_sheet_capture_root").captureToImage().asAndroidBitmap()
        val screenshotFile = File(context.getExternalFilesDir("screenshots"), "moasseum-quick-add-qa.png")
        screenshotFile.parentFile?.mkdirs()
        screenshotFile.outputStream().use { assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        val previewBytes = ByteArrayOutputStream().use { output ->
            assertTrue(screenshot.compress(Bitmap.CompressFormat.JPEG, 84, output))
            output.toByteArray()
        }
        screenshot.recycle()
        assertTrue("The isolated quick-add screenshot was not captured.", screenshotFile.length() > 10_000)
        Base64.encodeToString(previewBytes, Base64.NO_WRAP).chunked(1_800).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_QUICK_ADD_QA|$index|$chunk")
        }

        composeRule.onNodeWithText("함께").performClick()
        composeRule.runOnIdle { assertTrue(shareOnSave.value) }
        composeRule.onNodeWithText("개인").performClick()
        composeRule.runOnIdle { assertTrue(!shareOnSave.value) }
        composeRule.onNodeWithText("직접").performClick()
        composeRule.onNodeWithText("저장").assertExists()
        composeRule.onNodeWithText("더 보기").performClick()
        composeRule.onNodeWithText("상세 입력").performClick()
        composeRule.runOnIdle { assertTrue(mode.value == AddMode.DIRECT) }
        composeRule.onNodeWithText("거래 기록").assertExists()
        composeRule.onNodeWithText("방법 바꾸기").performClick()
        composeRule.runOnIdle { assertTrue(mode.value == AddMode.MENU) }
        composeRule.onNodeWithTag("quick_add_input").assertExists()
    }
}
