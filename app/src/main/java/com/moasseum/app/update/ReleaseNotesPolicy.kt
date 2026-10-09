package com.moasseum.app.update

object ReleaseNotesPolicy {
    fun shouldShow(
        currentVersion: String,
        lastSeenVersion: String?,
        isUpgradeInstall: Boolean,
    ): Boolean = when {
        lastSeenVersion != null -> lastSeenVersion != currentVersion
        else -> isUpgradeInstall
    }
}
