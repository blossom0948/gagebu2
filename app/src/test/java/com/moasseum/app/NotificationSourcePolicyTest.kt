package com.moasseum.app

import com.moasseum.app.notification.NotificationSourcePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSourcePolicyTest {
    @Test fun knownMessengersAreExcludedBeforeReadingOrAi() {
        NotificationSourcePolicy.excludedPackages.forEach {
            assertTrue(NotificationSourcePolicy.isExcluded(it))
        }
    }

    @Test fun unknownConversationAppIsAlsoExcluded() {
        assertTrue(NotificationSourcePolicy.isExcluded("com.example.chat", isConversation = true))
    }

    @Test fun bankAlertsAndDefaultSmsRemainEligible() {
        assertFalse(NotificationSourcePolicy.isExcluded("com.kakaopay.app"))
        assertFalse(NotificationSourcePolicy.isExcluded("com.example.bank"))
        assertFalse(NotificationSourcePolicy.isExcluded("com.google.android.apps.messaging", isConversation = true, isDefaultSmsApp = true))
    }
}
