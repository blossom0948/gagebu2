package com.moasseum.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.ui.components.BottomNavBar
import com.moasseum.app.ui.theme.MoasseumTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavModalInteractionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun modalDimsAndBlocksNavigationAndAddActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Modal navigation checks are restricted to the isolated QA app." }
        val navClicks = AtomicInteger()
        val addClicks = AtomicInteger()
        val dismissRequests = AtomicInteger()

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    ModalNavigationFixture(navClicks, addClicks, dismissRequests)
                }
            }
        }

        composeRule.onNodeWithText("모달 열기").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_modal_scrim").assertExists()
        composeRule.onNodeWithText("소비내역").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("거래 추가").assertIsNotEnabled()

        composeRule.onNodeWithText("소비내역").performTouchInput { click() }
        assertEquals("A touch under the modal must not navigate.", 0, navClicks.get())
        assertEquals("Outside taps should request modal dismissal.", 1, dismissRequests.get())

        composeRule.onNodeWithContentDescription("거래 추가").performTouchInput { click() }
        assertEquals("A touch under the modal must not open add-entry UI.", 0, addClicks.get())
        assertEquals("The add button remains blocked while its area dismisses the modal.", 2, dismissRequests.get())
    }
}

@Composable
private fun ModalNavigationFixture(navClicks: AtomicInteger, addClicks: AtomicInteger, dismissRequests: AtomicInteger) {
    var modalOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = { modalOpen = true }) { Text("모달 열기") }
        Spacer(Modifier.weight(1f))
        BottomNavBar(
            currentRoute = "home",
            onNavigate = { navClicks.incrementAndGet() },
            interactionBlocked = modalOpen,
            onModalOutsideTap = { dismissRequests.incrementAndGet() },
            onAdd = { addClicks.incrementAndGet() },
        )
    }
}
