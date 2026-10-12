package com.moasseum.app.domain

data class IncomingSharePayload(
    val text: String?,
    val imageUris: List<String>,
)

/** Normalizes Android shares from other apps without accepting arbitrary file types. */
fun incomingSharePayload(
    action: String?,
    mimeType: String?,
    text: String?,
    streamUris: List<String>,
    clipUris: List<String>,
    maxImages: Int = 20,
): IncomingSharePayload? {
    require(maxImages in 1..20)
    if (action !in setOf("android.intent.action.SEND", "android.intent.action.SEND_MULTIPLE")) return null

    val cleanText = text?.trim()?.take(4_000)?.takeIf(String::isNotBlank)
    val imageUris = if (mimeType?.startsWith("image/", ignoreCase = true) == true) {
        (streamUris.asSequence() + clipUris.asSequence())
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(maxImages)
            .toList()
    } else {
        emptyList()
    }
    if (cleanText == null && imageUris.isEmpty()) return null
    return IncomingSharePayload(cleanText, imageUris)
}
