package com.moasseum.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moasseum.app.ui.components.FinanceTextField as OutlinedTextField
import com.moasseum.app.ui.theme.LocalFinanceColors

@Composable
fun ProfileEditorDialog(
    initialName: String,
    photoUri: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    val colors = LocalFinanceColors.current
    AlertDialog(
        modifier = Modifier.testTag("profile_editor_dialog"),
        onDismissRequest = onDismiss,
        title = { Text("프로필") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LocalProfileAvatar(displayName = name, photoUri = photoUri, size = 72.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onChoosePhoto) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (photoUri == null) "사진 추가" else "사진 변경")
                    }
                    if (photoUri != null) {
                        TextButton(onClick = onRemovePhoto) { Text("삭제") }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("표시 이름") },
                    singleLine = true,
                )
                Text(
                    "이름과 사진은 이 기기에만 저장돼요.",
                    color = colors.textSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim()) }) { Text("저장") } },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave("") }) { Text("기본 이름") }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}
