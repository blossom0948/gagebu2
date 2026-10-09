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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import com.moasseum.app.ui.components.FinanceTextField as OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.NoSpendChallengeSettings
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.PaymentCard
import com.moasseum.app.domain.RecurringRule
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.domain.cardUsage
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.rememberSaveActionState
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
    homeDashboardCards: Set<String>,
    onSaveHomeDashboardCards: suspend (Set<String>) -> Unit,
    noSpendChallenge: NoSpendChallengeSettings,
    onSaveNoSpendChallenge: suspend (Boolean, Int) -> Unit,
    paymentMethods: List<String>,
    paymentCards: List<PaymentCard>,
    onSavePaymentCards: suspend (List<PaymentCard>) -> Unit,
    categoryBudgets: Map<String, Long>,
    onSaveCategoryBudgets: suspend (Map<String, Long>) -> Unit,
    categoryOrder: List<String>,
    onSaveCategoryOrder: suspend (List<String>) -> Unit,
    monthlyIncomeTarget: Long?,
    onSaveMonthlyIncomeTarget: suspend (Long?) -> Unit,
    onSavePaymentMethods: suspend (List<String>) -> Unit,
    onSaveCategoryLabels: suspend (Map<String, String>) -> Unit,
    onDeleteCustomCategory: suspend (String, Map<String, String>) -> Unit,
    onClearLocalData: () -> Unit,
    recurringRules: List<RecurringRule>,
    onAddRecurringRule: suspend (String, TransactionType, String, String, String, String, String) -> Unit,
    onEditRecurringRule: suspend (Long, String, TransactionType, String, String, String, String, String) -> Unit,
    onSetRecurringRuleActive: (Long, Boolean) -> Unit,
    onDeleteRecurringRule: (Long) -> Unit,
    onAddInstallmentPlan: suspend (Long, Int, java.time.LocalDate, String, String, String, String) -> Unit,
    onDeleteInstallmentPlan: (String) -> Unit,
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onUpdateBudget: suspend (String) -> Unit,
    notificationAccessEnabled: Boolean,
    pendingCandidates: List<NotificationCandidate>,
    notificationServiceConnectedAt: Long?,
    notificationServiceDisconnectedAt: Long?,
    notificationLastSeenAt: Long?,
    appLockEnabled: Boolean,
    onSetAppLockEnabled: (Boolean) -> Unit,
    financeRemindersEnabled: Boolean,
    onSetFinanceRemindersEnabled: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onOpenCandidates: () -> Unit,
    appNotificationsEnabled: Boolean,
    aiNotificationClassificationEnabled: Boolean,
    aiLoginRequired: Boolean = false,
    onSetAiNotificationClassificationEnabled: (Boolean) -> Unit,
    updateState: UpdateCheckState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: (AppRelease) -> Unit,
    onContinueInstall: () -> Unit,
    onOpenAccounts: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onExportSafetyBackup: () -> Unit,
    onExportPdf: () -> Unit,
    onExportYearTransactions: (Int) -> Unit,
    onSetBudgetRollover: (Boolean) -> Unit,
    onOpenAuth: () -> Unit,
    onOpenGuide: () -> Unit,
    accountStatus: String,
    scrollToTopRequest: Int = 0,
) {
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryBudgetsDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryOrderDialog by rememberSaveable { mutableStateOf(false) }
    var showIncomeTargetDialog by rememberSaveable { mutableStateOf(false) }
    var showPaymentCardsDialog by rememberSaveable { mutableStateOf(false) }
    var showPaymentMethodsDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showAiNotificationConsent by rememberSaveable { mutableStateOf(false) }
    var showRecurringDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateRecurringDialog by rememberSaveable { mutableStateOf(false) }
    var editingRecurringId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detectedRecurringPrefill by remember { mutableStateOf<RecurringRule?>(null) }
    var showCardUsageDialog by rememberSaveable { mutableStateOf(false) }
    var showFixedRadarDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeCardsDialog by rememberSaveable { mutableStateOf(false) }
    var showNoSpendDialog by rememberSaveable { mutableStateOf(false) }
    var showInstallmentDialog by rememberSaveable { mutableStateOf(false) }
    var showYearEndDialog by rememberSaveable { mutableStateOf(false) }
    var selectedReportYear by rememberSaveable { mutableStateOf(java.time.LocalDate.now().year) }
    val colors = LocalFinanceColors.current
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) listState.animateScrollToItem(0)
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("관리", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            FinanceCard(
                modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { showBudgetDialog = true },
                highlighted = true,
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Wallet, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${formatMonth(uiState.month)} 목표 지출", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                    Text(uiState.budgetAmount?.let(::formatWon) ?: "설정되지 않음", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { ManageSectionTitle("카드·고정 지출") }
        item {
            FinanceCard {
                val installmentGroups = uiState.transactions.filter { it.installmentGroupId != null }.groupBy { it.installmentGroupId }
                ManageRow(
                    Icons.Rounded.CreditCard,
                    "할부 관리",
                    if (installmentGroups.isEmpty()) "" else "${installmentGroups.size}건",
                    onClick = { showInstallmentDialog = true },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.CalendarMonth,
                    "결제일 기준 보기",
                    if (paymentCards.isEmpty()) "등록된 카드 없음" else "${formatMonth(uiState.month)} · ${formatWon(paymentCards.sumOf { cardUsage(it, uiState.month, uiState.transactions).total })}",
                    onClick = { showCardUsageDialog = true },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.CreditCard,
                    "카드·결제일",
                    if (paymentCards.isEmpty()) "" else "${paymentCards.size}장",
                    onClick = { showPaymentCardsDialog = true },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.Rounded.Repeat, "반복 거래 관리", "${recurringRules.count { it.isActive }}개 사용 중", onClick = { showRecurringDialog = true })
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.Wallet, "구독·고정비 레이더",
                    "월 예정 ${formatWon(recurringRules.filter { it.isActive && it.type == TransactionType.EXPENSE }.sumOf { it.amount })}",
                    onClick = { showFixedRadarDialog = true },
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ManageSectionTitle("카테고리 비중")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showCategoryOrderDialog = true }) { Text("순서 변경") }
            }
        }
        item {
            CategoryWeightCard(
                uiState = uiState,
                budgets = categoryBudgets,
                onEditBudgets = { showCategoryBudgetsDialog = true },
                onAddCategory = { showCategoryDialog = true },
            )
        }
        item { ManageSectionTitle("예산") }
        item {
            FinanceCard {
                ManageRow(
                    Icons.Rounded.Wallet,
                    "월 예산",
                    uiState.budgetAmount?.let(::formatWon) ?: "예산 미설정",
                    onClick = { showBudgetDialog = true },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.AccountBalance,
                    "월 수입 목표",
                    monthlyIncomeTarget?.let(::formatWon) ?: "설정 안 됨",
                    onClick = { showIncomeTargetDialog = true },
                )
            }
        }
        item {
            FinanceCard { SettingSwitchRow(Icons.Rounded.Wallet, "남은 예산 다음 달 이월", "", uiState.budgetRollover, onSetBudgetRollover) }
        }
        item { ManageSectionTitle("홈 화면") }
        item {
            FinanceCard {
                ManageRow(
                    Icons.Rounded.Home,
                    "홈 카드 구성",
                    "${homeDashboardCards.size}개 표시 · 월 목표 지출은 항상 표시",
                    onClick = { showHomeCardsDialog = true },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.AutoAwesome,
                    "무지출 챌린지",
                    if (noSpendChallenge.enabled) "${noSpendChallenge.goalDays}일 목표 · 홈에서 진행 확인" else "목표 기간을 정하고 시작",
                    onClick = { showNoSpendDialog = true },
                )
            }
        }
        item { ManageSectionTitle("가계부 구성") }
        item {
            FinanceCard {
                ManageRow(Icons.Rounded.AccountBalance, "계좌·지갑과 이체", "", onClick = onOpenAccounts)
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.Rounded.Category, "카테고리", "", onClick = { showCategoryDialog = true })
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(
                    Icons.Rounded.Category,
                    "카테고리별 예산",
                    if (categoryBudgets.isEmpty()) "미설정" else "${categoryBudgets.size}개 설정",
                    onClick = { showCategoryBudgetsDialog = true },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.Rounded.AccountBalance, "결제수단", "${paymentMethods.joinToString(" · ")}", onClick = { showPaymentMethodsDialog = true })
            }
        }
        item { ManageSectionTitle("앱 설정") }
        item {
            FinanceCard {
                ManageRow(Icons.Rounded.Wallet, "전체 백업 내보내기", "", onClick = onExportBackup)
                ManageRow(Icons.Rounded.Wallet, "백업 복원", "", onClick = onRestoreBackup)
                ManageRow(Icons.Rounded.Wallet, "복원 전 백업 내보내기", "", onClick = onExportSafetyBackup)
                ManageRow(Icons.Rounded.CalendarMonth, "연말정산 자료 정리", "연간 수입·지출 및 CSV", onClick = {
                    selectedReportYear = java.time.LocalDate.now().year
                    showYearEndDialog = true
                })
                ManageRow(Icons.Rounded.CalendarMonth, "월별 PDF 리포트", formatMonth(uiState.month), onClick = onExportPdf)
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.Rounded.Security, "로그인·계정", accountStatus, onClick = onOpenAuth)
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.AutoMirrored.Rounded.HelpOutline, "기능 안내", "처음 보는 기능 다시 보기", onClick = onOpenGuide)
            }
        }
        item {
            FinanceCard {
                SettingSwitchRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "다크 모드",
                    message = "",
                    checked = darkTheme,
                    onCheckedChange = onDarkThemeChanged,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                SettingSwitchRow(
                    icon = Icons.Rounded.Palette,
                    title = "모션 줄이기",
                    message = "",
                    checked = reduceMotion,
                    onCheckedChange = onReduceMotionChanged,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                SettingSwitchRow(
                    icon = Icons.Rounded.Security,
                    title = "앱 잠금",
                    message = "생체 인증 또는 기기 잠금",
                    checked = appLockEnabled,
                    onCheckedChange = onSetAppLockEnabled,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                SettingSwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "예산·고정비 알림",
                    message = "예산 50%·초과 · 고정비 전날",
                    checked = financeRemindersEnabled,
                    onCheckedChange = onSetFinanceRemindersEnabled,
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
                    if (appNotificationsEnabled) "허용됨" else "알림 권한 필요",
                    onClick = onOpenAppNotificationSettings,
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                SettingSwitchRow(
                    icon = Icons.Rounded.Security,
                    title = "AI 알림 오탐 줄이기",
                    message = if (aiLoginRequired) "AI 판별은 로그인 필요" else "",
                    checked = aiNotificationClassificationEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) showAiNotificationConsent = true
                        else onSetAiNotificationClassificationEnabled(false)
                    },
                )
                HorizontalDivider(color = colors.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
                ManageRow(Icons.Rounded.Security, "개인정보와 데이터", "", onClick = { showPrivacyDialog = true })
            }
        }
        item {
            FinanceCard {
                ManageRow(Icons.Rounded.NotificationsActive, "알림 후보함", "확인 대기 ${pendingCandidates.size}건", onClick = onOpenCandidates)
            }
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
                "모아씀 ${com.moasseum.app.BuildConfig.VERSION_NAME}",
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            )
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            month = uiState.month,
            initialValue = uiState.baseBudgetAmount?.toString().orEmpty(),
            onDismiss = { showBudgetDialog = false },
            onSave = { input ->
                onUpdateBudget(input)
                showBudgetDialog = false
            },
        )
    }
    if (showIncomeTargetDialog) {
        IncomeTargetDialog(
            month = uiState.month,
            initialValue = monthlyIncomeTarget?.toString().orEmpty(),
            onDismiss = { showIncomeTargetDialog = false },
            onSave = onSaveMonthlyIncomeTarget,
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
    if (showCategoryOrderDialog) {
        CategoryOrderDialog(
            categoryOrder = categoryOrder,
            onDismiss = { showCategoryOrderDialog = false },
            onSave = onSaveCategoryOrder,
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
            paymentMethods = (paymentMethods + uiState.transactions.map { it.paymentMethod }).distinct(),
            onDismiss = { showPaymentCardsDialog = false },
            onSave = { cards ->
                onSavePaymentCards(cards)
                showPaymentCardsDialog = false
            },
        )
    }
    if (showCardUsageDialog) {
        CardUsageDialog(paymentCards, uiState.transactions, uiState.month,
            onDismiss = { showCardUsageDialog = false },
            onManageCards = { showCardUsageDialog = false; showPaymentCardsDialog = true },
        )
    }
    if (showFixedRadarDialog) {
        FixedExpenseRadarDialog(recurringRules, uiState,
            onDismiss = { showFixedRadarDialog = false },
            onManageRules = { showFixedRadarDialog = false; showRecurringDialog = true },
            onAddDetected = { pattern ->
                val today = java.time.LocalDate.now()
                val day = pattern.usualDay.coerceAtMost(today.lengthOfMonth())
                detectedRecurringPrefill = RecurringRule(
                    id = -1L, type = TransactionType.EXPENSE, amount = pattern.averageAmount,
                    merchant = pattern.merchant, dayOfMonth = pattern.usualDay,
                    nextOccurrenceDate = today.withDayOfMonth(day), categoryKey = pattern.categoryKey,
                    memo = "자동 감지 후보", paymentMethod = pattern.paymentMethod, isActive = true,
                )
                editingRecurringId = null
                showFixedRadarDialog = false
                showCreateRecurringDialog = true
            },
        )
    }
    if (showHomeCardsDialog) {
        HomeDashboardCardsDialog(
            selectedCards = homeDashboardCards,
            onDismiss = { showHomeCardsDialog = false },
            onSave = { cards ->
                onSaveHomeDashboardCards(cards)
                showHomeCardsDialog = false
            },
        )
    }
    if (showNoSpendDialog) {
        NoSpendChallengeDialog(
            settings = noSpendChallenge,
            onDismiss = { showNoSpendDialog = false },
            onSave = { enabled, goal ->
                onSaveNoSpendChallenge(enabled, goal)
                showNoSpendDialog = false
            },
        )
    }
    if (showInstallmentDialog) {
        InstallmentPlansDialog(
            transactions = uiState.transactions,
            paymentMethods = paymentMethods,
            onDismiss = { showInstallmentDialog = false },
            onSave = onAddInstallmentPlan,
            onDelete = onDeleteInstallmentPlan,
        )
    }
    if (showYearEndDialog) {
        YearEndSummaryDialog(
            year = selectedReportYear,
            transactions = uiState.transactions,
            onSelectYear = { selectedReportYear = it },
            onExport = onExportYearTransactions,
            onDismiss = { showYearEndDialog = false },
        )
    }
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("개인정보와 데이터") },
            text = {
                Text("가계부는 기기에 저장됩니다. AI 입력은 문장, 소비 분석·질문은 집계와 질문, AI 알림 판별은 선별된 알림 내용을 외부 AI 서버로 전송합니다. 영수증 사진은 기기에서만 처리합니다. 로그인만으로 기록이 공유되지는 않습니다.")
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
            text = { Text("저장된 거래, 계좌, 예산, 알림 후보, 반복 규칙을 모두 지워요. 이 작업은 되돌릴 수 없으니 필요하면 먼저 JSON 전체 백업을 내보내세요. 앱 설정과 로그인 상태는 유지됩니다.") },
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
                Text("카카오톡 등 대화 앱은 읽거나 AI로 보내지 않아요. 로그인 후 나머지 알림 중 금액과 금융 단서가 함께 있는 제목·내용만 Gemini로 전송해 입금·지출인지 판별합니다. 로그인하지 않았거나 AI 연결에 실패하면 기기 내 기본 검사로 감지를 유지합니다. 이 기능은 AI 사용량을 쓸 수 있고, 확인 전에는 거래로 저장하지 않아요.")
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
                editingRecurringId = null
                showRecurringDialog = false
                showCreateRecurringDialog = true
            },
            onEdit = { id ->
                editingRecurringId = id
                showRecurringDialog = false
                showCreateRecurringDialog = true
            },
            onToggle = onSetRecurringRuleActive,
            onDelete = onDeleteRecurringRule,
        )
    }
    if (showCreateRecurringDialog) {
        RecurringRuleDialog(
            initialRule = editingRecurringId?.let { id -> recurringRules.firstOrNull { it.id == id } } ?: detectedRecurringPrefill,
            paymentMethods = (paymentMethods + recurringRules.map { it.paymentMethod }).distinct(),
            onDismiss = { showCreateRecurringDialog = false },
            onSave = { amount, type, merchant, categoryKey, memo, paymentMethod, day ->
                editingRecurringId?.let { id -> onEditRecurringRule(id, amount, type, merchant, categoryKey, memo, paymentMethod, day) }
                    ?: onAddRecurringRule(amount, type, merchant, categoryKey, memo, paymentMethod, day)
                detectedRecurringPrefill = null
                showCreateRecurringDialog = false
                showRecurringDialog = true
            },
        )
    }
}

