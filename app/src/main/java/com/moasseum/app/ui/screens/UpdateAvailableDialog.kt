package com.moasseum.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moasseum.app.update.AppRelease

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
