package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.ui.components.ProfileEditorDialog
import com.moasseum.app.ui.theme.MoasseumTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileEditorScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun renderProfileEditorForReleaseCapture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Screenshot capture is restricted to the isolated QA app." }
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ProfileEditorDialog(
                        initialName = "민지",
                        photoUri = null,
                        onDismiss = {},
                        onSave = {},
                        onChoosePhoto = {},
                        onRemovePhoto = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("사진 추가").assertExists()
        composeRule.waitForIdle()
        val bitmap = composeRule.onNodeWithTag("profile_editor_dialog").captureToImage().asAndroidBitmap()
        val bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 84, output)
            output.toByteArray()
        }
        assertTrue("The Compose screenshot is unexpectedly small.", bytes.size > 10_000)
        Base64.encodeToString(bytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_PROFILE_SCREEN|$index|$chunk")
        }
    }
}
