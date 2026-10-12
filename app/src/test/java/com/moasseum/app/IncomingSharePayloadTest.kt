package com.moasseum.app

import com.moasseum.app.domain.incomingSharePayload
import org.junit.Assert.*
import org.junit.Test

class IncomingSharePayloadTest {
    @Test fun multipleImagesAndClipDataAreCombinedOnceInStableOrder() {
        val payload = incomingSharePayload(
            action = "android.intent.action.SEND_MULTIPLE",
            mimeType = "image/jpeg",
            text = null,
            streamUris = listOf("content://photos/1", "content://photos/2"),
            clipUris = listOf("content://photos/2", "content://photos/3"),
        )

        assertEquals(listOf("content://photos/1", "content://photos/2", "content://photos/3"), payload?.imageUris)
    }

    @Test fun unsupportedActionsAndNonImageStreamsAreIgnoredButSharedTextWorks() {
        assertNull(incomingSharePayload("android.intent.action.VIEW", "image/png", null, listOf("content://x"), emptyList()))
        assertNull(incomingSharePayload("android.intent.action.SEND", "text/plain", null, listOf("content://x"), emptyList()))
        assertEquals(
            "점심 8000원",
            incomingSharePayload("android.intent.action.SEND", "text/plain", " 점심 8000원 ", emptyList(), emptyList())?.text,
        )
    }

    @Test fun sharedImageCountIsCappedAndBlankUrisAreDropped() {
        val uris = (1..25).map { "content://photos/$it" }
        val payload = incomingSharePayload("android.intent.action.SEND_MULTIPLE", "image/*", null, uris, listOf(" "))

        assertEquals(20, payload?.imageUris?.size)
        assertEquals("content://photos/20", payload?.imageUris?.last())
        assertNull(incomingSharePayload("android.intent.action.SEND_MULTIPLE", "image/*", " ", emptyList(), emptyList()))
    }
}
