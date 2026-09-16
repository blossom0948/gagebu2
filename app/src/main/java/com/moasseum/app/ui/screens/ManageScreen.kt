package com.moasseum.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.PaymentCard
import com.moasseum.app.domain.RecurringRule
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.CategorySpecs
import com.moasseum.app.ui.components.LocalCategoryLabels
import com.moasseum.app.ui.components.allCategorySpecs
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.update.AppRelease
import com.moasseum.app.update.UpdateCheckState
import java.util.Locale
import java.util.UUID

@Composable
fun ManageScreen(
    uiState: LedgerUiState,
    paymentMethods: List<String>,
    paymentCards: List<PaymentCard>,
    onSavePaymentCards: (List<PaymentCard>) -> Unit,
    categoryBudgets: Map<String, Long>,
    onSaveCategoryBudgets: (Map<String, Long>) -> Unit,
    onSavePaymentMethods: (List<String>) -> Unit,
    onSaveCategoryLabels: (Map<String, String>) -> Unit,
    onDeleteCustomCategory: (String, Map<String, String>) -> Unit,
    onClearLocalData: () -> Unit,
    recurringRules: List<RecurringRule>,
    onAddRecurringRule: (String, TransactionType, String, String, String, String, String) -> Boolean,
    onSetRecurringRuleActive: (Long, Boolean) -> Unit,
    onDeleteRecurringRule: (Long) -> Unit,
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onUpdateBudget: (String) -> Boolean,
    notificationAccessEnabled: Boolean,
    pendingCandidates: List<NotificationCandidate>,
    notificationServiceConnectedAt: Long?,
    notificationServiceDisconnectedAt: Long?,
    notificationLastSeenAt: Long?,
    notificationLastCandidateAt: Long?,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onAcceptNotificationCandidate: (Long) -> Unit,
    onDismissNotificationCandidate: (Long) -> Unit,
    appNotificationsEnabled: Boolean,
    aiNotificationClassificationEnabled: Boolean,
    onSetAiNotificationClassificationEnabled: (Boolean) -> Unit,
    updateState: UpdateCheckState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: (AppRelease) -> Unit,
    onContinueInstall: () -> Unit,
) {
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryBudgetsDialog by rememberSaveable { mutableStateOf(false) }
    var showPaymentCardsDialog by rememberSaveable { mutableStateOf(false) }
    var showPaymentMethodsDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showAiNotificationConsent by rememberSaveable { mutableStateOf(false) }
    var showRecurringDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateRecurringDialog by rememberSaveable { mutableStateOf(false) }
    val colors = LocalFinanceColors.current
    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("관리", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            FinanceCard(highlighted = true) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Wallet, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
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
                ManageRow(Icons.Rounded.Category, "카테고리", "기본 7개 이름 변경 · 내 카테고리 추가", onClick = { showCategoryDialog = true })
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.Category,
                    "카테고리별 예산",
                    if (categoryBudgets.isEmpty()) "카테고리별 한도를 설정해요" else "${categoryBudgets.size}개 카테고리 한도 설정됨",
                    onClick = { showCategoryBudgetsDialog = true },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.Rounded.AccountBalance, "결제수단", "${paymentMethods.joinToString(" · ")}", onClick = { showPaymentMethodsDialog = true })
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.CreditCard,
                    "카드·결제일",
                    if (paymentCards.isEmpty()) "카드를 등록하면 결제일 순으로 보여요" else "${paymentCards.size}장 · ${paymentCards.minOf { it.dueDay }}일 기준",
                    onClick = { showPaymentCardsDialog = true },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.Rounded.Repeat, "구독·고정비", "매월 자동 기록 · ${recurringRules.count { it.isActive }}개 사용 중", onClick = { showRecurringDialog = true })
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
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                SettingSwitchRow(
                    icon = Icons.Rounded.Palette,
                    title = "모션 줄이기",
                    message = "전환과 강조 움직임을 줄여요.",
                    checked = reduceMotion,
                    onCheckedChange = onReduceMotionChanged,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.NotificationsActive,
                    "결제 알림 감지",
                    notificationStatusMessage(
                        notificationAccessEnabled = notificationAccessEnabled,
                        serviceConnectedAt = notificationServiceConnectedAt,
                        serviceDisconnectedAt = notificationServiceDisconnectedAt,
                        lastSeenAt = notificationLastSeenAt,
                        pendingCount = pendingCandidates.size,
                    ),
                    onClick = onOpenNotificationSettings,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.NotificationsActive,
                    "인식 알림 표시",
                    if (appNotificationsEnabled) "허용됨 · 감지 결과를 바로 알려드려요" else "앱 알림 권한을 켜야 감지 즉시 알려드려요",
                    onClick = onOpenAppNotificationSettings,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                SettingSwitchRow(
                    icon = Icons.Rounded.Security,
                    title = "AI 알림 오탐 줄이기",
                    message = if (aiNotificationClassificationEnabled) "금융 단서가 있는 알림을 AI가 판별해요." else "선택 알림의 금융 거래 여부를 AI가 판별해요.",
                    checked = aiNotificationClassificationEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) showAiNotificationConsent = true
                        else onSetAiNotificationClassificationEnabled(false)
                    },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.Rounded.Security, "개인정보와 데이터", "기기 저장 · AI 전송 안내 · 데이터 삭제", onClick = { showPrivacyDialog = true })
            }
        }
        item {
            NotificationCandidatesCard(
                candidates = pendingCandidates,
                notificationAccessEnabled = notificationAccessEnabled,
                lastCandidateAt = notificationLastCandidateAt,
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
                onContinueInstall = onContinueInstall,
            )
        }
        item {
            Text(
                "모아씀 ${com.moasseum.app.BuildConfig.VERSION_NAME} · 현재는 기기 안에만 저장돼요",
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
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
    if (showCategoryDialog) {
        CategoryNamesDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = { labels ->
                onSaveCategoryLabels(labels)
                showCategoryDialog = false
            },
            onRemove = onDeleteCustomCategory,
        )
    }
    if (showCategoryBudgetsDialog) {
        CategoryBudgetsDialog(
            budgets = categoryBudgets,
            onDismiss = { showCategoryBudgetsDialog = false },
            onSave = { budgets ->
                onSaveCategoryBudgets(budgets)
                showCategoryBudgetsDialog = false
            },
        )
    }
    if (showPaymentMethodsDialog) {
        PaymentMethodsDialog(
            methods = paymentMethods,
            onDismiss = { showPaymentMethodsDialog = false },
            onSave = { methods ->
                onSavePaymentMethods(methods)
                showPaymentMethodsDialog = false
            },
        )
    }
    if (showPaymentCardsDialog) {
        PaymentCardsDialog(
            cards = paymentCards,
            onDismiss = { showPaymentCardsDialog = false },
            onSave = { cards ->
                onSavePaymentCards(cards)
                showPaymentCardsDialog = false
            },
        )
    }
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("개인정보와 데이터") },
            text = {
                Text("거래와 예산은 이 기기의 로컬 데이터베이스에 저장돼요. 자연어 입력을 해석하거나 AI 분석을 누를 때만 입력 문장 또는 합계·카테고리 통계가 AI Worker로 전송됩니다. 영수증 사진은 기기 안에서 OCR 처리하며 서버로 보내지 않아요.")
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("닫기") } },
            dismissButton = {
                TextButton(onClick = {
                    showPrivacyDialog = false
                    showDeleteConfirmation = true
                }) { Text("기기 데이터 삭제", color = colors.expense) }
            },
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("기기 데이터를 삭제할까요?") },
            text = { Text("저장된 거래, 예산, 알림 후보, 반복 규칙을 모두 지워요. 이 작업은 되돌릴 수 없으니 필요하면 먼저 내역 화면에서 CSV로 내보내세요. 앱 설정은 유지됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onClearLocalData()
                }) { Text("모두 삭제", color = colors.expense) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("취소") } },
        )
    }
    if (showAiNotificationConsent) {
        AlertDialog(
            onDismissRequest = { showAiNotificationConsent = false },
            title = { Text("AI 알림 판별을 켤까요?") },
            text = {
                Text("먼저 기기 안에서 금액과 금융 단서가 함께 있는 알림만 골라요. 선택된 알림의 제목과 내용은 Cloudflare AI Worker를 거쳐 Gemini로 전송되어 실제 입금·지출인지 판별됩니다. 이 기능은 AI 사용량을 쓸 수 있고, 원문 알림은 앱 거래로 자동 저장하지 않아요.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showAiNotificationConsent = false
                    onSetAiNotificationClassificationEnabled(true)
                }) { Text("동의하고 켜기") }
            },
            dismissButton = {
                TextButton(onClick = { showAiNotificationConsent = false }) { Text("취소") }
            },
        )
    }
    if (showRecurringDialog) {
        RecurringRulesDialog(
            rules = recurringRules,
            onDismiss = { showRecurringDialog = false },
            onCreate = {
                showRecurringDialog = false
                showCreateRecurringDialog = true
            },
            onToggle = onSetRecurringRuleActive,
            onDelete = onDeleteRecurringRule,
        )
    }
    if (showCreateRecurringDialog) {
        RecurringRuleDialog(
            paymentMethods = paymentMethods,
            onDismiss = { showCreateRecurringDialog = false },
            onSave = { amount, type, merchant, categoryKey, memo, paymentMethod, day ->
                val saved = onAddRecurringRule(amount, type, merchant, categoryKey, memo, paymentMethod, day)
                if (saved) {
                    showCreateRecurringDialog = false
                    showRecurringDialog = true
                }
                saved
            },
        )
    }
}

