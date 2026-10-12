package com.moasseum.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moasseum.app.ui.theme.LocalFinanceColors

/** A compact determinate finance bar without the detached end marker used by the Material indicator. */
@Composable
fun FinanceProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalFinanceColors.current.accent,
    trackColor: Color = LocalFinanceColors.current.surfaceOverlay,
    height: Dp = 6.dp,
) {
    val value = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.fillMaxWidth().height(height).clip(CircleShape).background(trackColor)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) },
    ) {
        if (value > 0f) {
            Box(
                modifier = Modifier.fillMaxWidth(value).fillMaxHeight().clip(CircleShape).background(color),
            )
        }
    }
}
