package com.moasseum.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moasseum.app.update.AppRelease
import com.moasseum.app.ui.theme.LocalFinanceColors

@Composable
fun UpdateAvailableDialog(
    release: AppRelease,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val sizeLabel = if (release.apkSizeBytes > 0L) {
        "APK ${(release.apkSizeBytes / (1024L * 1024L)).coerceAtLeast(1L)}MB"
    } else null
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("새 버전이 있어요") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("모아씀 ${release.versionName}으로 업데이트할까요?", style = MaterialTheme.typography.bodyLarge)
                Text(
                    listOfNotNull(sizeLabel, "설치할 때 Android 확인이 필요해요.").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "나중에 선택해도 관리에서 언제든 직접 확인할 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onUpdate) { Text("업데이트") } },
        dismissButton = { TextButton(onClick = onLater) { Text("나중에") } },
    )
}

@Composable
fun UpdateDownloadDialog(
    progressPercent: Int,
    onDismiss: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val progress = progressPercent.coerceIn(0, 100)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("업데이트 다운로드 중") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(72.dp),
                        strokeWidth = 6.dp,
                    )
                    Text("$progress%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text("다운로드가 끝나면 Android 설치 화면이 열려요.", style = MaterialTheme.typography.bodySmall)
                Surface(color = colors.surfaceRaised, shape = MaterialTheme.shapes.medium) {
                    Text(
                        "삼성 보안 경고가 뜨면 공식 모아씀 GitHub에서 시작한 설치인지 확인한 뒤 ‘무시하고 설치’를 선택하세요.",
                        modifier = Modifier.padding(12.dp),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("백그라운드로") } },
    )
}
