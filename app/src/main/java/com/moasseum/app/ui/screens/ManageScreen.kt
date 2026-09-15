package com.moasseum.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.update.AppRelease
import com.moasseum.app.update.UpdateCheckState

@Composable
fun ManageScreen(
    uiState: LedgerUiState,
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onUpdateBudget: (String) -> Boolean,
    onShowUnavailable: (String) -> Unit,
    notificationAccessEnabled: Boolean,
    pendingCandidates: List<NotificationCandidate>,
    onOpenNotificationSettings: () -> Unit,
    onAcceptNotificationCandidate: (Long) -> Unit,
    onDismissNotificationCandidate: (Long) -> Unit,
    updateState: UpdateCheckState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: (AppRelease) -> Unit,
) {
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    val colors = LocalFinanceColors.current
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("관리", style = MaterialTheme.typography.headlineSmall)
                Text("나에게 맞는 기록 환경을 설정하세요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            FinanceCard(highlighted = true) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Wallet, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${formatMonth(uiState.month)} 목표 지출", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("홈 화면의 진행률에 반영돼요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { showBudgetDialog = true }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "예산 수정")
                        }
                    }
                    Text(uiState.budgetAmount?.let(::formatWon) ?: "설정되지 않음", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { ManageSectionTitle("가계부 구성") }
        item {
            FinanceCard {
                ManageRow(Icons.Rounded.Category, "카테고리", "식비 · 교통 · 쇼핑 등 기본 7개", onClick = { onShowUnavailable("카테고리 편집") })
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 20.dp))
                ManageRow(Icons.Rounded.AccountBalance, "결제수단", "카드와 현금 관리", onClick = { onShowUnavailable("결제수단 관리") })
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 20.dp))
                ManageRow(Icons.Rounded.Repeat, "반복 거래", "고정비를 자동으로 준비", onClick = { onShowUnavailable("반복 거래") })
            }
        }
        item { ManageSectionTitle("앱 설정") }
        item {
            FinanceCard {
                SettingSwitchRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "다크 모드",
                    message = "차분한 어두운 화면을 사용해요.",
                    checked = darkTheme,
                    onCheckedChange = onDarkThemeChanged,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 20.dp))
                SettingSwitchRow(
                    icon = Icons.Rounded.Palette,
                    title = "모션 줄이기",
                    message = "전환과 강조 움직임을 줄여요.",
                    checked = reduceMotion,
                    onCheckedChange = onReduceMotionChanged,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 20.dp))
                ManageRow(
                    Icons.Rounded.NotificationsActive,
                    "결제 알림 감지",
                    if (notificationAccessEnabled) "허용됨 · 후보 ${pendingCandidates.size}건" else "권한을 허용하면 결제 후보를 읽어요",
                    onClick = onOpenNotificationSettings,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 20.dp))
                ManageRow(Icons.Rounded.Security, "개인정보와 데이터", "내보내기 · 삭제 · AI 전송 설정", onClick = { onShowUnavailable("개인정보 설정") })
            }
        }
        item {
            NotificationCandidatesCard(
                candidates = pendingCandidates,
                notificationAccessEnabled = notificationAccessEnabled,
                onOpenSettings = onOpenNotificationSettings,
                onAccept = onAcceptNotificationCandidate,
                onDismiss = onDismissNotificationCandidate,
            )
        }
        item {
            AppUpdateCard(
                updateState = updateState,
                onCheckForUpdate = onCheckForUpdate,
                onInstallUpdate = onInstallUpdate,
            )
        }
        item {
            Text(
                "모아씀 ${com.moasseum.app.BuildConfig.VERSION_NAME} · 현재는 기기 안에만 저장돼요",
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            initialValue = uiState.budgetAmount?.toString().orEmpty(),
            onDismiss = { showBudgetDialog = false },
            onSave = { input ->
                val saved = onUpdateBudget(input)
                if (saved) showBudgetDialog = false
                saved
            },
        )
    }
}

@Composable
private fun AppUpdateCard(
    updateState: UpdateCheckState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: (AppRelease) -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(modifier = Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = colors.accent, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("앱 업데이트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when (updateState) {
                            UpdateCheckState.Idle -> "GitHub에서 새 버전을 직접 확인해요."
                            UpdateCheckState.Checking -> "새 버전을 확인하고 있어요…"
                            UpdateCheckState.UpToDate -> "현재 최신 버전이에요."
                            is UpdateCheckState.Available -> "${updateState.release.tagName} 업데이트가 있어요."
                            is UpdateCheckState.Downloading -> "${updateState.release.tagName} 다운로드 중…"
                            UpdateCheckState.WaitingForInstallPermission -> "설치 권한을 켠 뒤 다시 업데이트를 눌러주세요."
                            is UpdateCheckState.Installing -> "Android 설치 화면을 열었어요."
                            is UpdateCheckState.Error -> updateState.message
                        },
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            when (updateState) {
                UpdateCheckState.Checking,
                is UpdateCheckState.Downloading,
                is UpdateCheckState.Installing,
                -> Unit
                is UpdateCheckState.Available -> {
                    val release = updateState.release
                    TextButton(onClick = { onInstallUpdate(release) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("다운로드 및 설치", color = colors.accent)
                    }
                }
                else -> {
                    TextButton(onClick = onCheckForUpdate, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("업데이트 확인", color = colors.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCandidatesCard(
    candidates: List<NotificationCandidate>,
    notificationAccessEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onAccept: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(modifier = Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("알림 후보함", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (notificationAccessEnabled) "결제 알림은 확인 후에만 거래로 저장돼요." else "알림 접근을 허용하면 이곳에 후보가 쌓여요.",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            if (!notificationAccessEnabled) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.padding(horizontal = 12.dp)) { Text("알림 접근 설정 열기") }
            } else if (candidates.isEmpty()) {
                Text("아직 검토할 결제 알림이 없어요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
            } else {
                candidates.take(5).forEachIndexed { index, candidate ->
                    NotificationCandidateRow(candidate = candidate, onAccept = { onAccept(candidate.id) }, onDismiss = { onDismiss(candidate.id) })
                    if (index < candidates.take(5).lastIndex) HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationCandidateRow(
    candidate: NotificationCandidate,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(com.moasseum.app.ui.components.categoryIcon(candidate.categoryKey), contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(candidate.merchant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("${formatDate(candidate.occurredDate)} · ${candidate.title}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = if (candidate.type == com.moasseum.app.domain.TransactionType.EXPENSE) "−${formatWon(candidate.amount)}" else "+${formatWon(candidate.amount)}",
                color = if (candidate.type == com.moasseum.app.domain.TransactionType.EXPENSE) colors.expense else colors.income,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(candidate.preview, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAccept) { Text("거래로 저장", color = colors.accent) }
            TextButton(onClick = onDismiss) { Text("무시", color = colors.textSecondary) }
        }
    }
}

@Composable
private fun ManageSectionTitle(title: String) {
    Text(title, color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
}

@Composable
private fun ManageRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onClick: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(message, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(message, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BudgetDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Boolean,
) {
    var input by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var showError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("한 달 목표 지출 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${formatMonth(java.time.YearMonth.now())} 기준 목표", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit); showError = false },
                    label = { Text("금액") },
                    suffix = { Text("원") },
                    singleLine = true,
                    isError = showError,
                )
                if (showError) Text("1원 이상 입력해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(input)) showError = true }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
