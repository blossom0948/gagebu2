package com.moasseum.app

import com.moasseum.app.update.ReleaseNotesPolicy
import com.moasseum.app.ui.screens.ReleaseNotesCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesPolicyTest {
    @Test
    fun `every packaged app version has its own illustrated release note`() {
        assertTrue(ReleaseNotesCatalog.slides(BuildConfig.VERSION_NAME).isNotEmpty())
    }

    @Test
    fun `fresh installs do not get an update popup`() {
        assertFalse(ReleaseNotesPolicy.shouldShow("0.1.24", null, isUpgradeInstall = false))
    }

    @Test
    fun `existing installs see the current release notes once`() {
        assertTrue(ReleaseNotesPolicy.shouldShow("0.1.24", "0.1.23", isUpgradeInstall = true))
        assertFalse(ReleaseNotesPolicy.shouldShow("0.1.24", "0.1.24", isUpgradeInstall = false))
    }

    @Test
    fun `older builds with no stored release note state use package install timestamps`() {
        assertTrue(ReleaseNotesPolicy.shouldShow("0.1.24", null, isUpgradeInstall = true))
        assertFalse(ReleaseNotesPolicy.shouldShow("0.1.24", null, isUpgradeInstall = false))
    }
}
