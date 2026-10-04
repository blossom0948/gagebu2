package com.moasseum.app.notification

/** Conversation contents are not evidence of the phone owner's financial activity. */
object NotificationSourcePolicy {
    val excludedPackages = listOf(
        "com.kakao.talk", "org.telegram.messenger", "org.telegram.messenger.web",
        "com.whatsapp", "com.whatsapp.w4b", "com.discord", "jp.naver.line.android",
        "com.facebook.orca", "com.facebook.katana", "com.instagram.android",
        "com.Slack", "com.microsoft.teams", "com.snapchat.android",
    )

    fun isExcluded(
        packageName: String,
        isConversation: Boolean = false,
        isDefaultSmsApp: Boolean = false,
    ): Boolean = packageName in excludedPackages || (isConversation && !isDefaultSmsApp)
}
