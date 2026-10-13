package com.moasseum.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.moasseum.app.ui.components.FinanceTextField as OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.AiAdviceCards
import com.moasseum.app.domain.NoSpendChallengeSettings
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.domain.formatLongDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.noSpendStreakDays
import com.moasseum.app.domain.noSpendChallengeHeadline
import com.moasseum.app.ui.components.AddMode
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.FinanceProgressBar
import com.moasseum.app.ui.components.formatDisplayWon
import com.moasseum.app.ui.components.FittedAmountText
import com.moasseum.app.ui.components.TransactionRow
import com.moasseum.app.ui.components.LocalProfileAvatar
import com.moasseum.app.ui.components.ProfileEditorDialog
import com.moasseum.app.ui.components.categoryColor
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.ui.theme.LocalFinanceMotion
import java.time.YearMonth
import java.time.LocalDate
import java.time.DayOfWeek
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

private enum class HomeTab(val label: String) {
    SUMMARY("요약"),
    INSIGHTS("인사이트"),
    REPORT("리포트"),
}

@Composable
fun HomeScreen(
    uiState: LedgerUiState,
    aiAnalysisState: SpendingAnalysisState,
    onGenerateAiAnalysis: () -> Unit,
    aiQuestionState: SpendingQuestionState,
    onAskAiQuestion: (String) -> Unit,
    categoryBudgets: Map<String, Long>,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onAdd: (AddMode) -> Unit,
    onOpenManage: () -> Unit,
    onOpenManageFeature: (String) -> Unit = {},
    onOpenHistory: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenHelp: () -> Unit,
    onOpenNotifications: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onPickReceipt: () -> Unit,
    onTakeReceipt: () -> Unit,
    displayName: String,
    profilePhotoUri: String? = null,
    onSaveDisplayName: (String) -> Unit = {},
    onSaveProfilePhotoUri: (String?) -> Unit = {},
    pendingCount: Int = 0,
    homeDashboardCards: Set<String> = HomeDashboardCards.defaults,
    aiAdviceCards: Set<String> = AiAdviceCards.defaults,
    noSpendChallenge: NoSpendChallengeSettings = NoSpendChallengeSettings(),
    activeFixedCostCount: Int = 0,
    activeFixedCostMonthlyAmount: Long = 0L,
    scrollToTopRequest: Int = 0,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(HomeTab.SUMMARY.name) }
    var showProfileEditor by rememberSaveable { mutableStateOf(false) }
    var showAiAssistant by rememberSaveable { mutableStateOf(false) }
    var showAiLaunchTransition by remember { mutableStateOf(false) }
    var aiButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var homeRootBounds by remember { mutableStateOf(Rect.Zero) }
    val reduceMotion = LocalFinanceMotion.current.reduceMotion
    val context = LocalContext.current
    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            profilePhotoUri?.let { previous ->
                if (previous != uri.toString()) runCatching {
                    context.contentResolver.releasePersistableUriPermission(Uri.parse(previous), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            onSaveProfilePhotoUri(uri.toString())
        }
    }
    val availableTabs = HomeTab.entries
    // Older saved state may still contain the removed AI tab name.
    val selectedTab = runCatching { HomeTab.valueOf(selectedTabName) }.getOrDefault(HomeTab.SUMMARY)
        .takeIf { it in availableTabs } ?: HomeTab.SUMMARY
    val showAiEntry = AiAdviceCards.HOME_SUMMARY in aiAdviceCards || AiAdviceCards.HOME_REPORT in aiAdviceCards
    val isCurrentMonth = uiState.month == YearMonth.now()
    val listState = rememberLazyGridState()
    // The Fold4 inner display is about 690dp wide in portrait. Keep the cover
    // screen unchanged and use two dashboard columns only when the usable
    // window is genuinely wide enough for them.
    val expandedLayout = LocalConfiguration.current.screenWidthDp >= 640

    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) listState.animateScrollToItem(0)
    }
    LaunchedEffect(selectedTab, availableTabs) {
        val savedTab = runCatching { HomeTab.valueOf(selectedTabName) }.getOrNull()
        if (savedTab !in availableTabs) selectedTabName = HomeTab.SUMMARY.name
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { homeRootBounds = it.boundsInRoot() },
    ) {
    LazyVerticalGrid(
        columns = if (expandedLayout) GridCells.Adaptive(minSize = 320.dp) else GridCells.Fixed(1),
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(if (expandedLayout) 10.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeHeader(
                onOpenManage = onOpenManage,
                onOpenHelp = onOpenHelp,
                onOpenNotifications = onOpenNotifications,
                displayName = displayName,
                profilePhotoUri = profilePhotoUri,
                onOpenProfile = { showProfileEditor = true },
                pendingCount = pendingCount,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            QuickCaptureCard(
                onAdd = onAdd,
                onStartVoiceInput = onStartVoiceInput,
                onPickReceipt = onPickReceipt,
                onTakeReceipt = onTakeReceipt,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeTabs(
                    tabs = availableTabs,
                    selected = selectedTab,
                    onSelect = { selectedTabName = it.name },
                    modifier = if (showAiEntry) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                )
                if (showAiEntry) {
                    HomeAiAssistantButton(
                        onClick = {
                            if (reduceMotion || aiButtonBounds.width <= 0f || aiButtonBounds.height <= 0f) {
                                showAiAssistant = true
                            } else {
                                showAiLaunchTransition = true
                            }
                        },
                        onPositioned = {
                            val bounds = it.boundsInRoot()
                            aiButtonBounds = Rect(
                                left = bounds.left - homeRootBounds.left,
                                top = bounds.top - homeRootBounds.top,
                                right = bounds.right - homeRootBounds.left,
                                bottom = bounds.bottom - homeRootBounds.top,
                            )
                        },
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            val reduceMotion = LocalFinanceMotion.current.reduceMotion
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (reduceMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        (fadeIn(tween(190, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                animationSpec = tween(190, easing = FastOutSlowInEasing),
                                initialOffsetY = { height -> height / 36 },
                            )) togetherWith
                            (fadeOut(tween(110)) + slideOutVertically(
                                animationSpec = tween(110),
                                targetOffsetY = { height -> -height / 54 },
                            ))
                    }
                },
                label = "homeDashboardTabContent",
            ) { tab ->
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("home_tab_content_${tab.name.lowercase()}"),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    when (tab) {
                        HomeTab.SUMMARY -> {
                            MonthlySummaryCard(uiState, onOpenManageFeature, onSelectMonth)
                            // Live cards belong to the present, not to a historical month.
                            if (isCurrentMonth && noSpendChallenge.enabled && HomeDashboardCards.NO_SPEND_CHALLENGE in homeDashboardCards) {
                                NoSpendChallengeCard(uiState, noSpendChallenge)
                            }
                            if (isCurrentMonth && HomeDashboardCards.TODAY_WEEK in homeDashboardCards) TodayAndWeekCard(uiState)
                            if (isCurrentMonth && HomeDashboardCards.TODAY_INSIGHT in homeDashboardCards) TodayInsightCard(uiState)
                            val hasCategories = HomeDashboardCards.CATEGORIES in homeDashboardCards
                            val hasRecent = HomeDashboardCards.RECENT_TRANSACTIONS in homeDashboardCards &&
                                uiState.monthTransactions.any { it.type != com.moasseum.app.domain.TransactionType.TRANSFER }
                            if (expandedLayout && hasCategories && hasRecent) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().testTag("home_summary_secondary_cards_wide"),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    CategorySpendingCard(uiState, categoryBudgets, onOpenHistory, Modifier.weight(1f))
                                    RecentTransactionsCard(uiState, onOpenHistory, onOpenTransaction, Modifier.weight(1f))
                                }
                            } else {
                                if (hasCategories) CategorySpendingCard(uiState, categoryBudgets, onOpenHistory)
                                if (hasRecent) RecentTransactionsCard(uiState, onOpenHistory, onOpenTransaction)
                            }
                        }

                        HomeTab.INSIGHTS -> InsightsContent(
                            uiState = uiState,
                            noSpendChallenge = noSpendChallenge,
                            onOpenManageFeature = onOpenManageFeature,
                            activeFixedCostCount = activeFixedCostCount,
                            activeFixedCostMonthlyAmount = activeFixedCostMonthlyAmount,
                        )
                        HomeTab.REPORT -> ReportContent(
                            uiState = uiState,
                            onSelectMonth = onSelectMonth,
                            onOpenManageFeature = onOpenManageFeature,
                            onExportPdf = onExportPdf,
                            onExportCsv = onExportCsv,
                            aiState = aiAnalysisState,
                            onGenerateAiReport = onGenerateAiAnalysis,
                            showAiReport = AiAdviceCards.HOME_REPORT in aiAdviceCards,
                        )
                    }
                }
            }
        }
    }
    if (showAiLaunchTransition && showAiEntry) {
        AiLaunchTransitionOverlay(
            originBounds = aiButtonBounds,
            onAnimationFinished = {
                showAiLaunchTransition = false
                showAiAssistant = true
            },
        )
    }
    }

    if (showProfileEditor) {
        ProfileEditorDialog(
            initialName = displayName,
            photoUri = profilePhotoUri,
            onDismiss = { showProfileEditor = false },
            onSave = { name ->
                onSaveDisplayName(name)
                showProfileEditor = false
            },
            onChoosePhoto = {
                profilePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = {
                profilePhotoUri?.let { current -> runCatching {
                    context.contentResolver.releasePersistableUriPermission(Uri.parse(current), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } }
                onSaveProfilePhotoUri(null)
            },
        )
    }
    if (showAiAssistant && showAiEntry) {
        AiAssistantBottomSheet(
            uiState = uiState,
            analysisState = aiAnalysisState,
            onGenerateAnalysis = onGenerateAiAnalysis,
            questionState = aiQuestionState,
            onAskQuestion = onAskAiQuestion,
            showSummary = AiAdviceCards.HOME_SUMMARY in aiAdviceCards,
            showReport = AiAdviceCards.HOME_REPORT in aiAdviceCards,
            onDismiss = { showAiAssistant = false },
        )
    }
}

@Composable
private fun NoSpendChallengeCard(
    uiState: LedgerUiState,
    settings: NoSpendChallengeSettings,
) {
    val colors = LocalFinanceColors.current
    val today = LocalDate.now()
    val startDate = settings.startDate ?: return
    val streak = noSpendStreakDays(uiState.transactions, startDate, today)
    val progress = (streak.toFloat() / settings.goalDays.coerceAtLeast(1)).coerceIn(0f, 1f)
    val reached = streak >= settings.goalDays.coerceAtLeast(1)
    FinanceCard(highlighted = reached) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("무지출 챌린지", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${settings.goalDays}일 목표", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = noSpendChallengeHeadline(uiState.transactions, settings, today),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (reached) colors.success else colors.textPrimary,
            )
            FinanceProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = if (reached) colors.success else colors.accent,
                trackColor = colors.surfaceOverlay,
                height = 6.dp,
            )
            Text("${streak.coerceAtMost(settings.goalDays)} / ${settings.goalDays}일", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HomeHeader(
    onOpenManage: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenNotifications: () -> Unit,
    displayName: String,
    profilePhotoUri: String?,
    onOpenProfile: () -> Unit,
    pendingCount: Int,
) {
    val colors = LocalFinanceColors.current
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11 -> "좋은 아침이에요"
        in 12..17 -> "좋은 오후예요"
        else -> "좋은 밤이에요"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalProfileAvatar(
            displayName = displayName,
            photoUri = profilePhotoUri,
            size = 38.dp,
            modifier = Modifier.clickable(onClick = onOpenProfile),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting, ${displayName.ifBlank { "모아씀" }}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatLongDate(java.time.LocalDate.now()),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HeaderIconButton(Icons.AutoMirrored.Rounded.HelpOutline, "도움말", onClick = onOpenHelp)
        Spacer(Modifier.width(4.dp))
        HeaderIconButton(Icons.Rounded.NotificationsNone, "알림", onClick = onOpenNotifications, badgeCount = pendingCount)
        Spacer(Modifier.width(4.dp))
        HeaderIconButton(Icons.Rounded.Settings, "관리 설정", onOpenManage)
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    badgeCount: Int = 0,
) {
    val colors = LocalFinanceColors.current
    Surface(
        modifier = Modifier.size(34.dp),
        onClick = onClick,
        color = colors.surfaceRaised,
        shape = RoundedCornerShape(11.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            BadgedBox(badge = { if (badgeCount > 0) Badge { Text(if (badgeCount > 9) "9+" else badgeCount.toString()) } }) {
                Icon(icon, contentDescription = contentDescription, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun QuickCaptureCard(
    onAdd: (AddMode) -> Unit,
    onStartVoiceInput: () -> Unit,
    onPickReceipt: () -> Unit,
    onTakeReceipt: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(highlighted = true) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f).clickable(role = androidx.compose.ui.semantics.Role.Button) { onAdd(AddMode.AI_INPUT) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("예: 점심 8,000원", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            QuickCaptureAction(Icons.Rounded.KeyboardVoice, "음성으로 입력", onStartVoiceInput)
            QuickCaptureAction(Icons.Rounded.CameraAlt, "영수증 촬영", onTakeReceipt)
            QuickCaptureAction(Icons.Rounded.PhotoLibrary, "사진에서 거래 가져오기", onPickReceipt)
        }
    }
}

@Composable
private fun QuickCaptureAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Surface(
        modifier = Modifier.size(38.dp),
        onClick = onClick,
        color = colors.surfaceOverlay,
        contentColor = colors.accent,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun HomeTabs(
    tabs: List<HomeTab>,
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised, RoundedCornerShape(24.dp))
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEach { tab ->
            val tabColor by animateColorAsState(if (tab == selected) colors.accent else Color.Transparent, tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 140), label = "homeTab")
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 33.dp)
                    .selectable(
                        selected = tab == selected,
                        role = Role.Tab,
                        onClick = { onSelect(tab) },
                    )
                    .testTag("home_tab_${tab.name.lowercase()}"),
                color = tabColor,
                contentColor = if (tab == selected) MaterialTheme.colorScheme.onPrimary else colors.textSecondary,
                shape = RoundedCornerShape(19.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = tab.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun HomeAiAssistantButton(
    onClick: () -> Unit,
    onPositioned: (LayoutCoordinates) -> Unit,
) {
    val colors = LocalFinanceColors.current
    Surface(
        modifier = Modifier
            .size(width = 54.dp, height = 48.dp)
            .onGloballyPositioned(onPositioned)
            .testTag("home_ai_assistant_button")
            .semantics { contentDescription = "AI 소비 코치 열기" },
        onClick = onClick,
        color = colors.accent.copy(alpha = 0.14f),
        contentColor = colors.accent,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(4.dp))
            Text("AI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AiLaunchTransitionOverlay(
    originBounds: Rect,
    onAnimationFinished: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val reduceMotion = LocalFinanceMotion.current.reduceMotion
    val progress = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
            onAnimationFinished()
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(430, easing = FastOutSlowInEasing),
            )
            onAnimationFinished()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_ai_launch_transition")
            .semantics { contentDescription = "AI 소비 코치 팝업 여는 중" }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        val density = LocalDensity.current
        val targetTop = with(density) { maxHeight.toPx() * 0.20f }
        val targetPanelWidth = maxWidth.coerceAtMost(640.dp)
        val panelLeftPx = with(density) { ((maxWidth - targetPanelWidth) / 2f).toPx() }
        val targetIconCenterX = panelLeftPx + with(density) { 59.dp.toPx() }
        val targetIconCenterY = targetTop + with(density) { 61.dp.toPx() }
        val fraction = progress.value
        val currentIconCenterX = originBounds.center.x + (targetIconCenterX - originBounds.center.x) * fraction
        val currentIconCenterY = originBounds.center.y + (targetIconCenterY - originBounds.center.y) * fraction
        val iconSizePx = with(density) { 19.dp.toPx() + (39.dp.toPx() - 19.dp.toPx()) * fraction }
        val panelTitleAlpha = ((fraction - 0.57f) / 0.36f).coerceIn(0f, 1f)

        Canvas(Modifier.fillMaxSize()) {
            val drawProgress = fraction.coerceIn(0f, 1f)
            drawRect(Color.Black.copy(alpha = 0.46f * drawProgress))

            val left = originBounds.left + (panelLeftPx - originBounds.left) * drawProgress
            val top = originBounds.top + (size.height * 0.20f - originBounds.top) * drawProgress
            val targetRight = size.width - panelLeftPx
            val right = originBounds.right + (targetRight - originBounds.right) * drawProgress
            val bottom = originBounds.bottom + (size.height - originBounds.bottom) * drawProgress
            val topRadius = with(density) { 16.dp.toPx() + (28.dp.toPx() - 16.dp.toPx()) * drawProgress }
            val bottomRadius = with(density) { 16.dp.toPx() * (1f - drawProgress) }
            val morphPath = Path().apply {
                moveTo(left + topRadius, top)
                lineTo(right - topRadius, top)
                quadraticTo(right, top, right, top + topRadius)
                lineTo(right, bottom - bottomRadius)
                quadraticTo(right, bottom, right - bottomRadius, bottom)
                lineTo(left + bottomRadius, bottom)
                quadraticTo(left, bottom, left, bottom - bottomRadius)
                lineTo(left, top + topRadius)
                quadraticTo(left, top, left + topRadius, top)
                close()
            }
            drawPath(
                path = morphPath,
                color = androidx.compose.ui.graphics.lerp(
                    colors.accent.copy(alpha = 0.14f),
                    colors.surfaceRaised,
                    drawProgress,
                ),
            )
            clipPath(morphPath) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.accent.copy(alpha = 0.17f * drawProgress), colors.surfaceRaised),
                        startY = top,
                        endY = (top + with(density) { 190.dp.toPx() }).coerceAtLeast(top + 1f),
                    ),
                )
            }
            if (drawProgress < 0.99f) {
                drawPath(
                    path = morphPath,
                    color = colors.accent.copy(alpha = 0.38f * (1f - drawProgress)),
                    style = Stroke(width = with(density) { 1.2.dp.toPx() }),
                )
            }

            val burst = (1f - drawProgress).coerceIn(0f, 1f)
            val haloRadius = with(density) { 22.dp.toPx() + 33.dp.toPx() * drawProgress }
            drawCircle(
                color = colors.accent.copy(alpha = 0.22f * burst),
                radius = haloRadius,
                center = Offset(currentIconCenterX, currentIconCenterY),
            )
            drawCircle(
                color = colors.income.copy(alpha = 0.9f * burst),
                radius = with(density) { 10.dp.toPx() + 14.dp.toPx() * drawProgress },
                center = Offset(currentIconCenterX, currentIconCenterY),
                style = Stroke(width = with(density) { 1.3.dp.toPx() }),
            )
            repeat(6) { index ->
                val angle = (index * 60f - 90f) * (Math.PI.toFloat() / 180f)
                val particleDistance = with(density) { 16.dp.toPx() + 30.dp.toPx() * drawProgress }
                val particle = Offset(
                    x = currentIconCenterX + cos(angle) * particleDistance,
                    y = currentIconCenterY + sin(angle) * particleDistance,
                )
                val particleColor = if (index % 2 == 0) colors.income else colors.accent
                drawCircle(
                    color = particleColor.copy(alpha = 0.95f * burst),
                    radius = with(density) { 1.8.dp.toPx() * burst },
                    center = particle,
                )
                if (index % 2 == 0 && burst > 0f) {
                    val ray = with(density) { 3.6.dp.toPx() * burst }
                    drawLine(
                        color = particleColor.copy(alpha = 0.7f * burst),
                        start = Offset(particle.x - ray, particle.y),
                        end = Offset(particle.x + ray, particle.y),
                        strokeWidth = with(density) { 0.8.dp.toPx() },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (currentIconCenterX - iconSizePx / 2f).roundToInt(),
                        y = (currentIconCenterY - iconSizePx / 2f).roundToInt(),
                    )
                }
                .size(with(density) { iconSizePx.toDp() })
                .graphicsLayer { rotationZ = -32f * (1f - fraction) }
                .background(Brush.linearGradient(listOf(colors.accent, colors.income, colors.accent)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }

        Column(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = panelLeftPx.roundToInt() + with(density) { 98.dp.roundToPx() },
                        y = targetTop.roundToInt() + with(density) { 38.dp.roundToPx() },
                    )
                }
                .graphicsLayer { alpha = panelTitleAlpha },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("AI 소비 코치", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text("이번 달 흐름을 함께 살펴봐요", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        }
    }
}

@Composable
private fun AiLaunchOrb(modifier: Modifier = Modifier) {
    val colors = LocalFinanceColors.current
    Box(modifier.size(58.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().testTag("home_ai_launch_orb")) {
            drawCircle(colors.accent.copy(alpha = 0.12f), radius = size.minDimension * 0.47f)
            drawCircle(colors.income.copy(alpha = 0.25f), radius = size.minDimension * 0.39f, style = Stroke(width = 1.dp.toPx()))
        }
        Box(
            modifier = Modifier
                .size(39.dp)
                .background(
                    brush = Brush.linearGradient(listOf(colors.accent, colors.income, colors.accent)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiAssistantBottomSheet(
    uiState: LedgerUiState,
    analysisState: SpendingAnalysisState,
    onGenerateAnalysis: () -> Unit,
    questionState: SpendingQuestionState,
    onAskQuestion: (String) -> Unit,
    showSummary: Boolean,
    showReport: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val motion = LocalFinanceMotion.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bodyVisibility = remember { MutableTransitionState(false) }
    val labelAlpha by animateFloatAsState(
        targetValue = if (bodyVisibility.targetState) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (motion.reduceMotion) 0 else 300,
            delayMillis = if (motion.reduceMotion) 0 else 145,
            easing = FastOutSlowInEasing,
        ),
        label = "aiAssistantTitleEntrance",
    )
    LaunchedEffect(Unit) { bodyVisibility.targetState = true }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceRaised,
        contentColor = colors.textPrimary,
        dragHandle = null,
        modifier = Modifier.testTag("home_ai_assistant_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.88f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
                .testTag("home_ai_sheet_content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp, bottom = 2.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .background(colors.textSecondary.copy(alpha = 0.34f), CircleShape),
            )
            FinanceCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 11.dp, end = 8.dp, bottom = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AiLaunchOrb()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                alpha = labelAlpha
                                translationY = (1f - labelAlpha) * 7.dp.toPx()
                            },
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("AI 소비 코치", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("이번 달 흐름을 함께 살펴봐요", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "AI 창 닫기", tint = colors.textSecondary)
                    }
                }
            }
            AnimatedVisibility(
                visibleState = bodyVisibility,
                enter = if (motion.reduceMotion) EnterTransition.None else
                    fadeIn(tween(230, easing = FastOutSlowInEasing)) + slideInVertically(
                        animationSpec = tween(230, easing = FastOutSlowInEasing),
                        initialOffsetY = { height -> height / 18 },
                    ),
                exit = if (motion.reduceMotion) ExitTransition.None else fadeOut(tween(120)),
            ) {
                AiAnalysisContent(
                    uiState = uiState,
                    state = analysisState,
                    onGenerate = onGenerateAnalysis,
                    questionState = questionState,
                    onAskQuestion = onAskQuestion,
                    showSummary = showSummary,
                    showReport = showReport,
                )
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(
    uiState: LedgerUiState,
    onOpenManageFeature: (String) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
) {
    val colors = LocalFinanceColors.current
    val change = uiState.budgetPeriodExpenseTotal - uiState.previousBudgetPeriodExpenseTotal
    val budget = uiState.budgetAmount
    val budgetCycle = uiState.budgetCycle
    val isCurrentBudgetCycle = LocalDate.now() in budgetCycle.startDate..budgetCycle.endDate
    val budgetCycleLabel = budgetCycle.label
    val progress = if (budget != null && budget > 0) {
        (uiState.budgetPeriodExpenseTotal.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(progress, tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 240), label = "budgetProgress")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(
                onClick = { onSelectMonth(uiState.month.minusMonths(1)) },
                modifier = Modifier.size(32.dp),
            ) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "지난달") }
            TextButton(
                onClick = { onSelectMonth(YearMonth.now()) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_month_period_selector")
                    .semantics {
                        contentDescription = "${formatMonth(uiState.month)}. 예산 기간 $budgetCycleLabel. 누르면 이번 달로 이동"
                    },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatMonth(uiState.month),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        "예산 기간 · $budgetCycleLabel",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            androidx.compose.material3.IconButton(
                onClick = { onSelectMonth(uiState.month.plusMonths(1)) },
                modifier = Modifier.size(32.dp),
            ) { Icon(Icons.Rounded.ChevronRight, contentDescription = "다음달") }
            Text("${uiState.budgetPeriodExpenseCount}건", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
        FittedAmountText(
            formatDisplayWon(uiState.budgetPeriodExpenseTotal),
            MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = { onOpenManageFeature("budget") },
                modifier = Modifier.testTag("home_budget_target_action"),
                color = Color.Transparent,
                contentColor = colors.accent,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = budget?.let { "목표 ${formatDisplayWon(it)}" } ?: "목표 설정",
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (budget != null) {
            FinanceProgressBar(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth().testTag("home_budget_progress"),
                color = if (progress > 0.9f) colors.expense else colors.accent,
                trackColor = colors.surfaceOverlay,
                height = 6.dp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val remaining = budget - uiState.budgetPeriodExpenseTotal
                Text(
                    text = "${(uiState.budgetPeriodExpenseTotal.toDouble() / budget.coerceAtLeast(1L) * 100).toInt()}% 사용",
                    color = if (progress > 0.9f) colors.expense else colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        remaining < 0 -> "예산 초과 ${formatDisplayWon(-remaining)}"
                        remaining == 0L -> "예산 한도 도달"
                        else -> "남은 예산 ${formatDisplayWon(remaining)}"
                    },
                    color = if (remaining < 0) colors.expense else colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val pace = com.moasseum.app.domain.budgetPace(uiState)
            if (uiState.carriedBudgetAmount > 0) Text("지난 예산 기간 이월 ${formatDisplayWon(uiState.carriedBudgetAmount)} 포함", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            if (isCurrentBudgetCycle) {
                if (budget != null && pace.remainingDays > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("남은 ${pace.remainingDays}일 · 하루 ${formatDisplayWon(pace.dailyAllowance)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        pace.projectedExpense?.let { Text("예상 ${formatDisplayWon(it)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium) }
                    }
                } else pace.projectedExpense?.let { Text("현재 속도 기간 종료 예상 ${formatDisplayWon(it)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium) }
            }
            if (pace.previousSamePeriodExpense > 0 && isCurrentBudgetCycle) Text("지난 예산 기간 같은 시점 ${formatDisplayWon(pace.previousSamePeriodExpense)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            if (uiState.previousBudgetPeriodExpenseTotal > 0L) {
                Text(
                    text = if (change >= 0) "지난 예산 기간보다 ${formatDisplayWon(change)} 더 썼어요" else "지난 예산 기간보다 ${formatDisplayWon(-change)} 아꼈어요",
                    color = if (change > 0) colors.expense else colors.success,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (uiState.budgetPeriodIncomeTotal > 0) {
                Text("기간 수입 ${formatDisplayWon(uiState.budgetPeriodIncomeTotal)}", color = colors.income, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TodayAndWeekCard(uiState: LedgerUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniSpendCard(
            label = "오늘 지출",
            amount = uiState.todayExpenseTotal,
            count = uiState.todayTransactionCount,
            modifier = Modifier.weight(1f),
        )
        MiniSpendCard(
            label = "이번 주 지출",
            amount = uiState.weekExpenseTotal,
            count = uiState.weekTransactionCount,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TodayInsightCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val today = java.time.LocalDate.now()
    val todayTransactions = uiState.transactions.filter {
        it.type == com.moasseum.app.domain.TransactionType.EXPENSE && it.occurredDate == today
    }
    val todayTotal = todayTransactions.sumOf { it.amount }
    val yesterdayTotal = uiState.transactions.asSequence()
        .filter { it.type == com.moasseum.app.domain.TransactionType.EXPENSE && it.occurredDate == today.minusDays(1) }
        .sumOf { it.amount }
    val headline = when {
        todayTotal == 0L -> "오늘 소비가 없어요"
        yesterdayTotal > todayTotal -> "어제보다 ${formatDisplayWon(yesterdayTotal - todayTotal)} 줄었어요"
        yesterdayTotal in 1 until todayTotal -> "어제보다 ${formatDisplayWon(todayTotal - yesterdayTotal)} 더 썼어요"
        else -> "오늘 ${formatDisplayWon(todayTotal)} 사용했어요"
    }
    val supportingText = when {
        todayTotal == 0L && yesterdayTotal == 0L -> "좋은 하루 보내세요"
        todayTotal == 0L -> "어제는 ${formatDisplayWon(yesterdayTotal)} 사용했어요"
        else -> "오늘 기록 ${todayTransactions.size}건"
    }
    FinanceCard(highlighted = true) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(headline, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(supportingText, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(
    uiState: LedgerUiState,
    onOpenHistory: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest = uiState.monthTransactions
        .filter { it.type != com.moasseum.app.domain.TransactionType.TRANSFER }
        .sortedByDescending { it.occurredAt }
        .take(3)
    val colors = LocalFinanceColors.current
    Column(modifier = modifier.testTag("home_recent_transactions_card"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("최근 내역", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Surface(onClick = onOpenHistory, color = Color.Transparent, contentColor = colors.accent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("전체 보기", style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "소비내역 전체 보기", modifier = Modifier.size(17.dp))
                }
            }
        }
        FinanceCard {
            Column {
                latest.forEachIndexed { index, transaction ->
                    TransactionRow(transaction, onClick = { onOpenTransaction(transaction.id) })
                    if (index != latest.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = colors.divider.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSpendCard(
    label: String,
    amount: Long,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            FittedAmountText(formatDisplayWon(amount), MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("${count}건", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CategorySpendingCard(
    uiState: LedgerUiState,
    categoryBudgets: Map<String, Long>,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    Column(modifier = modifier.padding(horizontal = 4.dp).testTag("home_category_spending_card"), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("${formatMonth(uiState.month)} 카테고리 TOP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("달력 월 기준", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Surface(onClick = onOpenHistory, color = Color.Transparent, contentColor = colors.accent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("전체", style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "소비내역 전체 보기", modifier = Modifier.size(17.dp))
                }
            }
        }
        if (uiState.categoryTotals.isEmpty()) {
            Text("기록된 지출 없음", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            val maxValue = uiState.categoryTotals.maxOf { it.total }.coerceAtLeast(1L)
            uiState.categoryTotals.take(2).forEach { total ->
                CategoryBar(total.key, total.total, maxValue, uiState.expenseTotal)
                categoryBudgets[total.key]?.let { budget ->
                    val used = (total.total * 100L / budget.coerceAtLeast(1L)).coerceAtMost(999L)
                    Text(
                        "${categoryLabel(total.key)} 월 한도 ${formatDisplayWon(budget)} 중 ${used}% 사용",
                        color = if (used > 90L) colors.expense else colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(key: String, total: Long, maxValue: Long, allTotal: Long) {
    val colors = LocalFinanceColors.current
    val tint = categoryColor(key)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Text(categoryLabel(key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatDisplayWon(total), color = colors.textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((total.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(tint, RoundedCornerShape(8.dp)),
            )
        }
        Text(
            text = "${if (allTotal == 0L) 0 else (total * 100 / allTotal)}%",
            color = colors.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(34.dp),
        )
    }
    }
}

@Composable
private fun InsightsContent(
    uiState: LedgerUiState,
    noSpendChallenge: NoSpendChallengeSettings,
    onOpenManageFeature: (String) -> Unit,
    activeFixedCostCount: Int,
    activeFixedCostMonthlyAmount: Long,
) {
    val colors = LocalFinanceColors.current
    val previousExpense = uiState.previousExpenseTotal
    val change = uiState.expenseTotal - previousExpense
    val budgetPeriodSpent = uiState.budgetPeriodExpenseTotal
    val pace = com.moasseum.app.domain.budgetPace(uiState)
    val cycle = uiState.budgetCycle
    val today = LocalDate.now()
    val paceMetric = when {
        today.isBefore(cycle.startDate) -> "예산 주기 시작 전" to "시작 전"
        today.isAfter(cycle.endDate) -> "예산 기간 지출" to formatDisplayWon(budgetPeriodSpent)
        else -> "예산 주기 종료 예상" to (pace.projectedExpense?.let { formatDisplayWon(it) } ?: "아직 계산 전")
    }
    val savingsRate = if (uiState.incomeTotal > 0L) {
        (((uiState.incomeTotal - uiState.expenseTotal).toDouble() / uiState.incomeTotal) * 100.0)
            .roundToInt().coerceIn(-999, 999).toString() + "%"
    } else "수입 없음"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceCard(highlighted = true) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Text("예산 주기 요약", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                }
                val message = when {
                    budgetPeriodSpent == 0L -> "예산 기간 지출 없음"
                    uiState.budgetAmount != null && budgetPeriodSpent > uiState.budgetAmount -> "예산 ${formatDisplayWon(budgetPeriodSpent - uiState.budgetAmount)} 초과"
                    else -> "예산 기간 지출 ${formatDisplayWon(budgetPeriodSpent)}"
                }
                Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${cycle.label} 기준", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
        Text("바로 쓰는 도구", color = colors.textSecondary, style = MaterialTheme.typography.labelLarge)
        val challengeStreak = noSpendChallenge.startDate?.let { start -> noSpendStreakDays(uiState.transactions, start) } ?: 0
        InsightsShortcutCard(
            tag = "insight_no_spend_shortcut",
            icon = Icons.Rounded.AutoAwesome,
            title = "무지출 챌린지",
            subtitle = if (noSpendChallenge.enabled) {
                "현재 ${challengeStreak.coerceAtMost(noSpendChallenge.goalDays)} / ${noSpendChallenge.goalDays}일 기록 · 목표 관리"
            } else "기간을 정하고 무지출 기록 시작",
            onClick = { onOpenManageFeature("no-spend") },
        )
        InsightsShortcutCard(
            tag = "insight_fixed_costs_shortcut",
            icon = Icons.Rounded.Repeat,
            title = "구독·고정비 레이더",
            subtitle = if (activeFixedCostCount == 0) "반복 결제와 월 예정액 확인" else "현재 ${activeFixedCostCount}개 활성 · 월 ${formatDisplayWon(activeFixedCostMonthlyAmount)}",
            onClick = { onOpenManageFeature("fixed-costs") },
        )
        InsightsShortcutCard(
            tag = "insight_year_end_shortcut",
            icon = Icons.Rounded.CalendarMonth,
            title = "연말정산 도우미",
            subtitle = "연간 카드 사용 기록과 25% 기준 확인",
            onClick = { onOpenManageFeature("year-end") },
        )
        FinanceCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("${formatMonth(uiState.month)} 체크 포인트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                InsightRow("기록한 거래", "${uiState.monthTransactions.size}건")
                InsightRow("달력 월 최다 지출", uiState.categoryTotals.firstOrNull()?.let { categoryLabel(it.key) } ?: "아직 없음")
                InsightRow("예산 기간 사용률", uiState.budgetAmount?.let { "${(budgetPeriodSpent * 100 / it.coerceAtLeast(1)).coerceAtMost(999)}%" } ?: "예산 없음")
                InsightRow(
                    "지난달 대비",
                    if (previousExpense == 0L) "비교 기록 없음" else "${if (change > 0L) "+" else "−"}${formatDisplayWon(kotlin.math.abs(change))}",
                )
                InsightRow(paceMetric.first, paceMetric.second)
                InsightRow("달력 월 저축률", savingsRate)
            }
        }
    }
}

@Composable
private fun InsightsShortcutCard(
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "열기", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    val colors = LocalFinanceColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
    }
}

@Composable
private fun ReportContent(
    uiState: LedgerUiState,
    onSelectMonth: (YearMonth) -> Unit,
    onOpenManageFeature: (String) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    aiState: SpendingAnalysisState,
    onGenerateAiReport: () -> Unit,
    showAiReport: Boolean,
) {
    val colors = LocalFinanceColors.current
    val net = uiState.incomeTotal - uiState.expenseTotal
    val budgetSpent = uiState.budgetPeriodExpenseTotal
    val remainingBudget = uiState.budgetAmount?.minus(budgetSpent)
    val savingsRate = if (uiState.incomeTotal <= 0L) null else
        ((net.toDouble() / uiState.incomeTotal.toDouble()) * 100.0).roundToInt().coerceIn(-999, 999)
    val budget = uiState.budgetAmount?.takeIf { it > 0L }
    val budgetRemaining = budget?.minus(budgetSpent)
    val budgetProgress = if (budget == null) 0f else
        (budgetSpent.toDouble() / budget.toDouble()).toFloat().coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showAiReport) AiReportCard(uiState, aiState, onGenerateAiReport)
        FinanceCard(modifier = Modifier.testTag("report_month_budget_card")) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("월간 리포트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Surface(color = colors.surfaceOverlay, shape = RoundedCornerShape(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.IconButton(
                                onClick = { onSelectMonth(uiState.month.minusMonths(1)) },
                                modifier = Modifier.size(48.dp).testTag("report_previous_month"),
                            ) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "이전 달") }
                            Text(
                                formatMonth(uiState.month),
                                color = colors.textPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.testTag("report_month_label"),
                                maxLines = 1,
                            )
                            androidx.compose.material3.IconButton(
                                onClick = { onSelectMonth(uiState.month.plusMonths(1)) },
                                modifier = Modifier.size(48.dp).testTag("report_next_month"),
                            ) { Icon(Icons.Rounded.ChevronRight, contentDescription = "다음 달") }
                        }
                    }
                }
                if (budget == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "월 예산을 정하면 소비 속도와 남은 금액을 볼 수 있어요.",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { onOpenManageFeature("budget") },
                            modifier = Modifier.testTag("report_set_budget"),
                        ) { Text("예산 설정") }
                    }
                } else {
                    FinanceProgressBar(
                        progress = budgetProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = if ((budgetRemaining ?: 0L) >= 0L) colors.accent else colors.expense,
                        trackColor = colors.surfaceOverlay,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val usedPercent = (budgetSpent.toDouble() / budget.toDouble() * 100.0)
                            .roundToInt().coerceAtLeast(0)
                        Text("예산 주기 ${usedPercent}% 사용", color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        val remaining = budgetRemaining ?: 0L
                        Text(
                            "${formatDisplayWon(kotlin.math.abs(remaining))} ${if (remaining >= 0L) "남음" else "초과"}",
                            color = if (remaining >= 0L) colors.textSecondary else colors.expense,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportMetric("달력 월 지출", formatDisplayWon(uiState.expenseTotal), colors.expense, Modifier.weight(1f))
                    ReportMetric("달력 월 수입", formatDisplayWon(uiState.incomeTotal), colors.income, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportMetric("예산 주기 잔액", remainingBudget?.let { formatDisplayWon(it) } ?: "목표 없음", if ((remainingBudget ?: 0L) >= 0L) colors.income else colors.expense, Modifier.weight(1f))
                    ReportMetric("달력 월 저축률", savingsRate?.let { "$it%" } ?: "수입 기록 없음", colors.accent, Modifier.weight(1f))
                }
            }
        }
        MonthlyTrendCard(uiState)
        CategoryReportCard(uiState)
        WeekdayReportCard(uiState)
        FinanceCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("월간 PDF 리포트 저장")
                }
                androidx.compose.material3.TextButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text("거래 CSV 내보내기")
                }
            }
        }
    }
}

@Composable
private fun AiReportCard(
    uiState: LedgerUiState,
    state: SpendingAnalysisState,
    onGenerate: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(highlighted = true) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Text("AI 리포트", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            when (state) {
                SpendingAnalysisState.Idle -> TextButton(onClick = onGenerate, enabled = uiState.expenseCount > 0) {
                    Text(if (uiState.expenseCount > 0) "이번 달 분석 보기" else "거래를 기록하면 볼 수 있어요")
                }
                SpendingAnalysisState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("리포트를 만들고 있어요", color = colors.textSecondary)
                }
                is SpendingAnalysisState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onGenerate, enabled = uiState.expenseCount > 0) { Text("다시 시도") }
                }
                is SpendingAnalysisState.Success -> {
                    if (state.month != uiState.month) TextButton(onClick = onGenerate, enabled = uiState.expenseCount > 0) {
                        Text("${formatMonth(uiState.month)} 분석 만들기")
                    } else {
                        Text(state.analysis.summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        state.analysis.observations.take(2).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
                        state.analysis.suggestions.take(2).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = colors.accent) }
                        TextButton(onClick = onGenerate, enabled = uiState.expenseCount > 0) { Text("다시 분석") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val summaries = uiState.recentMonthlySummaries(6)
    val maxExpense = summaries.maxOfOrNull { it.expenseTotal }?.coerceAtLeast(1L) ?: 1L
    val current = summaries.lastOrNull()
    val previous = summaries.getOrNull(summaries.lastIndex - 1)
    val comparison = when {
        previous == null || previous.expenseTotal == 0L -> "지난달 지출 기록 없음"
        current == null -> ""
        current.expenseTotal == previous.expenseTotal -> "지난달과 같아요"
        else -> {
            val difference = current.expenseTotal - previous.expenseTotal
            val percent = ((difference.toDouble() / previous.expenseTotal.toDouble()) * 100.0).roundToInt().coerceIn(-999, 999)
            "지난달보다 ${formatDisplayWon(kotlin.math.abs(difference))} ${if (difference > 0L) "더 썼어요" else "덜 썼어요"} · ${kotlin.math.abs(percent)}%"
        }
    }
    FinanceCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("최근 6개월 지출", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(comparison, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                summaries.forEach { summary ->
                    val fraction = (summary.expenseTotal.toFloat() / maxExpense.toFloat()).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 240),
                        label = "monthly-trend-${summary.month}",
                    )
                    val isSelectedMonth = summary.month == uiState.month
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = summary.expenseTotal.takeIf { it > 0L }?.let(::compactWon) ?: "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelectedMonth) colors.textPrimary else colors.textSecondary,
                            maxLines = 1,
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().height(62.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .fillMaxHeight(if (fraction == 0f) 0.04f else animatedFraction)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(if (isSelectedMonth) colors.accent else colors.accentSoft),
                            )
                        }
                        Text("${summary.month.monthValue}월", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryReportCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val previousTotals = uiState.recentMonthlySummaries(2).firstOrNull()?.categories.orEmpty()
        .associate { it.key to it.total }
    FinanceCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("지출 카테고리", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            uiState.categoryTotals.take(6).forEach { total ->
                val ratio = if (uiState.expenseTotal == 0L) 0f else
                    (total.total.toDouble() / uiState.expenseTotal.toDouble()).toFloat().coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(8.dp).background(categoryColor(total.key), CircleShape))
                    Text(categoryLabel(total.key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(
                        Modifier.weight(1.1f).height(7.dp).background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
                    ) {
                        Box(Modifier.fillMaxWidth(ratio).height(7.dp).background(categoryColor(total.key), RoundedCornerShape(8.dp)))
                    }
                    Text("${(ratio * 100).roundToInt()}%", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
                val previous = previousTotals[total.key] ?: 0L
                val difference = total.total - previous
                Text(
                    text = when {
                        previous == 0L -> "지난달 기록 없음"
                        difference == 0L -> "지난달과 같아요"
                        else -> "지난달보다 ${formatDisplayWon(kotlin.math.abs(difference))} ${if (difference > 0L) "더 썼어요" else "덜 썼어요"}"
                    },
                    color = when {
                        previous == 0L || difference == 0L -> colors.textSecondary
                        difference > 0L -> colors.expense
                        else -> colors.income
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 17.dp),
                )
            }
            if (uiState.categoryTotals.isEmpty()) Text("기록된 지출 없음", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WeekdayReportCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    val totals = uiState.monthTransactions.filter { it.type == com.moasseum.app.domain.TransactionType.EXPENSE }
        .groupBy { it.occurredDate.dayOfWeek }.mapValues { (_, rows) -> rows.sumOf { it.amount } }
    val maxExpense = totals.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val labels = mapOf(
        DayOfWeek.MONDAY to "월", DayOfWeek.TUESDAY to "화", DayOfWeek.WEDNESDAY to "수",
        DayOfWeek.THURSDAY to "목", DayOfWeek.FRIDAY to "금", DayOfWeek.SATURDAY to "토", DayOfWeek.SUNDAY to "일",
    )
    FinanceCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("요일별 지출", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                days.forEach { day ->
                    val amount = totals[day] ?: 0L
                    val target = (amount.toDouble() / maxExpense.toDouble()).toFloat().coerceIn(0f, 1f)
                    val animated by animateFloatAsState(
                        targetValue = target,
                        animationSpec = tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 240),
                        label = "weekday-${day.value}",
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(amount.takeIf { it > 0L }?.let(::compactWon) ?: "", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1)
                        Box(
                            modifier = Modifier.fillMaxWidth(0.52f).height(54.dp)
                                .background(colors.surfaceOverlay, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (amount > 0L) Box(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(animated.coerceAtLeast(0.06f))
                                    .background(if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) colors.accentSoft else colors.accent, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                            )
                        }
                        Text(labels.getValue(day), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

private fun compactWon(amount: Long): String = when {
    amount >= 100_000_000L -> "${amount / 100_000_000L}억"
    amount >= 10_000L -> "${amount / 10_000L}만"
    else -> "${amount}원"
}

@Composable
private fun ReportMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    val colors = LocalFinanceColors.current
    Surface(modifier = modifier, color = colors.surfaceOverlay, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            FittedAmountText(value, MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), tint)
        }
    }
}

@Composable
private fun AiAnalysisContent(
    uiState: LedgerUiState,
    state: SpendingAnalysisState,
    onGenerate: () -> Unit,
    questionState: SpendingQuestionState,
    onAskQuestion: (String) -> Unit,
    showSummary: Boolean,
    showReport: Boolean,
) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanceCard(highlighted = true) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
                Text("${com.moasseum.app.domain.formatMonth(uiState.month)} AI 소비 분석", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "거래처·메모 없이 최근 4개월 집계만 전송해요.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
                RecentSpendingTrend(uiState)
                when (state) {
                    SpendingAnalysisState.Idle -> {
                        if (uiState.expenseCount == 0) Text("이 달 거래를 기록하면 분석할 수 있어요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        else AnalysisButton(onGenerate, enabled = true, label = "AI 분석 만들기")
                    }
                    SpendingAnalysisState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("분석 중…", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    is SpendingAnalysisState.Error -> {
                        Text(state.message, color = colors.expense, style = MaterialTheme.typography.bodyMedium)
                        AnalysisButton(onGenerate, enabled = uiState.expenseCount > 0, label = "다시 시도")
                    }
                    is SpendingAnalysisState.Success -> {
                        if (state.month != uiState.month) {
                            Text("분석 기준 월이 바뀌었어요. 새로 분석해 주세요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            AnalysisButton(onGenerate, enabled = uiState.expenseCount > 0, label = "이번 달 다시 분석")
                        } else {
                            if (showSummary) Text(state.analysis.summary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            if (showReport && state.analysis.observations.isNotEmpty()) {
                                Text("살펴볼 점", color = colors.accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                state.analysis.observations.forEach { Text("• $it", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium) }
                            }
                            if (showReport && state.analysis.suggestions.isNotEmpty()) {
                                Text("작은 제안", color = colors.accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                state.analysis.suggestions.forEach { Text("• $it", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium) }
                            }
                            AnalysisButton(onGenerate, enabled = uiState.expenseCount > 0, label = "다시 분석")
                        }
                    }
                }
            }
        }
        SpendingQuestionCard(
            uiState = uiState,
            state = questionState,
            onAsk = onAskQuestion,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendingQuestionCard(
    uiState: LedgerUiState,
    state: SpendingQuestionState,
    onAsk: (String) -> Unit,
) {
    val colors = LocalFinanceColors.current
    var question by rememberSaveable { mutableStateOf("") }
    val quickQuestions = listOf("예산 얼마나 남았어?", "지난달보다 많이 썼어?", "가장 많이 쓴 항목은?")
    FinanceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                Text("내 소비에 물어보기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("최근 4개월의 합계와 카테고리만 사용해 답해요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                quickQuestions.forEach { suggested ->
                    AssistChip(
                        onClick = {
                            question = suggested
                            if (uiState.monthTransactions.isNotEmpty()) onAsk(suggested)
                        },
                        enabled = uiState.monthTransactions.isNotEmpty() && state !is SpendingQuestionState.Loading,
                        label = { Text(suggested, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            OutlinedTextField(
                value = question,
                onValueChange = { question = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예: 이번 달 예산이 얼마나 남았어?") },
                maxLines = 2,
            )
            if (uiState.monthTransactions.isEmpty()) {
                Text("거래를 기록하면 소비에 대해 질문할 수 있어요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            when (state) {
                SpendingQuestionState.Idle -> Unit
                SpendingQuestionState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("답변 중…", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                is SpendingQuestionState.Error -> Text(state.message, color = colors.expense, style = MaterialTheme.typography.bodyMedium)
                is SpendingQuestionState.Success -> {
                    if (state.month != uiState.month) {
                        Text("기준 월이 바뀌었어요. 현재 월로 다시 질문해 주세요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Q. ${state.question}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        Text(state.answer, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            AnalysisButton(
                onClick = { onAsk(question.trim()) },
                enabled = question.trim().isNotBlank() && uiState.monthTransactions.isNotEmpty() && state !is SpendingQuestionState.Loading,
                label = "질문하기",
            )
        }
    }
}

@Composable
private fun RecentSpendingTrend(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val months = uiState.recentMonthlySummaries(monthCount = 4)
    if (months.none { it.expenseCount > 0 }) return
    val maxExpense = months.maxOf { it.expenseTotal }.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("최근 4개월 지출", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        months.forEach { summary ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${summary.month.monthValue}월", modifier = Modifier.width(28.dp), color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium)
                val progress by animateFloatAsState(
                    targetValue = (summary.expenseTotal.toFloat() / maxExpense.toFloat()).coerceIn(0f, 1f),
                    animationSpec = tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 240),
                    label = "monthlyTrendProgress-${summary.month}",
                )
                Box(
                    modifier = Modifier.weight(1f).height(6.dp)
                        .clip(RoundedCornerShape(50)).background(colors.surfaceOverlay),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                            .background(if (summary.month == uiState.month) colors.accent else colors.accent.copy(alpha = 0.56f)),
                    )
                }
                Text(
                    text = formatDisplayWon(summary.expenseTotal),
                    modifier = Modifier.width(82.dp),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun AnalysisButton(onClick: () -> Unit, enabled: Boolean, label: String) {
    val colors = LocalFinanceColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
        shape = RoundedCornerShape(14.dp),
    ) { Text(label, fontWeight = FontWeight.Bold) }
}
