package com.moasseum.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moasseum.app.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A profile image loaded only from a user-selected, device-local content URI. */
@Composable
fun LocalProfileAvatar(
    displayName: String,
    photoUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = LocalFinanceColors.current
    val targetPixels = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    var bitmap by remember(photoUri, targetPixels) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoUri, targetPixels) {
        bitmap = photoUri?.let { rawUri ->
            withContext(Dispatchers.IO) { decodeProfileImage(context, rawUri, targetPixels) }
        }
    }

    Box(
        modifier = Modifier.size(size).then(modifier).clip(CircleShape).background(colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "$displayName 프로필 사진",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = displayName.trim().firstOrNull()?.toString() ?: "모",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun decodeProfileImage(context: android.content.Context, rawUri: String, targetPixels: Int): Bitmap? =
    runCatching {
        val uri = Uri.parse(rawUri)
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > targetPixels * 2 || bounds.outHeight / sampleSize > targetPixels * 2) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
