package com.moasseum.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moasseum.app.ui.theme.LocalFinanceColors

@Composable
fun QuickHelpDialog(
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onHistory: () -> Unit,
    onManage: () -> Unit,
    onTogether: () -> Unit,
    onFirstRunGuide: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("어디서 할까요?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HelpAction(Icons.Rounded.AddCircleOutline, "거래 기록", "직접 입력 · AI · 사진", onAdd)
                HelpAction(Icons.Rounded.History, "내역 찾기·수정", "날짜 · 검색 · 삭제", onHistory)
                HelpAction(Icons.Rounded.AccountBalanceWallet, "예산·카드·고정비", "관리에서 설정", onManage)
                HelpAction(Icons.Rounded.People, "파트너와 공유", "함께 장부 · 정산", onTogether)
                TextButton(onClick = onFirstRunGuide, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null)
                    Text("  처음 안내 다시 보기")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun HelpAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = colors.accent)
        Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
}