@Composable
private fun YearEndSummaryDialog(
    year: Int,
    transactions: List<com.moasseum.app.domain.Transaction>,
    onSelectYear: (Int) -> Unit,
    onExport: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val yearTransactions = remember(transactions, year) {
        transactions.filter { it.occurredDate.year == year }
    }
    val expenses = yearTransactions.filter { it.type == TransactionType.EXPENSE }
    val income = yearTransactions.filter { it.type == TransactionType.INCOME }
    val categoryTotals = expenses.groupBy { it.categoryKey }.mapValues { (_, rows) -> rows.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("연말정산 자료 정리") },
        text = {
            Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (year - 2..year).forEach { option ->
                        FilterChip(selected = year == option, onClick = { onSelectYear(option) }, label = { Text("${option}년") })
                    }
                }
                FinanceCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SummaryLine("연간 지출", formatWon(expenses.sumOf { it.amount }), colors.expense)
                        SummaryLine("연간 수입", formatWon(income.sumOf { it.amount }), colors.income)
                        SummaryLine("거래 수", "${yearTransactions.size}건", colors.textPrimary)
                    }
                }
                Text("지출 카테고리", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (categoryTotals.isEmpty()) Text("기록이 없습니다.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                categoryTotals.forEach { (key, amount) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(categoryLabel(key), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(formatWon(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("환급액·소득공제는 계산하지 않습니다. 카드사·현금영수증 자료와 함께 확인하세요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = { TextButton(onClick = { onExport(year) }) { Text("${year}년 CSV 내보내기", color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InstallmentPlansDialog(
    transactions: List<com.moasseum.app.domain.Transaction>,
    paymentMethods: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (Long, Int, java.time.LocalDate, String, String, String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val colors = LocalFinanceColors.current
    val saving = rememberSaveActionState()
    var totalText by rememberSaveable { mutableStateOf("") }
    var countText by rememberSaveable { mutableStateOf("6") }
    var dateText by rememberSaveable { mutableStateOf(java.time.LocalDate.now().toString()) }
    var merchant by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var categoryKey by rememberSaveable { mutableStateOf("SHOPPING") }
    var paymentMethod by rememberSaveable { mutableStateOf(paymentMethods.firstOrNull() ?: "카드") }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val groups = remember(transactions) {
        transactions.filter { it.installmentGroupId != null }
            .groupBy { it.installmentGroupId!! }
            .values
            .sortedByDescending { rows -> rows.minOfOrNull { it.occurredAt } ?: 0L }
    }
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("할부 관리") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                groups.forEach { rows ->
                    val first = rows.minByOrNull { it.installmentNumber ?: 0 }
                    FinanceCard {
                        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(first?.merchant.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("${rows.size}회 · 총 ${formatWon(rows.sumOf { it.amount })}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { deleteTarget = rows.first().installmentGroupId }) { Text("삭제", color = colors.expense) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = colors.divider)
                Text("할부 추가", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(totalText, { totalText = it.take(24); formError = null }, label = { Text("총 결제 금액") }, suffix = { Text("원") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !saving.busy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(countText, { countText = it.take(2); formError = null }, modifier = Modifier.weight(1f), label = { Text("개월") }, suffix = { Text("회") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !saving.busy)
                    OutlinedTextField(dateText, { dateText = it.take(10); formError = null }, modifier = Modifier.weight(1.6f), label = { Text("첫 청구일") }, singleLine = true, enabled = !saving.busy)
                }
                OutlinedTextField(merchant, { merchant = it.take(100); formError = null }, label = { Text("가맹점") }, singleLine = true, enabled = !saving.busy)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allCategorySpecs().forEach { spec ->
                        FilterChip(categoryKey == spec.key, { categoryKey = spec.key }, enabled = !saving.busy, label = { Text(categoryLabel(spec.key)) })
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paymentMethods.forEach { method ->
                        FilterChip(paymentMethod == method, { paymentMethod = method }, enabled = !saving.busy, label = { Text(method) })
                    }
                }
                OutlinedTextField(memo, { memo = it.take(300); formError = null }, label = { Text("메모 (선택)") }, singleLine = true, enabled = !saving.busy)
                Text("회차별 지출로 나눠 기록하며 총액은 보존됩니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                formError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
                saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving.busy, onClick = {
                val total = com.moasseum.app.domain.parseAmount(totalText)
                val count = countText.toIntOrNull()
                val date = runCatching { java.time.LocalDate.parse(dateText) }.getOrNull()
                val error = when {
                    total == null || count == null || count !in 2..60 -> "총액과 2~60회 할부를 확인해 주세요."
                    total < count.toLong() -> "회차마다 1원 이상이어야 해요."
                    date == null -> "첫 청구일을 YYYY-MM-DD로 입력해 주세요."
                    merchant.isBlank() -> "가맹점 이름을 입력해 주세요."
                    else -> null
                }
                if (error != null) formError = error
                else saving.save({ onSave(requireNotNull(total), requireNotNull(count), requireNotNull(date), categoryKey, merchant, memo, paymentMethod) })
            }) { Text("할부 추가", color = colors.accent) }
        },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("닫기") } },
    )
    deleteTarget?.let { groupId ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("할부 내역을 삭제할까요?") },
            text = { Text("이 할부의 모든 회차가 거래 내역에서 삭제됩니다.") },
            confirmButton = { TextButton(onClick = { onDelete(groupId); deleteTarget = null }) { Text("삭제", color = colors.expense) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun HomeDashboardCardsDialog(
    selectedCards: Set<String>,
    onDismiss: () -> Unit,
    onSave: suspend (Set<String>) -> Unit,
) {
    val saving = rememberSaveActionState()
    var draft by remember(selectedCards) { mutableStateOf(selectedCards) }
    val choices = listOf(
        HomeDashboardCards.TODAY_WEEK to ("오늘·이번 주 지출" to Icons.Rounded.CalendarMonth),
        HomeDashboardCards.TODAY_INSIGHT to ("오늘 소비 요약" to Icons.Rounded.Info),
        HomeDashboardCards.CATEGORIES to ("카테고리별 지출" to Icons.Rounded.Category),
        HomeDashboardCards.RECENT_TRANSACTIONS to ("최근 거래" to Icons.Rounded.Repeat),
        HomeDashboardCards.NO_SPEND_CHALLENGE to ("무지출 챌린지" to Icons.Rounded.AutoAwesome),
    )
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("홈 카드 구성") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("표시할 항목을 골라요. 월 목표 지출은 계속 보여요.", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
                choices.forEach { (key, choice) ->
                    SettingSwitchRow(
                        icon = choice.second,
                        title = choice.first,
                        message = "",
                        checked = key in draft,
                        onCheckedChange = { checked -> draft = if (checked) draft + key else draft - key },
                    )
                }
                saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = { TextButton(enabled = !saving.busy, onClick = { saving.save({ onSave(draft) }, onDismiss) }) { Text("저장") } },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun NoSpendChallengeDialog(
    settings: NoSpendChallengeSettings,
    onDismiss: () -> Unit,
    onSave: suspend (Boolean, Int) -> Unit,
) {
    val saving = rememberSaveActionState()
    var enabled by remember(settings) { mutableStateOf(settings.enabled) }
    var goalDays by remember(settings) { mutableStateOf(settings.goalDays) }
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("무지출 챌린지") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("하루 동안 지출 거래가 없으면 무지출 하루로 계산해요. 기록은 이 기기에만 저장돼요.", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                SettingSwitchRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "챌린지 사용",
                    message = if (enabled) "진행 중" else "중지됨",
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                )
                Text("목표 기간", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        FilterChip(
                            selected = goalDays == days,
                            onClick = { goalDays = days },
                            label = { Text("${days}일") },
                            enabled = !saving.busy,
                        )
                    }
                }
                saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = { TextButton(enabled = !saving.busy, onClick = { saving.save({ onSave(enabled, goalDays) }, onDismiss) }) { Text("저장") } },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun CategoryWeightCard(
    uiState: LedgerUiState,
    budgets: Map<String, Long>,
    onEditBudgets: () -> Unit,
    onAddCategory: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val specs = allCategorySpecs()
    val totals = uiState.categoryTotals.associateBy { it.key }
    val budgeted = specs.filter { it.key in budgets }
    val unbudgeted = specs.filterNot { it.key in budgets }
    var showUnbudgeted by rememberSaveable { mutableStateOf(false) }
    FinanceCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            budgeted.forEach { spec ->
                val spent = totals[spec.key]?.total ?: 0L
                val limit = budgets[spec.key] ?: 1L
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(categoryLabel(spec.key), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${formatWon(spent)} / ${formatWon(limit)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(
                        progress = { (spent.toFloat() / limit.coerceAtLeast(1L)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (spent > limit) colors.expense else spec.color,
                        trackColor = colors.surfaceRaised,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showUnbudgeted = !showUnbudgeted }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("예산 없음 (${unbudgeted.size})", color = colors.textSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onEditBudgets) { Text("예산 설정") }
                Icon(
                    imageVector = if (showUnbudgeted) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (showUnbudgeted) "미설정 카테고리 접기" else "미설정 카테고리 펼치기",
                    tint = colors.textSecondary,
                )
            }
            if (showUnbudgeted) {
                unbudgeted.forEach { spec ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onEditBudgets).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(categoryLabel(spec.key), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("한도 추가", color = colors.accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            OutlinedButton(onClick = onAddCategory, modifier = Modifier.fillMaxWidth()) { Text("+ 카테고리 추가") }
        }
    }
}

@Composable
private fun CategoryOrderDialog(
    categoryOrder: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (List<String>) -> Unit,
) {
    val saving = rememberSaveActionState()
    val specs = allCategorySpecs()
    val specsByKey = specs.associateBy { it.key }
    val initialOrder = specs.sortedBy { categoryOrder.indexOf(it.key).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
        .map { it.key }
    var draft by remember(initialOrder) { mutableStateOf(initialOrder) }
    val colors = LocalFinanceColors.current
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("카테고리 순서 변경") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("위아래 버튼으로 거래 입력과 목록에 표시할 순서를 바꿔요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                draft.forEachIndexed { index, key ->
                    val spec = specsByKey[key] ?: return@forEachIndexed
                    Surface(color = colors.surfaceRaised, shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 11.dp, end = 3.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(categoryLabel(key), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(enabled = index > 0 && !saving.busy, onClick = {
                                draft = draft.toMutableList().also { list ->
                                    val previous = list[index - 1]
                                    list[index - 1] = list[index]
                                    list[index] = previous
                                }
                            }) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "${categoryLabel(key)} 위로") }
                            IconButton(enabled = index < draft.lastIndex && !saving.busy, onClick = {
                                draft = draft.toMutableList().also { list ->
                                    val next = list[index + 1]
                                    list[index + 1] = list[index]
                                    list[index] = next
                                }
                            }) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "${categoryLabel(key)} 아래로") }
                        }
                    }
                }
                saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving.busy, onClick = { saving.save({ onSave(draft) }, onDismiss) }) { Text("저장") }
        },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun CategoryNamesDialog(
    onDismiss: () -> Unit,
    onSave: suspend (Map<String, String>) -> Unit,
    onRemove: suspend (String, Map<String, String>) -> Unit,
) {
    val saving = rememberSaveActionState()
    val currentLabels = LocalCategoryLabels.current
    val colors = LocalFinanceColors.current
    val initialNames = currentLabels
    var names by remember(initialNames) { mutableStateOf(initialNames) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var categoryToRemove by remember { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
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
                        enabled = !saving.busy,
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
                                enabled = !saving.busy,
                            )
                            IconButton(enabled = !saving.busy, onClick = { categoryToRemove = key }) {
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
                        enabled = !saving.busy,
                    )
                    TextButton(enabled = !saving.busy, onClick = {
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
                saving.error?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { saving.save({ onSave(names) }) },
                enabled = !saving.busy && names.values.all(String::isNotBlank) && names.values.map { it.trim().lowercase(Locale.ROOT) }.distinct().size == names.size,
            ) { Text("저장") }
        },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
    categoryToRemove?.let { key ->
        val label = names[key].orEmpty()
        AlertDialog(
            onDismissRequest = { if (!saving.busy) categoryToRemove = null },
            title = { Text("‘$label’ 카테고리를 삭제할까요?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("기존 거래, 반복 규칙, 알림 후보는 ‘기타’로 옮겨져요.")
                saving.error?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            } },
            confirmButton = {
                TextButton(enabled = !saving.busy, onClick = {
                    val updated = names - key
                    saving.save({ onRemove(key, updated) }) {
                        names = updated
                        categoryToRemove = null
                    }
                }) { Text("삭제", color = colors.expense) }
            },
            dismissButton = { TextButton(enabled = !saving.busy, onClick = { categoryToRemove = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun CategoryBudgetsDialog(
    budgets: Map<String, Long>,
    onDismiss: () -> Unit,
    onSave: suspend (Map<String, Long>) -> Unit,
) {
    val saving = rememberSaveActionState()
    val colors = LocalFinanceColors.current
    val specs = allCategorySpecs()
    var draft by remember(budgets) { mutableStateOf(budgets.mapValues { (_, amount) -> amount.toString() }) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("카테고리별 예산") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text("비워 두면 한도를 해제합니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                specs.forEach { spec ->
                    OutlinedTextField(
                        value = draft[spec.key].orEmpty(),
                        onValueChange = { value ->
                            draft = draft + (spec.key to value.take(24))
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${spec.label} 한도") },
                        trailingIcon = { Text("원", color = colors.textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !saving.busy,
                    )
                }
                errorMessage?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
                saving.error?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving.busy, onClick = {
                val invalid = draft.values.any { value ->
                    value.isNotBlank() && (value.toLongOrNull() == null || value.toLong() !in 1..1_000_000_000_000L)
                }
                if (invalid) {
                    errorMessage = "비워 두거나 1원 이상 금액을 입력해 주세요."
                } else {
                    val submitted = draft.mapNotNull { (key, value) -> value.toLongOrNull()?.takeIf { it > 0L }?.let { key to it } }.toMap()
                    saving.save({ onSave(submitted) })
                }
            }) { Text("저장", color = colors.accent) }
        },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun PaymentMethodsDialog(
    methods: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (List<String>) -> Unit,
) {
    val saving = rememberSaveActionState()
    var draft by remember(methods) { mutableStateOf(methods) }
    var newMethod by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("결제수단 관리") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("목록에서 제거해도 기존 거래는 유지됩니다.", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    draft.forEach { method ->
                        FilterChip(selected = true, onClick = { draft = draft - method }, enabled = !saving.busy, label = { Text("$method  ×") })
                    }
                }
                OutlinedTextField(
                    value = newMethod,
                    onValueChange = { newMethod = it.take(20); error = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("새 결제수단") },
                    singleLine = true,
                    isError = error,
                    enabled = !saving.busy,
                )
                TextButton(enabled = !saving.busy, onClick = {
                    val value = newMethod.trim()
                    if (value.isBlank() || value in draft || draft.size >= 36) {
                        error = true
                    } else {
                        draft = draft + value
                        newMethod = ""
                        error = false
                    }
                }) { Text("추가") }
                if (error) Text("중복되지 않은 이름을 입력해 주세요. 최대 36개까지 설정할 수 있어요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                if (draft.isEmpty()) Text("결제수단을 하나 이상 남겨주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = { TextButton(onClick = { saving.save({ onSave(draft) }) }, enabled = !saving.busy && draft.isNotEmpty()) { Text("저장") } },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun PaymentCardsDialog(
    cards: List<PaymentCard>,
    paymentMethods: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (List<PaymentCard>) -> Unit,
) {
    val saving = rememberSaveActionState()
    val colors = LocalFinanceColors.current
    var draft by remember(cards) { mutableStateOf(cards) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<PaymentCard?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("카드·결제일 관리") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (draft.isEmpty()) {
                    Text("등록된 카드가 없습니다.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
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
                                Text("${card.paymentMethod} · ${card.periodEndDay}일 마감", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(enabled = !saving.busy, onClick = {
                                editingCard = card
                                showEditor = true
                            }) { Text("수정", color = colors.accent) }
                            IconButton(enabled = !saving.busy, onClick = { draft = draft - card }) {
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
                    enabled = !saving.busy && draft.size < 12,
                ) { Text("+ 카드 추가", color = colors.accent) }
                if (draft.size >= 12) Text("카드는 최대 12장까지 등록할 수 있어요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                errorMessage?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
                saving.error?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = { TextButton(enabled = !saving.busy, onClick = { saving.save({ onSave(draft) }) }) { Text("저장", color = colors.accent) } },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )

    if (showEditor) {
        PaymentCardEditorDialog(
            initialCard = editingCard,
            paymentMethods = paymentMethods,
            onDismiss = { showEditor = false },
            onSave = { card ->
                val duplicate = draft.any { it.id != card.id && (it.name.equals(card.name, ignoreCase = true) || it.paymentMethod == card.paymentMethod) }
                if (duplicate) {
                    errorMessage = "카드 이름과 연결 결제수단은 카드마다 달라야 해요."
                    false
                } else {
                    draft = if (editingCard == null) draft + card else draft.map { existing -> if (existing.id == card.id) card else existing }
                    errorMessage = null
                    showEditor = false
                    true
                }
            },
        )
    }
}

@Composable
private fun PaymentCardEditorDialog(
    initialCard: PaymentCard?,
    paymentMethods: List<String>,
    onDismiss: () -> Unit,
    onSave: (PaymentCard) -> Boolean,
) {
    val colors = LocalFinanceColors.current
    var name by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.name.orEmpty()) }
    var dueDay by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.dueDay?.toString() ?: "25") }
    var periodEndDay by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.periodEndDay?.toString() ?: "31") }
    var periodOffset by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.periodEndMonthsBeforeDue ?: 1) }
    var linkedMethod by rememberSaveable(initialCard?.id) { mutableStateOf(initialCard?.paymentMethod) }
    var showError by rememberSaveable(initialCard?.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCard == null) "카드 추가" else "카드 수정") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    onValueChange = { dueDay = it.take(3); showError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("결제일 (1~31일)") },
                    suffix = { Text("일") },
                    singleLine = true,
                    isError = showError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = periodEndDay,
                    onValueChange = { periodEndDay = it.take(3); showError = false },
                    modifier = Modifier.fillMaxWidth(), label = { Text("이용기간 종료일 (1~31일)") },
                    singleLine = true, isError = showError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text("이용기간 종료월 · 결제월 기준", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "이번 달", 1 to "지난달", 2 to "두 달 전").forEach { (offset, label) ->
                        FilterChip(periodOffset == offset, { periodOffset = offset; showError = false }, label = { Text(label) })
                    }
                }
                Text("합산할 결제수단", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(linkedMethod == null, { linkedMethod = null }, label = { Text("카드 이름으로 새로 만들기") })
                    (paymentMethods + listOfNotNull(initialCard?.paymentMethod)).distinct().forEach { method ->
                        FilterChip(linkedMethod == method, { linkedMethod = method }, label = { Text(method) })
                    }
                }
                Text("해당 날짜가 없는 달은 말일로 계산합니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                if (showError) Text("이름·연결 결제수단의 중복과 날짜를 확인해 주세요. 이번 달 마감일은 결제일 이후일 수 없어요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedDueDay = dueDay.toIntOrNull()
                val parsedEndDay = periodEndDay.toIntOrNull()
                if (name.isBlank() || parsedDueDay == null || parsedDueDay !in 1..31 || parsedEndDay == null || parsedEndDay !in 1..31 || (periodOffset == 0 && parsedEndDay > parsedDueDay)) {
                    showError = true
                } else {
                    val saved = onSave(
                        PaymentCard(
                            id = initialCard?.id ?: "CARD_${UUID.randomUUID().toString().replace("-", "").take(12).uppercase(Locale.ROOT)}",
                            name = name.trim(),
                            dueDay = parsedDueDay,
                            paymentMethod = linkedMethod ?: name.trim(),
                            periodEndDay = parsedEndDay,
                            periodEndMonthsBeforeDue = periodOffset,
                        ),
                    )
                    if (!saved) showError = true
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
    onEdit: (Long) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val colors = LocalFinanceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("반복 거래 관리") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (rules.isEmpty()) {
                    Text("등록된 반복 거래가 없습니다.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                rules.forEach { rule ->
                    FinanceCard {
                        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(rule.merchant, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Switch(checked = rule.isActive, onCheckedChange = { onToggle(rule.id, it) })
                            }
                            Text("${if (rule.type == TransactionType.EXPENSE) "지출" else "수입"} ${formatWon(rule.amount)} · 매월 ${rule.dayOfMonth}일", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            Text("${if (rule.isActive) "다음 ${rule.nextOccurrenceDate}" else "일시 중지"} · ${categoryLabel(rule.categoryKey)} · ${rule.paymentMethod}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            Row {
                                TextButton(onClick = { onEdit(rule.id) }) { Text("수정") }
                                TextButton(onClick = { onDelete(rule.id) }) { Text("삭제", color = colors.expense) }
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
    initialRule: RecurringRule?,
    paymentMethods: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (String, TransactionType, String, String, String, String, String) -> Unit,
) {
    val saving = rememberSaveActionState()
    val colors = LocalFinanceColors.current
    var amount by rememberSaveable(initialRule?.id) { mutableStateOf(initialRule?.amount?.toString().orEmpty()) }
    var merchant by rememberSaveable(initialRule?.id) { mutableStateOf(initialRule?.merchant.orEmpty()) }
    var day by rememberSaveable(initialRule?.id) { mutableStateOf(initialRule?.dayOfMonth?.toString() ?: "1") }
    var typeName by rememberSaveable(initialRule?.id) { mutableStateOf(initialRule?.type?.name ?: TransactionType.EXPENSE.name) }
    var categoryKey by rememberSaveable(initialRule?.id) { mutableStateOf(initialRule?.categoryKey ?: "LIVING") }
    var paymentMethod by rememberSaveable(initialRule?.id) { mutableStateOf(initialRule?.paymentMethod ?: paymentMethods.firstOrNull().orEmpty()) }
    var memo by rememberSaveable(initialRule?.id) { mutableStateOf(initialRule?.memo.orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text(if (initialRule == null || initialRule.id < 0) "매월 반복 거래 추가" else "반복 거래 수정") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(type == TransactionType.EXPENSE, { typeName = TransactionType.EXPENSE.name }, enabled = !saving.busy, label = { Text("지출") })
                    FilterChip(type == TransactionType.INCOME, { typeName = TransactionType.INCOME.name }, enabled = !saving.busy, label = { Text("수입") })
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it.take(24); showError = false }, label = { Text("금액") }, suffix = { Text("원") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !saving.busy)
                OutlinedTextField(value = merchant, onValueChange = { merchant = it; showError = false }, label = { Text("가맹점 또는 이름") }, singleLine = true, enabled = !saving.busy)
                OutlinedTextField(value = day, onValueChange = { day = it.take(3); showError = false }, label = { Text("매월 며칠 (1~31)") }, suffix = { Text("일") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !saving.busy)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allCategorySpecs().forEach { spec ->
                        FilterChip(categoryKey == spec.key, { categoryKey = spec.key }, enabled = !saving.busy, label = { Text(categoryLabel(spec.key)) })
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paymentMethods.forEach { method ->
                        FilterChip(paymentMethod == method, { paymentMethod = method }, enabled = !saving.busy, label = { Text(method) })
                    }
                }
                OutlinedTextField(value = memo, onValueChange = { memo = it }, label = { Text("메모 (선택)") }, singleLine = true, enabled = !saving.busy)
                if (showError) Text("금액, 이름, 반복 날짜를 확인해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                saving.error?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
                if (initialRule != null) Text("변경은 다음 기록부터 적용됩니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            TextButton(enabled = !saving.busy, onClick = {
                if (com.moasseum.app.domain.parseAmount(amount) == null || merchant.isBlank() || day.toIntOrNull() !in 1..31) showError = true
                else saving.save({ onSave(amount, type, merchant, categoryKey, memo, paymentMethod, day) })
            }) {
                Text("저장", color = colors.accent)
            }
        },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
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
                            UpdateCheckState.Idle -> "현재 ${com.moasseum.app.BuildConfig.VERSION_NAME}"
                            UpdateCheckState.Checking -> "확인 중…"
                            UpdateCheckState.UpToDate -> "최신 버전"
                            is UpdateCheckState.Available -> "${updateState.release.tagName} 사용 가능"
                            is UpdateCheckState.Downloading -> "다운로드 ${updateState.progressPercent}%"
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
                        Text("업데이트", color = colors.accent)
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
            if (message.isNotBlank()) Text(message, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
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
            if (message.isNotBlank()) Text(message, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BudgetDialog(
    month: java.time.YearMonth,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Unit,
) {
    val saving = rememberSaveActionState()
    var input by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var showError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("한 달 목표 지출 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${formatMonth(month)} 기준 목표", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(24); showError = false },
                    label = { Text("금액") },
                    suffix = { Text("원") },
                    singleLine = true,
                    isError = showError,
                    enabled = !saving.busy,
                )
                if (showError) Text("1원 이상 입력해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving.busy, onClick = {
                if (com.moasseum.app.domain.parseAmount(input) == null) showError = true
                else saving.save({ onSave(input) })
            }) { Text("저장") }
        },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun IncomeTargetDialog(
    month: java.time.YearMonth,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: suspend (Long?) -> Unit,
) {
    val saving = rememberSaveActionState()
    var input by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var showError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("월 수입 목표 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${formatMonth(month)} 기준 목표 수입", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(24); showError = false },
                    label = { Text("금액") },
                    suffix = { Text("원") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showError,
                    enabled = !saving.busy,
                )
                Text("비워 두면 이 달의 수입 목표를 해제합니다.", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
                if (showError) Text("1원 이상 입력해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (initialValue.isNotBlank()) {
                    TextButton(enabled = !saving.busy, onClick = { saving.save({ onSave(null) }, onDismiss) }) { Text("해제") }
                }
                TextButton(enabled = !saving.busy, onClick = {
                    if (input.isBlank()) saving.save({ onSave(null) }, onDismiss)
                    else {
                        val amount = com.moasseum.app.domain.parseAmount(input)
                        if (amount == null) showError = true
                        else saving.save({ onSave(amount) }, onDismiss)
                    }
                }) { Text("저장") }
            }
        },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
}
