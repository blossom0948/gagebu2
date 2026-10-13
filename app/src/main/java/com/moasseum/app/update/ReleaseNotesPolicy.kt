package com.moasseum.app.update

object ReleaseNotesPolicy {
    private val versionPattern = Regex("\\d+(?:\\.\\d+){2}")

    fun shouldShow(
        currentVersion: String,
        lastSeenVersion: String?,
        isUpgradeInstall: Boolean,
    ): Boolean {
        require(currentVersion.matches(versionPattern))
        if (lastSeenVersion == null) return isUpgradeInstall
        require(lastSeenVersion.matches(versionPattern))
        return compareVersions(currentVersion, lastSeenVersion) > 0
    }

    fun compareVersions(left: String, right: String): Int {
        require(left.matches(versionPattern) && right.matches(versionPattern))
        val leftParts = left.split('.').map(String::toInt)
        val rightParts = right.split('.').map(String::toInt)
        return leftParts.zip(rightParts).firstNotNullOfOrNull { (a, b) ->
            a.compareTo(b).takeIf { it != 0 }
        } ?: 0
    }
}