@Composable
private fun CategoryNamesDialog(
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
    onRemove: (String, Map<String, String>) -> Unit,
) {
    val currentLabels = LocalCategoryLabels.current
    val colors = LocalFinanceColors.current
    val initialNames = currentLabels
    var names by remember(initialNames) { mutableStateOf(initialNames) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var categoryToRemove by remember { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카테고리 관리") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategorySpecs.forEach { spec ->
                    OutlinedTextField(
                        value = names[spec.key].orEmpty(),
                        onValueChange = { value -> names = names + (spec.key to value.take(16)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("기본 ${categoryLabel(spec.key)}") },
                        leadingIcon = { Icon(spec.icon, contentDescription = null, tint = spec.color) },
                        singleLine = true,
                    )
                }
                if (names.keys.any { it.startsWith("CUSTOM_") }) {
                    Text("내 카테고리", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                    names.filterKeys { it.startsWith("CUSTOM_") }.forEach { (key, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = label,
                                onValueChange = { value -> names = names + (key to value.take(16)) },
                                modifier = Modifier.weight(1f),
                                label = { Text("카테고리 이름") },
                                leadingIcon = { Icon(Icons.Rounded.Category, contentDescription = null, tint = colors.accent) },
                                singleLine = true,
                            )
                            IconButton(onClick = { categoryToRemove = key }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "${label} 삭제", tint = colors.expense)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it.take(16); errorMessage = null },
                        modifier = Modifier.weight(1f),
                        label = { Text("새 카테고리") },
                        singleLine = true,
                    )
                    TextButton(onClick = {
                        val label = newCategoryName.trim()
                        when {
                            label.isBlank() -> errorMessage = "이름을 입력해 주세요."
                            names.values.any { it.trim().lowercase(Locale.ROOT) == label.lowercase(Locale.ROOT) } -> errorMessage = "이미 있는 이름이에요."
                            names.keys.count { it.startsWith("CUSTOM_") } >= 20 -> errorMessage = "내 카테고리는 최대 20개까지 만들 수 있어요."
                            else -> {
                                val key = "CUSTOM_${UUID.randomUUID().toString().replace("-", "").take(12).uppercase(Locale.ROOT)}"
                                names = names + (key to label)
                                newCategoryName = ""
                                errorMessage = null
                            }
                        }
                    }) { Text("추가", color = colors.accent) }
                }
                errorMessage?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(names) },
                enabled = names.values.all(String::isNotBlank) && names.values.map { it.lowercase(Locale.ROOT) }.distinct().size == names.size,
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
    categoryToRemove?.let { key ->
        val label = names[key].orEmpty()
        AlertDialog(
            onDismissRequest = { categoryToRemove = null },
            title = { Text("‘$label’ 카테고리를 삭제할까요?") },
            text = { Text("기존 거래, 반복 규칙, 알림 후보는 ‘기타’로 옮겨져요.") },
            confirmButton = {
                TextButton(onClick = {
                    val updated = names - key
                    names = updated
                    categoryToRemove = null
                    onRemove(key, updated)
                }) { Text("삭제", color = colors.expense) }
            },
            dismissButton = { TextButton(onClick = { categoryToRemove = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun CategoryBudgetsDialog(
    budgets: Map<String, Long>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Long>) -> Unit,
) {
    val colors = LocalFinanceColors.current
    val specs = allCategorySpecs()
    var draft by remember(budgets) { mutableStateOf(budgets.mapValues { (_, amount) -> amount.toString() }) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카테고리별 예산") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text("비워 두면 해당 카테고리 한도를 해제해요. 홈 카테고리 현황에도 사용률을 표시합니다.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                specs.forEach { spec ->
                    OutlinedTextField(
                        value = draft[spec.key].orEmpty(),
                        onValueChange = { value ->
                            draft = draft + (spec.key to value.filter(Char::isDigit).take(13))
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${spec.label} 한도") },
                        trailingIcon = { Text("원", color = colors.textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                errorMessage?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val invalid = draft.values.any { value ->
                    value.isNotBlank() && (value.toLongOrNull() == null || value.toLong() !in 1..1_000_000_000_000L)
                }
                if (invalid) {
                    errorMessage = "비워 두거나 1원 이상 금액을 입력해 주세요."
                } else {
                    onSave(draft.mapNotNull { (key, value) -> value.toLongOrNull()?.takeIf { it > 0L }?.let { key to it } }.toMap())
                }
            }) { Text("저장", color = colors.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun PaymentMethodsDialog(
    methods: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var draft by remember(methods) { mutableStateOf(methods) }
    var newMethod by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("결제수단 관리") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("항목을 눌러 제거하거나 새 결제수단을 추가하세요. 이미 저장된 거래의 값은 바뀌지 않아요.", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    draft.forEach { method ->
                        FilterChip(selected = true, onClick = { draft = draft - method }, label = { Text("$method  ×") })
                    }
                }
                OutlinedTextField(
                    value = newMethod,
                    onValueChange = { newMethod = it.take(20); error = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("새 결제수단") },
                    singleLine = true,
                    isError = error,
                )
                TextButton(onClick = {
                    val value = newMethod.trim()
                    if (value.isBlank() || value in draft || draft.size >= 12) {
                        error = true
                    } else {
                        draft = draft + value
                        newMethod = ""
                        error = false
                    }
                }) { Text("추가") }
                if (error) Text("중복되지 않은 이름을 입력해 주세요. 최대 12개까지 설정할 수 있어요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                if (draft.isEmpty()) Text("결제수단을 하나 이상 남겨주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }, enabled = draft.isNotEmpty()) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun PaymentCardsDialog(
    cards: List<PaymentCard>,
    onDismiss: () -> Unit,
    onSave: (List<PaymentCard>) -> Unit,
) {
    val colors = LocalFinanceColors.current
    var draft by remember(cards) { mutableStateOf(cards) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<PaymentCard?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카드·결제일 관리") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Text("카드별 결제일을 저장해 두면 이번 달 확인 순서가 한눈에 보여요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                if (draft.isEmpty()) {
                    Text("등록된 카드가 없어요. 카드 이름과 결제일만 저장합니다.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                draft.sortedWith(compareBy<PaymentCard> { it.dueDay }.thenBy { it.name }).forEach { card ->
                    FinanceCard {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.CreditCard, contentDescription = null, tint = colors.accent, modifier = Modifier.size(21.dp))
                            Spacer(Modifier.width(9.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(card.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("매월 ${card.dueDay}일 결제", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(onClick = {
                                editingCard = card
                                showEditor = true
                            }) { Text("수정", color = colors.accent) }
                            IconButton(onClick = { draft = draft - card }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "${card.name} 삭제", tint = colors.expense)
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        editingCard = null
                        errorMessage = null
                        showEditor = true
                    },
                    enabled = draft.size < 12,
                ) { Text("+ 카드 추가", color = colors.accent) }
                if (draft.size >= 12) Text("카드는 최대 12장까지 등록할 수 있어요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                errorMessage?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("저장", color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )

    if (showEditor) {
        PaymentCardEditorDialog(
            initialCard = editingCard,
            onDismiss = { showEditor = false },
            onSave = { card ->
                val duplicate = draft.any { it.id != card.id && it.name.equals(card.name, ignoreCase = true) }
                if (duplicate) {
                    errorMessage = "같은 이름의 카드가 이미 있어요."
                } else {
                    draft = if (editingCard == null) draft + card else draft.map { existing -> if (existing.id == card.id) card else existing }
                    errorMessage = null
                    showEditor = false
                }
            },
        )
    }
}

@Composable
private fun PaymentCardEditorDialog(
    initialCard: PaymentCard?,
    onDismiss: () -> Unit,
    onSave: (PaymentCard) -> Unit,
) {
    val colors = LocalFinanceColors.current
    var name by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.name.orEmpty()) }
    var dueDay by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.dueDay?.toString() ?: "25") }
    var showError by rememberSaveable(initialCard?.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCard == null) "카드 추가" else "카드 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24); showError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("카드 이름") },
                    placeholder = { Text("예: 생활비 카드") },
                    singleLine = true,
                    isError = showError,
                )
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { dueDay = it.filter(Char::isDigit).take(2); showError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("결제일 (1~31일)") },
                    suffix = { Text("일") },
                    singleLine = true,
                    isError = showError,
                )
                if (showError) Text("카드 이름과 1~31 사이 결제일을 입력해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedDueDay = dueDay.toIntOrNull()
                if (name.isBlank() || parsedDueDay == null || parsedDueDay !in 1..31) {
                    showError = true
                } else {
                    onSave(
                        PaymentCard(
                            id = initialCard?.id ?: "CARD_${UUID.randomUUID().toString().replace("-", "").take(12).uppercase(Locale.ROOT)}",
                            name = name.trim(),
                            dueDay = parsedDueDay,
                        ),
                    )
                }
            }) { Text("저장", color = colors.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun RecurringRulesDialog(
    rules: List<RecurringRule>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val colors = LocalFinanceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("반복 거래") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("매월 지정한 날짜에 거래를 자동으로 추가해요. 앱을 열 때 놓친 회차도 한 번씩 보충합니다.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                if (rules.isEmpty()) {
                    Text("아직 반복 거래가 없어요. 월세나 정기 구독을 등록해 보세요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                rules.forEach { rule ->
                    FinanceCard {
                        Row(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(rule.merchant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("매월 ${rule.dayOfMonth}일 · ${formatWon(rule.amount)} · 다음 ${formatDate(rule.nextOccurrenceDate)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                                Text("${categoryLabel(rule.categoryKey)} · ${rule.paymentMethod}", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(checked = rule.isActive, onCheckedChange = { onToggle(rule.id, it) })
                            IconButton(onClick = { onDelete(rule.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "반복 거래 삭제", tint = colors.expense)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCreate) { Text("반복 거래 추가", color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun RecurringRuleDialog(
    paymentMethods: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType, String, String, String, String, String) -> Boolean,
) {
    val colors = LocalFinanceColors.current
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("1") }
    var typeName by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var categoryKey by rememberSaveable { mutableStateOf("LIVING") }
    var paymentMethod by rememberSaveable { mutableStateOf(paymentMethods.firstOrNull().orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("매월 반복 거래 추가") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(type == TransactionType.EXPENSE, { typeName = TransactionType.EXPENSE.name }, label = { Text("지출") })
                    FilterChip(type == TransactionType.INCOME, { typeName = TransactionType.INCOME.name }, label = { Text("수입") })
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit); showError = false }, label = { Text("금액") }, suffix = { Text("원") }, singleLine = true)
                OutlinedTextField(value = merchant, onValueChange = { merchant = it; showError = false }, label = { Text("가맹점 또는 이름") }, singleLine = true)
                OutlinedTextField(value = day, onValueChange = { day = it.filter(Char::isDigit).take(2); showError = false }, label = { Text("매월 며칠 (1~31)") }, suffix = { Text("일") }, singleLine = true)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allCategorySpecs().forEach { spec ->
                        FilterChip(categoryKey == spec.key, { categoryKey = spec.key }, label = { Text(categoryLabel(spec.key)) })
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paymentMethods.forEach { method ->
                        FilterChip(paymentMethod == method, { paymentMethod = method }, label = { Text(method) })
                    }
                }
                if (showError) Text("금액, 이름, 반복 날짜를 확인해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                Text("예: 매월 31일은 2월에는 마지막 날에 기록돼요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(amount, type, merchant, categoryKey, "", paymentMethod, day)) showError = true }) {
                Text("저장", color = colors.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun AppUpdateCard(
    updateState: UpdateCheckState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: (AppRelease) -> Unit,
    onContinueInstall: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("앱 업데이트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when (updateState) {
                            UpdateCheckState.Idle -> "새 버전을 확인하고 앱 안에서 내려받아요."
                            UpdateCheckState.Checking -> "새 버전을 확인하고 있어요…"
                            UpdateCheckState.UpToDate -> "현재 최신 버전이에요."
                            is UpdateCheckState.Available -> "${updateState.release.tagName} 업데이트가 있어요."
                            is UpdateCheckState.Downloading -> "앱 안에서 APK 다운로드 중 · ${updateState.progressPercent}%"
                            UpdateCheckState.WaitingForInstallPermission -> "이 앱의 ‘알 수 없는 앱 설치’를 허용하고 돌아오면 이어집니다."
                            UpdateCheckState.OpeningInstaller -> "Android 설치 확인 화면을 여는 중…"
                            UpdateCheckState.InstallerOpened -> "Android 설치 화면에서 업데이트를 승인해 주세요."
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
                UpdateCheckState.OpeningInstaller,
                -> Unit
                is UpdateCheckState.Available -> {
                    val release = updateState.release
                    TextButton(onClick = { onInstallUpdate(release) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("앱에서 다운로드·설치", color = colors.accent)
                    }
                }
                UpdateCheckState.WaitingForInstallPermission -> {
                    TextButton(onClick = onContinueInstall, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("설정에서 허용", color = colors.accent)
                    }
                }
                UpdateCheckState.InstallerOpened -> {
                    TextButton(onClick = onContinueInstall, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("설치 화면 다시 열기", color = colors.accent)
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
    lastCandidateAt: Long?,
    onOpenSettings: () -> Unit,
    onAccept: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("알림 후보함", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (notificationAccessEnabled) {
                            lastCandidateAt?.let { "마지막 후보 ${relativeNotificationTime(it)} · 확인 후에만 거래로 저장돼요." }
                                ?: "결제 알림은 확인 후에만 거래로 저장돼요."
                        } else {
                            "알림 접근을 허용하면 이곳에 후보가 쌓여요."
                        },
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            if (!notificationAccessEnabled) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.padding(horizontal = 6.dp)) { Text("알림 접근 설정 열기") }
            } else if (candidates.isEmpty()) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.padding(horizontal = 6.dp)) { Text("서비스 다시 연결") }
            } else {
                candidates.take(5).forEachIndexed { index, candidate ->
                    NotificationCandidateRow(candidate = candidate, onAccept = { onAccept(candidate.id) }, onDismiss = { onDismiss(candidate.id) })
                    if (index < candidates.take(5).lastIndex) HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                }
            }
        }
    }
}

private fun notificationStatusMessage(
    notificationAccessEnabled: Boolean,
    serviceConnectedAt: Long?,
    serviceDisconnectedAt: Long?,
    lastSeenAt: Long?,
    pendingCount: Int,
): String = when {
    !notificationAccessEnabled -> "Samsung 설정에서 알림 접근을 허용해요"
    serviceDisconnectedAt != null && (serviceConnectedAt == null || serviceDisconnectedAt >= serviceConnectedAt) ->
        "서비스 연결이 끊겼어요 · 다시 연결해 주세요"
    serviceConnectedAt == null -> "접근 허용됨 · 서비스 연결 대기 중"
    lastSeenAt == null -> "서비스 연결됨 · 외부 알림을 기다리는 중"
    else -> "연결됨 · ${relativeNotificationTime(lastSeenAt)} 수신 · 후보 ${pendingCount}건"
}

private fun relativeNotificationTime(timestamp: Long): String {
    val elapsedMinutes = ((System.currentTimeMillis() - timestamp) / 60_000L).coerceAtLeast(0L)
    return when {
        elapsedMinutes < 1L -> "방금"
        elapsedMinutes < 60L -> "${elapsedMinutes}분 전"
        elapsedMinutes < 1_440L -> "${elapsedMinutes / 60L}시간 전"
        else -> "${elapsedMinutes / 1_440L}일 전"
    }
}

@Composable
private fun NotificationCandidateRow(
    candidate: NotificationCandidate,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
