package com.moasseum.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moasseum.app.ui.components.AddMode
import com.moasseum.app.ui.components.AddTransactionSheet
import com.moasseum.app.ui.components.LocalCategoryLabels
import com.moasseum.app.ui.components.LocalCategoryOrder
import com.moasseum.app.ui.components.BottomNavBar
import com.moasseum.app.ui.components.ROUTE_HISTORY
import com.moasseum.app.ui.components.ROUTE_HOME
import com.moasseum.app.ui.components.ROUTE_MANAGE
import com.moasseum.app.ui.components.ROUTE_NOTIFICATIONS
import com.moasseum.app.ui.components.ROUTE_TOGETHER
import com.moasseum.app.ui.components.ROUTE_LEGACY_TOGETHER
import com.moasseum.app.ui.screens.HistoryScreen
import com.moasseum.app.ui.screens.HomeScreen
import com.moasseum.app.ui.screens.FirstRunGuide
import com.moasseum.app.ui.screens.ManageScreen
import com.moasseum.app.ui.screens.NotificationsScreen
import com.moasseum.app.ui.screens.TogetherScreen
import com.moasseum.app.ui.screens.QuickHelpDialog
import com.moasseum.app.ui.screens.ReleaseNotesCatalog
import com.moasseum.app.ui.screens.WhatsNewDialog
import com.moasseum.app.data.SharedLedgerSnapshot
import com.moasseum.app.ui.theme.MoasseumTheme
import com.moasseum.app.data.AiClient
import com.moasseum.app.data.CsvBackup
import com.moasseum.app.data.DEFAULT_CATEGORY_LABELS
import com.moasseum.app.data.DEFAULT_CATEGORY_ORDER
import com.moasseum.app.data.DEFAULT_PAYMENT_METHODS
import com.moasseum.app.data.JsonBackup
import com.moasseum.app.data.ReceiptOcr
import com.moasseum.app.data.PhotoImportState
import com.moasseum.app.data.PhotoTransactionImport
import com.moasseum.app.domain.AiParseState
import com.moasseum.app.domain.buildActivityNotices
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.NoSpendChallengeSettings
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.NetworkReconnectGate
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.domain.parseAmount
import com.moasseum.app.notification.NotificationAccess
import com.moasseum.app.notification.EXTRA_NOTIFICATION_CANDIDATE_ID
import com.moasseum.app.notification.PaymentNotificationNotifier
import com.moasseum.app.update.AppUpdateManager
import com.moasseum.app.update.UpdateCheckState
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.SnackbarResult

class MainActivity : FragmentActivity() {
    private val incomingNotificationCandidateId = MutableStateFlow<Long?>(null)
    private val incomingSharedContent = MutableStateFlow<IncomingShare?>(null)
    private var sharedContentSequence = 0L
    private val biometricLockReleased = MutableStateFlow(false)

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            val app = application as? FinanceApplication ?: return@launch
            if (app.preferencesRepository.appLockEnabled.first()) biometricLockReleased.value = false
        }
    }

    fun authenticateAppLock() {
        showBiometricPrompt { success -> if (success) biometricLockReleased.value = true }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (!enabled) {
            lifecycleScope.launch {
                (application as FinanceApplication).preferencesRepository.setAppLockEnabled(false)
                biometricLockReleased.value = true
            }
            return
        }
        val app = application as FinanceApplication
        val authenticators = appLockAuthenticators()
        val availability = BiometricManager.from(this).canAuthenticate(authenticators)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "기기 설정에서 생체 인증 또는 화면 잠금을 먼저 설정해 주세요.", Toast.LENGTH_LONG).show()
            return
        }
        showBiometricPrompt { success ->
            if (success) lifecycleScope.launch {
                app.preferencesRepository.setAppLockEnabled(true)
                biometricLockReleased.value = true
            }
        }
    }

    private fun showBiometricPrompt(onResult: (Boolean) -> Unit) {
        val title = "모아씀 잠금 해제"
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("생체 인증 또는 기기 잠금으로 확인해 주세요.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(appLockAuthenticators())
        } else {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("취소")
        }
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
            },
        ).authenticate(builder.build())
    }

    private fun appLockAuthenticators(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else BiometricManager.Authenticators.BIOMETRIC_STRONG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applySystemBars()
        incomingNotificationCandidateId.value = intentCandidateId(intent)
        incomingSharedContent.value = extractShare(intent)
        val application = application as FinanceApplication
        consumeAuthIntent(intent)
        setContent {
            val viewModel: LedgerViewModel = viewModel(
                factory = LedgerViewModel.Factory(application.financeRepository),
            )
            val darkTheme by application.preferencesRepository.isDarkTheme.collectAsStateWithLifecycle(initialValue = true)
            val reduceMotion by application.preferencesRepository.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
            val categoryLabels by application.preferencesRepository.categoryLabels.collectAsStateWithLifecycle(initialValue = DEFAULT_CATEGORY_LABELS)
            val categoryOrder by application.preferencesRepository.categoryOrder.collectAsStateWithLifecycle(initialValue = DEFAULT_CATEGORY_ORDER)
            val paymentMethods by application.preferencesRepository.paymentMethods.collectAsStateWithLifecycle(initialValue = DEFAULT_PAYMENT_METHODS)
            val paymentCards by application.preferencesRepository.paymentCards.collectAsStateWithLifecycle(initialValue = emptyList())
            val categoryBudgets by application.preferencesRepository.categoryBudgets.collectAsStateWithLifecycle(initialValue = emptyMap())
            val monthlyIncomeTargets by application.preferencesRepository.monthlyIncomeTargets.collectAsStateWithLifecycle(initialValue = emptyMap())
            val homeDashboardCards by application.preferencesRepository.homeDashboardCards.collectAsStateWithLifecycle(initialValue = HomeDashboardCards.defaults)
            val noSpendChallenge by application.preferencesRepository.noSpendChallenge.collectAsStateWithLifecycle(initialValue = NoSpendChallengeSettings())
            val profileDisplayName by application.preferencesRepository.profileDisplayName.collectAsStateWithLifecycle(initialValue = "")
            val readActivityNoticeIds by application.preferencesRepository.readActivityNoticeIds.collectAsStateWithLifecycle(initialValue = emptySet())
            val postNotificationPermissionPromptShown by application.preferencesRepository.notificationPostPermissionPromptShown.collectAsStateWithLifecycle(initialValue = false)
            val firstRunGuideCompleted by application.preferencesRepository.firstRunGuideCompleted.collectAsStateWithLifecycle(initialValue = true)
            val aiNotificationClassificationEnabled by application.preferencesRepository.aiNotificationClassificationEnabled.collectAsStateWithLifecycle(initialValue = false)
            val notificationServiceConnectedAt by application.preferencesRepository.notificationServiceConnectedAt.collectAsStateWithLifecycle(initialValue = null)
            val notificationServiceDisconnectedAt by application.preferencesRepository.notificationServiceDisconnectedAt.collectAsStateWithLifecycle(initialValue = null)
            val notificationLastSeenAt by application.preferencesRepository.notificationLastSeenAt.collectAsStateWithLifecycle(initialValue = null)
            val notificationLastCandidateAt by application.preferencesRepository.notificationLastCandidateAt.collectAsStateWithLifecycle(initialValue = null)
            val candidateIdFromNotification by incomingNotificationCandidateId.collectAsStateWithLifecycle()
            val sharedContent by incomingSharedContent.collectAsStateWithLifecycle()
            val appLockEnabled by application.preferencesRepository.appLockEnabled.collectAsStateWithLifecycle(initialValue = false)
            val appLockReleased by biometricLockReleased.collectAsStateWithLifecycle()
            val financeRemindersEnabled by application.preferencesRepository.financeRemindersEnabled.collectAsStateWithLifecycle(initialValue = true)
            CompositionLocalProvider(
                LocalCategoryLabels provides categoryLabels,
                LocalCategoryOrder provides categoryOrder,
            ) {
                MoasseumTheme(darkTheme = darkTheme, reduceMotion = reduceMotion) {
                    UpdateSystemBars(darkTheme)
                    MoasseumApp(
                        viewModel = viewModel,
                        application = application,
                        paymentMethods = (paymentMethods + paymentCards.map { it.paymentMethod }).distinct(),
                        onSavePaymentMethods = { methods ->
                            application.preferencesRepository.savePaymentMethods(methods)
                        },
                        paymentCards = paymentCards,
                        onSavePaymentCards = { cards ->
                            application.preferencesRepository.savePaymentCards(cards)
                        },
                        categoryBudgets = categoryBudgets,
                        onSaveCategoryBudgets = { budgets ->
                            application.preferencesRepository.saveCategoryBudgets(budgets)
                        },
                        onSaveCategoryLabels = { labels ->
                            application.preferencesRepository.saveCategoryLabels(labels)
                        },
                        categoryOrder = categoryOrder,
                        onSaveCategoryOrder = { order -> application.preferencesRepository.saveCategoryOrder(order) },
                        monthlyIncomeTargets = monthlyIncomeTargets,
                        onSaveMonthlyIncomeTarget = { monthKey, amount -> application.preferencesRepository.saveMonthlyIncomeTarget(monthKey, amount) },
                        homeDashboardCards = homeDashboardCards,
                        onSaveHomeDashboardCards = { cards -> application.preferencesRepository.saveHomeDashboardCards(cards) },
                        noSpendChallenge = noSpendChallenge,
                        profileDisplayName = profileDisplayName,
                        readActivityNoticeIds = readActivityNoticeIds,
                        onSaveDisplayName = { name ->
                            viewModel.performOperation("프로필 이름을 저장하지 못했어요.") { application.preferencesRepository.saveProfileDisplayName(name) }
                        },
                        onMarkActivityNoticesRead = { ids ->
                            viewModel.performOperation("알림 상태를 저장하지 못했어요.") { application.preferencesRepository.markActivityNoticesRead(ids) }
                        },
                        onSaveNoSpendChallenge = { enabled, goalDays -> application.preferencesRepository.configureNoSpendChallenge(enabled, goalDays) },
                        onSetAiNotificationClassificationEnabled = { enabled ->
                            viewModel.performOperation("AI 알림 설정을 저장하지 못했어요.") { application.preferencesRepository.setAiNotificationClassificationEnabled(enabled) }
                        },
                        notificationServiceConnectedAt = notificationServiceConnectedAt,
                        notificationServiceDisconnectedAt = notificationServiceDisconnectedAt,
                        notificationLastSeenAt = notificationLastSeenAt,
                        notificationLastCandidateAt = notificationLastCandidateAt,
                        darkTheme = darkTheme,
                        reduceMotion = reduceMotion,
                        onDarkThemeChanged = { enabled ->
                            viewModel.performOperation("화면 설정을 저장하지 못했어요.") { application.preferencesRepository.setDarkTheme(enabled) }
                        },
                        onReduceMotionChanged = { enabled ->
                            viewModel.performOperation("모션 설정을 저장하지 못했어요.") { application.preferencesRepository.setReduceMotion(enabled) }
                        },
                        notificationPostPermissionPromptShown = postNotificationPermissionPromptShown,
                        firstRunGuideCompleted = firstRunGuideCompleted,
                        onMarkFirstRunGuideCompleted = {
                            viewModel.performOperation("기능 안내를 저장하지 못했어요.") { application.preferencesRepository.setFirstRunGuideCompleted() }
                        },
                        onMarkNotificationPostPermissionPromptShown = {
                            viewModel.performOperation("알림 안내 설정을 저장하지 못했어요.") { application.preferencesRepository.setNotificationPostPermissionPromptShown() }
                        },
                        aiNotificationClassificationEnabled = aiNotificationClassificationEnabled,
                        incomingNotificationCandidateId = candidateIdFromNotification,
                        onIncomingNotificationCandidateConsumed = { id ->
                            if (incomingNotificationCandidateId.value == id) incomingNotificationCandidateId.value = null
                        },
                        incomingSharedContent = sharedContent,
                        onSharedContentConsumed = { token ->
                            if (incomingSharedContent.value?.token == token) incomingSharedContent.value = null
                        },
                        appLockEnabled = appLockEnabled,
                        appLockReleased = appLockReleased,
                        onSetAppLockEnabled = ::setAppLockEnabled,
                        onAuthenticateAppLock = ::authenticateAppLock,
                        financeRemindersEnabled = financeRemindersEnabled,
                        onSetFinanceRemindersEnabled = { enabled ->
                            viewModel.performOperation("알림 설정을 저장하지 못했어요.") {
                                application.preferencesRepository.setFinanceRemindersEnabled(enabled)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applySystemBars()
    }

    private fun applySystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingNotificationCandidateId.value = intentCandidateId(intent)
        incomingSharedContent.value = extractShare(intent)
        consumeAuthIntent(intent)
    }

    private fun consumeAuthIntent(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_VIEW) return
        val callback = incoming.data?.toString() ?: return
        // Do not retain a one-time auth code in the Activity intent after dispatch.
        incoming.data = null
        lifecycleScope.launch {
            val repository = (application as FinanceApplication).authRepository
            repository.initialize()
            repository.handleAuthCallback(callback)
        }
    }

    private fun intentCandidateId(intent: Intent?): Long? =
        intent?.getLongExtra(EXTRA_NOTIFICATION_CANDIDATE_ID, 0L)?.takeIf { it > 0L }

    private fun extractShare(incoming: Intent?): IncomingShare? {
        if (incoming?.action != Intent.ACTION_SEND) return null
        val text = runCatching { incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()?.take(4000)?.takeIf(String::isNotBlank) }
            .getOrNull()
        @Suppress("DEPRECATION")
        val stream = runCatching {
            if (Build.VERSION.SDK_INT >= 33) incoming.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            else incoming.getParcelableExtra(Intent.EXTRA_STREAM)
        }.getOrNull()
        val clipUri = runCatching { incoming.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri }.getOrNull()
        val uri = (stream ?: clipUri)
            ?.takeIf { incoming.type?.startsWith("image/") == true }
        if (text == null && uri == null) return null
        sharedContentSequence++
        return IncomingShare(sharedContentSequence, text, uri)
    }
}

private data class IncomingShare(val token: Long, val text: String?, val imageUri: android.net.Uri?)

@Composable
private fun UpdateSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    LaunchedEffect(darkTheme, view) {
        val activity = view.context as? Activity ?: return@LaunchedEffect
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightNavigationBars = !darkTheme
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MoasseumApp(
    viewModel: LedgerViewModel,
    application: FinanceApplication,
    paymentMethods: List<String>,
    onSavePaymentMethods: suspend (List<String>) -> Unit,
    paymentCards: List<com.moasseum.app.domain.PaymentCard>,
    onSavePaymentCards: suspend (List<com.moasseum.app.domain.PaymentCard>) -> Unit,
    categoryBudgets: Map<String, Long>,
    onSaveCategoryBudgets: suspend (Map<String, Long>) -> Unit,
    onSaveCategoryLabels: suspend (Map<String, String>) -> Unit,
    categoryOrder: List<String>,
    onSaveCategoryOrder: suspend (List<String>) -> Unit,
    monthlyIncomeTargets: Map<String, Long>,
    onSaveMonthlyIncomeTarget: suspend (String, Long?) -> Unit,
    homeDashboardCards: Set<String>,
    onSaveHomeDashboardCards: suspend (Set<String>) -> Unit,
    noSpendChallenge: NoSpendChallengeSettings,
    profileDisplayName: String,
    readActivityNoticeIds: Set<String>,
    onSaveDisplayName: (String) -> Unit,
    onMarkActivityNoticesRead: (Set<String>) -> Unit,
    onSaveNoSpendChallenge: suspend (Boolean, Int) -> Unit,
    onSetAiNotificationClassificationEnabled: (Boolean) -> Unit,
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    notificationPostPermissionPromptShown: Boolean,
    onMarkNotificationPostPermissionPromptShown: () -> Unit,
    firstRunGuideCompleted: Boolean,
    onMarkFirstRunGuideCompleted: () -> Unit,
    aiNotificationClassificationEnabled: Boolean,
    notificationServiceConnectedAt: Long?,
    notificationServiceDisconnectedAt: Long?,
    notificationLastSeenAt: Long?,
    notificationLastCandidateAt: Long?,
    incomingNotificationCandidateId: Long?,
    onIncomingNotificationCandidateConsumed: (Long) -> Unit,
    incomingSharedContent: IncomingShare?,
    onSharedContentConsumed: (Long) -> Unit,
    appLockEnabled: Boolean,
    appLockReleased: Boolean,
    onSetAppLockEnabled: (Boolean) -> Unit,
    onAuthenticateAppLock: () -> Unit,
    financeRemindersEnabled: Boolean,
    onSetFinanceRemindersEnabled: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    var homeScrollToTopRequest by rememberSaveable { mutableIntStateOf(0) }
    var historyScrollToTopRequest by rememberSaveable { mutableIntStateOf(0) }
    var togetherScrollToTopRequest by rememberSaveable { mutableIntStateOf(0) }
    var manageScrollToTopRequest by rememberSaveable { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.date.collectAsStateWithLifecycle()
    val pendingCandidates by viewModel.pendingNotificationCandidates.collectAsStateWithLifecycle()
    val recurringRules by viewModel.recurringRules.collectAsStateWithLifecycle()
    val activityNotices = remember(uiState, noSpendChallenge) { buildActivityNotices(uiState, noSpendChallenge) }
    val unreadActivityNoticeCount = activityNotices.count { it.id !in readActivityNoticeIds }
    val context = LocalContext.current
    var showWhatsNew by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(application, BuildConfig.VERSION_NAME) {
        @Suppress("DEPRECATION")
        val installedPackage = context.packageManager.getPackageInfo(context.packageName, 0)
        val upgraded = installedPackage.lastUpdateTime > installedPackage.firstInstallTime
        val shouldShow = application.preferencesRepository.shouldShowReleaseNotes(BuildConfig.VERSION_NAME, upgraded)
        if (shouldShow && ReleaseNotesCatalog.slides(BuildConfig.VERSION_NAME).isNotEmpty()) {
            showWhatsNew = true
        } else {
            application.preferencesRepository.markReleaseNotesSeen(BuildConfig.VERSION_NAME)
        }
    }
    LaunchedEffect(appLockEnabled, appLockReleased) {
        if (appLockEnabled && !appLockReleased) onAuthenticateAppLock()
    }
    var notificationAccessEnabled by remember { mutableStateOf(NotificationAccess.isEnabled(context)) }
    var appNotificationsEnabled by remember { mutableStateOf(NotificationAccess.areAppNotificationsEnabled(context)) }
    var showNotificationAccessPrompt by remember { mutableStateOf(false) }
    var notificationSetupDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var notificationSettingsInProgress by rememberSaveable { mutableStateOf(false) }
    var notificationPromptIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var postNotificationPermissionRequestStarted by rememberSaveable { mutableStateOf(false) }
    var guideDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var guideOpenedManually by rememberSaveable { mutableStateOf(false) }
    var showQuickHelp by rememberSaveable { mutableStateOf(false) }
    val showFirstRunGuide = guideOpenedManually || (!firstRunGuideCompleted && !guideDismissedThisSession)
    fun closeFirstRunGuide() {
        guideOpenedManually = false
        guideDismissedThisSession = true
        onMarkFirstRunGuideCompleted()
    }
    val postNotificationPermissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    val appNotificationsReady = appNotificationsEnabled && !postNotificationPermissionMissing
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessEnabled = NotificationAccess.isEnabled(context)
                appNotificationsEnabled = NotificationAccess.areAppNotificationsEnabled(context)
                notificationSettingsInProgress = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(notificationAccessEnabled, notificationSettingsInProgress) {
        if (notificationAccessEnabled && !notificationSettingsInProgress) {
            NotificationAccess.requestRebindWithRetry(context)
        }
    }
    LaunchedEffect(notificationAccessEnabled, appNotificationsReady, notificationSetupDismissedThisSession, showFirstRunGuide) {
        val needsNotificationSetup = !notificationAccessEnabled || !appNotificationsReady
        showNotificationAccessPrompt = needsNotificationSetup && !notificationSetupDismissedThisSession && !showFirstRunGuide
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        appNotificationsEnabled = NotificationAccess.areAppNotificationsEnabled(context)
    }
    LaunchedEffect(
        notificationAccessEnabled,
        postNotificationPermissionMissing,
        notificationPostPermissionPromptShown,
        notificationSetupDismissedThisSession,
        notificationSettingsInProgress,
        showNotificationAccessPrompt,
        showFirstRunGuide,
    ) {
        if (notificationAccessEnabled && postNotificationPermissionMissing && !notificationPostPermissionPromptShown &&
            !postNotificationPermissionRequestStarted && notificationSetupDismissedThisSession &&
            !notificationSettingsInProgress && !showNotificationAccessPrompt && !showFirstRunGuide
        ) {
            postNotificationPermissionRequestStarted = true
            onMarkNotificationPostPermissionPromptShown()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(application, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            application.notificationCandidateEvents.collect { candidate ->
                if (candidate.id !in notificationPromptIds) notificationPromptIds = notificationPromptIds + candidate.id
            }
        }
    }
    LaunchedEffect(incomingNotificationCandidateId, pendingCandidates) {
        val id = incomingNotificationCandidateId ?: return@LaunchedEffect
        val candidate = pendingCandidates.firstOrNull { it.id == id } ?: return@LaunchedEffect
        if (id !in notificationPromptIds) notificationPromptIds = notificationPromptIds + id
        onIncomingNotificationCandidateConsumed(id)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.operationErrors.collect { snackbarHostState.showSnackbar(it) }
        }
    }
    val analysisRequests = remember { com.moasseum.app.domain.LatestRequestGate() }
    val questionRequests = remember { com.moasseum.app.domain.LatestRequestGate() }
    val sharedAnalysisRequests = remember { com.moasseum.app.domain.LatestRequestGate() }
    val sharedQuestionRequests = remember { com.moasseum.app.domain.LatestRequestGate() }
    val notificationSaving = com.moasseum.app.ui.components.rememberSaveActionState()
    val coroutineScope = rememberCoroutineScope()
    val aiClient = remember { application.aiClient }
    val accounts by application.financeRepository.observeAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    var showAccounts by rememberSaveable { mutableStateOf(false) }
    var showAuth by rememberSaveable { mutableStateOf(false) }
    val authState by application.authRepository.state.collectAsStateWithLifecycle()
    var sharedSnapshot by remember { mutableStateOf(SharedLedgerSnapshot()) }
    var sharedReportMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var sharedBusy by remember { mutableStateOf(false) }
    var sharedError by remember { mutableStateOf<String?>(null) }
    var sharedAiAnalysisState by remember { mutableStateOf<SpendingAnalysisState>(SpendingAnalysisState.Idle) }
    var sharedAiQuestionState by remember { mutableStateOf<SpendingQuestionState>(SpendingQuestionState.Idle) }
    LaunchedEffect(currentRoute, authState.user?.id) {
        if (authState.user != null && (currentRoute == ROUTE_TOGETHER || sharedSnapshot.ledgerId == null) && !sharedBusy) {
            sharedBusy = true
            sharedError = null
            try {
                sharedSnapshot = application.sharedLedgerRepository.refresh()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                sharedError = error.message ?: "공유 장부를 불러오지 못했어요."
            } finally {
                sharedBusy = false
            }
        }
    }
    LaunchedEffect(authState.linkEvent) {
        if (authState.linkEvent > 0) showAuth = true
    }
    fun runSharedAction(successMessage: String? = null, action: suspend () -> SharedLedgerSnapshot) {
        if (sharedBusy) return
        sharedBusy = true
        sharedError = null
        coroutineScope.launch {
            try {
                sharedSnapshot = action()
                if (successMessage != null) snackbarHostState.showSnackbar(successMessage)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                sharedError = error.message ?: "공유 작업을 완료하지 못했어요."
            } finally {
                sharedBusy = false
            }
        }
    }
    val connectivityManager = remember(context) { context.getSystemService(ConnectivityManager::class.java) }
    val reconnectGate = remember(connectivityManager) {
        NetworkReconnectGate(initiallyAvailable = hasValidatedInternet(connectivityManager))
    }
    var isInternetValidated by remember(connectivityManager) {
        mutableStateOf(hasValidatedInternet(connectivityManager))
    }
    val sharedRefreshOnReconnect = rememberUpdatedState(newValue = {
        if (currentRoute == ROUTE_TOGETHER && authState.user != null && !sharedBusy) {
            runSharedAction { application.sharedLedgerRepository.refresh() }
        }
    })
    DisposableEffect(connectivityManager, context) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val available = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val reconnected = reconnectGate.update(available)
                ContextCompat.getMainExecutor(context).execute {
                    isInternetValidated = available
                    if (reconnected) sharedRefreshOnReconnect.value()
                }
            }

            override fun onLost(network: Network) {
                val available = hasValidatedInternet(connectivityManager)
                reconnectGate.update(available)
                ContextCompat.getMainExecutor(context).execute { isInternetValidated = available }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { runCatching { connectivityManager.unregisterNetworkCallback(callback) } }
    }
    var aiState by remember { mutableStateOf<AiParseState>(AiParseState.Idle) }
    var aiAnalysisState by remember { mutableStateOf<SpendingAnalysisState>(SpendingAnalysisState.Idle) }
    var aiQuestionState by remember { mutableStateOf<SpendingQuestionState>(SpendingQuestionState.Idle) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var pendingHistoryTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var savingTransaction by remember { mutableStateOf(false) }
    var addModeName by rememberSaveable { mutableStateOf(AddMode.MENU.name) }
    var photoImportState by remember { mutableStateOf<PhotoImportState>(PhotoImportState.Idle) }
    var shareNewRecords by rememberSaveable { mutableStateOf(false) }
    var sharedPrefillText by rememberSaveable { mutableStateOf<String?>(null) }
    var sharedPrefillKey by rememberSaveable { mutableStateOf("") }
    var updateState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    var downloadedUpdatePath by rememberSaveable { mutableStateOf<String?>(null) }
    var waitingForInstallPermission by rememberSaveable { mutableStateOf(false) }
    var speechResult by remember { mutableStateOf<String?>(null) }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            speechResult = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
        }
    }
    fun processPhotoUris(uris: List<android.net.Uri>, temporaryFile: File? = null) {
        if (uris.isEmpty()) {
            temporaryFile?.delete()
            return
        }
        val previousReview = photoImportState as? PhotoImportState.Review
        addOpen = true
        addModeName = AddMode.PHOTO_REVIEW.name
        photoImportState = PhotoImportState.Loading(0, uris.size)
        coroutineScope.launch {
            val extracted = mutableListOf<com.moasseum.app.data.PhotoTransactionCandidate>()
            var failedImages = 0
            uris.forEachIndexed { index, uri ->
                val result = try {
                    Result.success(withContext(Dispatchers.IO) {
                        PhotoTransactionImport.extractWithReceiptFallback(ReceiptOcr.recognize(context, uri))
                    })
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Result.failure(error)
                }
                result.fold(
                    onSuccess = { rows ->
                        val imageId = java.util.UUID.randomUUID().toString()
                        extracted += rows.map { it.copy(sourceImageId = imageId) }
                    },
                    onFailure = { failedImages++ },
                )
                photoImportState = PhotoImportState.Loading(index + 1, uris.size)
            }
            val combined = previousReview?.candidates.orEmpty() + extracted
            val preview = PhotoTransactionImport.preview(combined, uiState.transactions + sharedSnapshot.transactions)
            val duplicateCount = (previousReview?.duplicateCount ?: 0) + preview.duplicateCount
            val totalImages = (previousReview?.imageCount ?: 0) + uris.size
            val totalFailures = (previousReview?.failedImageCount ?: 0) + failedImages
            photoImportState = if (preview.candidates.isEmpty() && duplicateCount == 0) {
                PhotoImportState.Error("거래 내역을 찾지 못했어요. 날짜와 금액이 선명하게 보이는 화면을 선택해 주세요.")
            } else {
                PhotoImportState.Review(
                    preview.candidates,
                    duplicateCount,
                    totalImages,
                    totalFailures,
                    previousReview?.unselectedCandidateIds.orEmpty(),
                )
            }
            temporaryFile?.delete()
        }
    }
    val receiptPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) processPhotoUris(uris)
    }
    var receiptCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val receiptCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = receiptCameraPath?.let { java.io.File(it) }
        receiptCameraPath = null
        if (file != null && success) processPhotoUris(listOf(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)), file)
        else file?.delete()
    }
    fun takeReceiptPhoto() {
        addOpen = true
        addModeName = AddMode.PHOTO_REVIEW.name
        runCatching {
            val folder = java.io.File(context.cacheDir, "receipt-capture").apply { mkdirs() }
            val file = java.io.File.createTempFile("receipt-", ".jpg", folder)
            receiptCameraPath = file.absolutePath
            receiptCameraLauncher.launch(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }.onFailure { error ->
            coroutineScope.launch { snackbarHostState.showSnackbar(error.message ?: "카메라를 열 수 없어요.") }
        }
    }
    LaunchedEffect(incomingSharedContent?.token) {
        val incoming = incomingSharedContent ?: return@LaunchedEffect
        navigateTo(navController, ROUTE_HOME)
        if (incoming.imageUri != null) {
            sharedPrefillText = null
            processPhotoUris(listOf(incoming.imageUri))
        } else {
            sharedPrefillText = incoming.text
            sharedPrefillKey = incoming.token.toString()
            aiState = AiParseState.Idle
            addOpen = true
            addModeName = AddMode.AI_INPUT.name
        }
        onSharedContentConsumed(incoming.token)
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val rows = uiState.transactions
                val result = withContext(Dispatchers.IO) { runCatching {
                    val output = context.contentResolver.openOutputStream(uri) ?: error("선택한 위치에 CSV 파일을 쓸 수 없어요.")
                    output.bufferedWriter(Charsets.UTF_8).use { it.write(CsvBackup.encode(rows)) }
                } }
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) "${uiState.transactions.size}건을 CSV로 내보냈어요."
                    else result.exceptionOrNull()?.message ?: "CSV 내보내기에 실패했어요.",
                )
            }
        }
    }
    var yearCsvExport by rememberSaveable { mutableStateOf(java.time.LocalDate.now().year) }
    val exportYearCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) coroutineScope.launch {
            val rows = uiState.transactions.filter { it.occurredDate.year == yearCsvExport }
            val result = withContext(Dispatchers.IO) { runCatching {
                val output = context.contentResolver.openOutputStream(uri) ?: error("선택한 위치에 CSV 파일을 쓸 수 없어요.")
                output.bufferedWriter(Charsets.UTF_8).use { it.write(CsvBackup.encode(rows)) }
            } }
            snackbarHostState.showSnackbar(
                if (result.isSuccess) "${yearCsvExport}년 거래 ${rows.size}건을 내보냈어요."
                else result.exceptionOrNull()?.message ?: "CSV 내보내기에 실패했어요.",
            )
        }
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) { runCatching {
                    val backup = application.backupManager.snapshot()
                    val content = com.moasseum.app.data.FullBackupCodec.encode(backup)
                    require(content.toByteArray(Charsets.UTF_8).size <= com.moasseum.app.data.FullBackupCodec.MAX_BYTES) { "전체 백업 용량 한도는 10MB입니다." }
                    val output = context.contentResolver.openOutputStream(uri) ?: error("백업 파일을 쓸 수 없어요.")
                    output.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
                } }
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) "거래·계좌·예산과 앱 설정을 백업했어요."
                    else result.exceptionOrNull()?.message ?: "JSON 백업에 실패했어요.",
                )
            }
        }
    }
    var backupToRestore by remember { mutableStateOf<com.moasseum.app.data.FullBackup?>(null) }
    var restoringBackup by remember { mutableStateOf(false) }
    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) coroutineScope.launch {
            val parsed = withContext(Dispatchers.IO) { runCatching {
                val input = context.contentResolver.openInputStream(uri) ?: error("백업 파일을 열 수 없어요.")
                com.moasseum.app.data.FullBackupCodec.decode(com.moasseum.app.data.FullBackupCodec.read(input))
            } }
            parsed.onSuccess { backupToRestore = it }.onFailure { snackbarHostState.showSnackbar(it.message ?: "백업이 손상됐어요.") }
        }
    }
    val exportSafetyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) coroutineScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                require(application.backupManager.safetyFile.exists()) { "아직 복원 전 안전 백업이 없어요." }
                val output = context.contentResolver.openOutputStream(uri) ?: error("파일을 쓸 수 없어요.")
                output.use { out -> application.backupManager.safetyFile.inputStream().use { it.copyTo(out) } }
            } }
            snackbarHostState.showSnackbar(if (result.isSuccess) "복원 전 안전 백업을 내보냈어요." else result.exceptionOrNull()?.message ?: "내보내기에 실패했어요.")
        }
    }
    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) coroutineScope.launch {
            val snapshot = uiState
            val result = withContext(Dispatchers.IO) { runCatching {
                val labels = application.preferencesRepository.backupSettings().categoryLabels
                val output = context.contentResolver.openOutputStream(uri) ?: error("PDF를 쓸 수 없어요.")
                output.use { com.moasseum.app.data.MonthlyPdfReport.write(snapshot, labels, it) }
            } }
            snackbarHostState.showSnackbar(if (result.isSuccess) "${snapshot.month} PDF 리포트를 저장했어요." else result.exceptionOrNull()?.message ?: "PDF 저장에 실패했어요.")
        }
    }
    val exportSharedPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) coroutineScope.launch {
            val reportState = com.moasseum.app.domain.LedgerUiState(
                month = sharedReportMonth,
                transactions = sharedSnapshot.transactions,
            )
            val result = withContext(Dispatchers.IO) { runCatching {
                val labels = application.preferencesRepository.backupSettings().categoryLabels
                val output = context.contentResolver.openOutputStream(uri) ?: error("PDF를 쓸 수 없어요.")
                output.use { com.moasseum.app.data.MonthlyPdfReport.write(reportState, labels, it) }
            } }
            snackbarHostState.showSnackbar(
                if (result.isSuccess) "${sharedReportMonth} 공동 리포트를 저장했어요."
                else result.exceptionOrNull()?.message ?: "공동 PDF 리포트 저장에 실패했어요.",
            )
        }
    }
    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val parsed = withContext(Dispatchers.IO) {
                    runCatching {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("선택한 CSV 파일을 열 수 없어요.")
                        val content = com.moasseum.app.data.FullBackupCodec.read(input)
                        require(content.toByteArray(Charsets.UTF_8).size <= 5_000_000) { "CSV 파일은 5MB 이하만 가져올 수 있어요." }
                        CsvBackup.decode(content)
                    }
                }
                parsed.fold(
                    onSuccess = { rows ->
                        viewModel.importTransactions(rows) { result ->
                            coroutineScope.launch {
                                result.fold(
                                    onSuccess = { count -> snackbarHostState.showSnackbar("${count}건을 가져왔어요. 중복 내역은 건너뛰었습니다.") },
                                    onFailure = { error -> snackbarHostState.showSnackbar(error.message ?: "CSV 가져오기에 실패했어요.") },
                                )
                            }
                        }
                    },
                    onFailure = { error -> snackbarHostState.showSnackbar(error.message ?: "CSV 파일을 읽지 못했어요.") },
                )
            }
        }
    }
    val addMode = AddMode.valueOf(addModeName)

    fun startVoiceInput() {
        addOpen = true
        addModeName = AddMode.AI_INPUT.name
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "가계부에 기록할 내용을 말해 주세요")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { speechLauncher.launch(intent) }
            .onFailure { error ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(error.message ?: "이 기기에서 음성 입력을 시작하지 못했어요.")
                }
            }
    }

    fun pickReceiptPhoto() {
        addOpen = true
        addModeName = AddMode.PHOTO_REVIEW.name
        if (photoImportState !is PhotoImportState.Review) photoImportState = PhotoImportState.Idle
        receiptPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun generateSpendingAnalysis() {
        if (uiState.expenseCount == 0) return
        val requestedState = uiState
        val request = analysisRequests.start()
        aiAnalysisState = SpendingAnalysisState.Loading
        coroutineScope.launch {
            val result = aiClient.analyzeSpending(requestedState)
            if (!analysisRequests.isCurrent(request) || requestedState.month != uiState.month) return@launch
            aiAnalysisState = result.fold(
                onSuccess = { analysis -> SpendingAnalysisState.Success(requestedState.month, analysis) },
                onFailure = { error -> SpendingAnalysisState.Error(error.message ?: "AI 분석에 실패했어요. 잠시 후 다시 시도해 주세요.") },
            )
        }
    }

    fun askSpendingQuestion(question: String) {
        if (question.isBlank() || uiState.monthTransactions.isEmpty()) return
        val requestedState = uiState
        val request = questionRequests.start()
        aiQuestionState = SpendingQuestionState.Loading
        coroutineScope.launch {
            val result = aiClient.askSpending(question.trim(), requestedState)
            if (!questionRequests.isCurrent(request) || requestedState.month != uiState.month) return@launch
            aiQuestionState = result.fold(
                onSuccess = { answer -> SpendingQuestionState.Success(requestedState.month, question.trim(), answer) },
                onFailure = { error -> SpendingQuestionState.Error(error.message ?: "AI 답변을 만들지 못했어요. 잠시 후 다시 시도해 주세요.") },
            )
        }
    }

    fun generateSharedSpendingAnalysis(month: java.time.YearMonth) {
        val requestedState = com.moasseum.app.domain.LedgerUiState(month = month, transactions = sharedSnapshot.transactions)
        if (requestedState.expenseCount == 0) return
        val request = sharedAnalysisRequests.start()
        sharedAiAnalysisState = SpendingAnalysisState.Loading
        coroutineScope.launch {
            val result = aiClient.analyzeSpending(requestedState)
            if (!sharedAnalysisRequests.isCurrent(request)) return@launch
            sharedAiAnalysisState = result.fold(
                onSuccess = { SpendingAnalysisState.Success(month, it) },
                onFailure = { SpendingAnalysisState.Error(it.message ?: "공동 소비 분석에 실패했어요. 잠시 후 다시 시도해 주세요.") },
            )
        }
    }

    fun askSharedSpendingQuestion(question: String, month: java.time.YearMonth) {
        if (question.isBlank()) return
        val requestedState = com.moasseum.app.domain.LedgerUiState(month = month, transactions = sharedSnapshot.transactions)
        if (requestedState.monthTransactions.isEmpty()) return
        val request = sharedQuestionRequests.start()
        sharedAiQuestionState = SpendingQuestionState.Loading
        coroutineScope.launch {
            val result = aiClient.askSpending(question.trim(), requestedState)
            if (!sharedQuestionRequests.isCurrent(request)) return@launch
            sharedAiQuestionState = result.fold(
                onSuccess = { SpendingQuestionState.Success(month, question.trim(), it) },
                onFailure = { SpendingQuestionState.Error(it.message ?: "공동 장부 답변을 만들지 못했어요. 잠시 후 다시 시도해 주세요.") },
            )
        }
    }

    LaunchedEffect(uiState.month) {
        analysisRequests.invalidate()
        questionRequests.invalidate()
        aiAnalysisState = SpendingAnalysisState.Idle
        aiQuestionState = SpendingQuestionState.Idle
    }

    fun closeAdd() {
        if (savingTransaction) return
        addOpen = false
        addModeName = AddMode.MENU.name
        sharedPrefillText = null
        aiState = AiParseState.Idle
        photoImportState = PhotoImportState.Idle
        shareNewRecords = false
    }

    fun completeTransactionSave(result: Result<Long>, successMessage: String) {
        savingTransaction = false
        result.fold(
            onSuccess = { id ->
                val shouldShare = shareNewRecords && sharedSnapshot.ledgerId != null
                if (!shouldShare) {
                    closeAdd()
                    coroutineScope.launch { snackbarHostState.showSnackbar(successMessage) }
                    return@fold
                }
                closeAdd()
                sharedBusy = true
                sharedError = null
                coroutineScope.launch {
                    try {
                        sharedSnapshot = application.sharedLedgerRepository.shareTransactions(listOf(id))
                        snackbarHostState.showSnackbar("저장하고 함께 공유했어요.")
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        sharedError = error.message ?: "공유를 동기화하지 못했어요."
                        snackbarHostState.showSnackbar("개인 장부에는 저장했어요. 함께 공유는 연결 후 다시 동기화돼요.")
                    } finally {
                        sharedBusy = false
                    }
                }
            },
            onFailure = { error ->
                coroutineScope.launch { snackbarHostState.showSnackbar(error.message ?: "거래 저장에 실패했어요.") }
            },
        )
    }

    fun continueWithDownloadedUpdate() {
        val apkFile = downloadedUpdatePath?.let(::File)
        if (apkFile == null) {
            updateState = UpdateCheckState.Error("다운로드된 업데이트 파일이 없어요. 업데이트를 다시 확인해 주세요.")
            return
        }
        if (!AppUpdateManager.canInstallFromThisApp(context)) {
            waitingForInstallPermission = true
            updateState = UpdateCheckState.WaitingForInstallPermission
            runCatching { AppUpdateManager.openInstallPermissionSettings(context) }
                .onFailure { error -> updateState = UpdateCheckState.Error(error.message ?: "설치 권한 설정을 열지 못했어요.") }
            return
        }
        waitingForInstallPermission = false
        updateState = UpdateCheckState.OpeningInstaller
        AppUpdateManager.launchInstaller(context, apkFile).fold(
            onSuccess = { updateState = UpdateCheckState.InstallerOpened },
            onFailure = { error -> updateState = UpdateCheckState.Error(error.message ?: "Android 설치 화면을 열지 못했어요.") },
        )
    }

    fun downloadAndInstallUpdate(release: com.moasseum.app.update.AppRelease) {
        updateState = UpdateCheckState.Downloading(0)
        coroutineScope.launch {
            val result = AppUpdateManager.downloadApk(context, release) { progress ->
                coroutineScope.launch { updateState = UpdateCheckState.Downloading(progress) }
            }
            result.fold(
                onSuccess = { file ->
                    downloadedUpdatePath = file.absolutePath
                    continueWithDownloadedUpdate()
                },
                onFailure = { error -> updateState = UpdateCheckState.Error(error.message ?: "업데이트 APK를 다운로드하지 못했어요.") },
            )
        }
    }

    DisposableEffect(lifecycleOwner, waitingForInstallPermission, downloadedUpdatePath) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && waitingForInstallPermission) {
                waitingForInstallPermission = false
                if (AppUpdateManager.canInstallFromThisApp(context)) {
                    continueWithDownloadedUpdate()
                } else {
                    updateState = UpdateCheckState.WaitingForInstallPermission
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = addOpen) { closeAdd() }
    val transitionDuration = if (reduceMotion) 0 else 160

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route == currentRoute) {
                        when (route) {
                            ROUTE_HOME -> homeScrollToTopRequest++
                            ROUTE_HISTORY -> historyScrollToTopRequest++
                            ROUTE_TOGETHER -> togetherScrollToTopRequest++
                            ROUTE_MANAGE -> manageScrollToTopRequest++
                        }
                    } else navigateTo(navController, route)
                },
                addExpanded = addOpen,
                onAdd = {
                    if (addOpen) closeAdd() else {
                        addOpen = true
                        addModeName = AddMode.MENU.name
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(transitionDuration)) },
                exitTransition = { fadeOut(tween(transitionDuration)) },
                popEnterTransition = { fadeIn(tween(transitionDuration)) },
                popExitTransition = { fadeOut(tween(transitionDuration)) },
            ) {
                composable(ROUTE_HOME) {
                    HomeScreen(
                        uiState = uiState,
                        aiAnalysisState = aiAnalysisState,
                        onGenerateAiAnalysis = ::generateSpendingAnalysis,
                        aiQuestionState = aiQuestionState,
                        onAskAiQuestion = ::askSpendingQuestion,
                        categoryBudgets = categoryBudgets,
                        onExportCsv = { exportCsvLauncher.launch("moasseum-${java.time.LocalDate.now()}.csv") },
                        onExportPdf = { exportPdfLauncher.launch("moasseum-report-${uiState.month}.pdf") },
                        onSelectMonth = viewModel::selectMonth,
                        onAdd = { mode ->
                            addModeName = mode.name
                            addOpen = true
                        },
                        onOpenManage = { navigateTo(navController, ROUTE_MANAGE) },
                        onOpenHistory = { navigateTo(navController, ROUTE_HISTORY) },
                        onOpenTransaction = { id ->
                            uiState.transactions.firstOrNull { it.id == id }?.let { transaction ->
                                pendingHistoryTransactionId = id
                                viewModel.selectMonth(java.time.YearMonth.from(transaction.occurredDate))
                                viewModel.selectDate(transaction.occurredDate)
                            }
                            navigateTo(navController, ROUTE_HISTORY)
                        },
                        onOpenHelp = { showQuickHelp = true },
                        onOpenNotifications = { navigateTo(navController, ROUTE_NOTIFICATIONS) },
                        onStartVoiceInput = ::startVoiceInput,
                        onPickReceipt = ::pickReceiptPhoto,
                        onTakeReceipt = ::takeReceiptPhoto,
                        displayName = profileDisplayName.ifBlank {
                            authState.user?.email?.substringBefore("@")?.takeIf(String::isNotBlank) ?: "모아씀"
                        },
                        onSaveDisplayName = onSaveDisplayName,
                        pendingCount = pendingCandidates.size + unreadActivityNoticeCount,
                        homeDashboardCards = homeDashboardCards,
                        noSpendChallenge = noSpendChallenge,
                        scrollToTopRequest = homeScrollToTopRequest,
                    )
                }
                composable(ROUTE_HISTORY) {
                    HistoryScreen(
                        uiState = uiState,
                        paymentMethods = paymentMethods,
                        selectedDate = selectedDate,
                        onSelectDate = viewModel::selectDate,
                        onSelectMonth = viewModel::selectMonth,
                        onDeleteTransaction = { id ->
                            viewModel.deleteTransaction(id) {
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "거래를 삭제했어요",
                                        actionLabel = "실행 취소",
                                        withDismissAction = true,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreTransaction(id)
                                }
                            }
                        },
                        onUpdateTransaction = viewModel::updateTransaction,
                        onExportCsv = { exportCsvLauncher.launch("moasseum-${java.time.LocalDate.now()}.csv") },
                        onExportJson = { exportJsonLauncher.launch("moasseum-${java.time.LocalDate.now()}.json") },
                        onImportCsv = { importCsvLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel")) },
                        initialTransactionId = pendingHistoryTransactionId,
                        onInitialTransactionHandled = { pendingHistoryTransactionId = null },
                        scrollToTopRequest = historyScrollToTopRequest,
                    )
                }
                composable(ROUTE_NOTIFICATIONS) {
                    NotificationsScreen(
                        candidates = pendingCandidates,
                        activityNotices = activityNotices,
                        readActivityNoticeIds = readActivityNoticeIds,
                        onReview = { candidate ->
                            notificationPromptIds = listOf(candidate.id) + notificationPromptIds.filterNot { it == candidate.id }
                        },
                        onDismiss = { id ->
                            viewModel.dismissNotificationCandidate(id)
                            notificationPromptIds = notificationPromptIds.filterNot { it == id }
                            PaymentNotificationNotifier.cancel(context, id)
                        },
                        onDismissAll = {
                            val ids = pendingCandidates.map { it.id }
                            viewModel.dismissAllNotificationCandidates()
                            notificationPromptIds = notificationPromptIds.filterNot { it in ids }
                            ids.forEach { PaymentNotificationNotifier.cancel(context, it) }
                        },
                        onMarkRead = onMarkActivityNoticesRead,
                        onOpenSettings = { navigateTo(navController, ROUTE_MANAGE) },
                    )
                }
                composable(ROUTE_LEGACY_TOGETHER) {
                    NotificationsScreen(
                        candidates = pendingCandidates,
                        activityNotices = activityNotices,
                        readActivityNoticeIds = readActivityNoticeIds,
                        onReview = { candidate ->
                            notificationPromptIds = listOf(candidate.id) + notificationPromptIds.filterNot { it == candidate.id }
                        },
                        onDismiss = { id ->
                            viewModel.dismissNotificationCandidate(id)
                            notificationPromptIds = notificationPromptIds.filterNot { it == id }
                            PaymentNotificationNotifier.cancel(context, id)
                        },
                        onDismissAll = {
                            val ids = pendingCandidates.map { it.id }
                            viewModel.dismissAllNotificationCandidates()
                            notificationPromptIds = notificationPromptIds.filterNot { it in ids }
                            ids.forEach { PaymentNotificationNotifier.cancel(context, it) }
                        },
                        onMarkRead = onMarkActivityNoticesRead,
                        onOpenSettings = { navigateTo(navController, ROUTE_MANAGE) },
                    )
                }
                composable(ROUTE_TOGETHER) {
                    TogetherScreen(
                        email = authState.user?.email,
                        userId = authState.user?.id,
                        snapshot = sharedSnapshot,
                        personalTransactions = uiState.transactions,
                        busy = sharedBusy,
                        error = sharedError,
                        onLogin = { showAuth = true },
                        onCreateInvite = {
                            runSharedAction("초대 코드를 만들었어요.") { application.sharedLedgerRepository.createInvite() }
                        },
                        onJoin = { code ->
                            runSharedAction("파트너 장부에 참여했어요.") { application.sharedLedgerRepository.join(code) }
                        },
                        onRefresh = {
                            runSharedAction { application.sharedLedgerRepository.refresh() }
                        },
                        onToggleShare = { transaction, shared ->
                            runSharedAction(if (shared) "거래를 파트너와 공유했어요." else "공유를 해제했어요.") {
                                application.sharedLedgerRepository.setShared(transaction.id, shared)
                            }
                        },
                        onCopyInvite = { code ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "모아씀 공유 장부 초대 코드: $code\n24시간 이내에 앱에서 한 번만 사용할 수 있어요.")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "초대 코드 공유"))
                        },
                        onSaveGoal = { goalId, title, targetAmount, currentAmount, monthKey ->
                            runSharedAction("공동 목표를 저장했어요.") {
                                application.sharedLedgerRepository.saveGoal(goalId, title, targetAmount, currentAmount, monthKey)
                            }
                        },
                        onDeleteGoal = { goalId ->
                            runSharedAction("공동 목표를 삭제했어요.") {
                                application.sharedLedgerRepository.deleteGoal(goalId)
                            }
                        },
                        onSaveFinanceItem = { item ->
                            runSharedAction("공동 재정 항목을 저장했어요.") {
                                application.sharedLedgerRepository.saveFinanceItem(item)
                            }
                        },
                        onDeleteFinanceItem = { itemId ->
                            runSharedAction("공동 항목을 삭제했어요.") {
                                application.sharedLedgerRepository.deleteFinanceItem(itemId)
                            }
                        },
                        onSaveSettings = { settings ->
                            runSharedAction("함께 쓰는 방식을 저장했어요.") {
                                application.sharedLedgerRepository.saveSettings(settings)
                            }
                        },
                        onLeaveSharedLedger = {
                            runSharedAction("공유 연결을 해제했어요.") {
                                application.sharedLedgerRepository.leaveSharedLedger()
                            }
                        },
                        onExportReport = { month ->
                            sharedReportMonth = month
                            exportSharedPdfLauncher.launch("moasseum-together-${month}.pdf")
                        },
                        aiAnalysisState = sharedAiAnalysisState,
                        aiQuestionState = sharedAiQuestionState,
                        onAnalyzeShared = ::generateSharedSpendingAnalysis,
                        onAskShared = ::askSharedSpendingQuestion,
                        scrollToTopRequest = togetherScrollToTopRequest,
                        isOnline = isInternetValidated,
                    )
                }
                composable(ROUTE_MANAGE) {
                    ManageScreen(
                        onOpenAuth = { showAuth = true },
                        onOpenGuide = { showQuickHelp = true },
                        accountStatus = authState.user?.email ?: if (application.authRepository.configured) "로그인 안 됨" else "서버 연결 필요",
                        scrollToTopRequest = manageScrollToTopRequest,
                        aiLoginRequired = authState.user == null,
                        onSetBudgetRollover = viewModel::setBudgetRollover,
                        onExportBackup = { exportJsonLauncher.launch("moasseum-full-${java.time.LocalDate.now()}.json") },
                        onRestoreBackup = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                        onExportSafetyBackup = { exportSafetyLauncher.launch("moasseum-before-restore.json") },
                        onExportPdf = { exportPdfLauncher.launch("moasseum-report-${uiState.month}.pdf") },
                        onExportYearTransactions = { year ->
                            yearCsvExport = year
                            exportYearCsvLauncher.launch("moasseum-year-$year.csv")
                        },
                        onOpenAccounts = { showAccounts = true },
                        uiState = uiState,
                        homeDashboardCards = homeDashboardCards,
                        onSaveHomeDashboardCards = onSaveHomeDashboardCards,
                        noSpendChallenge = noSpendChallenge,
                        onSaveNoSpendChallenge = onSaveNoSpendChallenge,
                        paymentMethods = paymentMethods,
                        paymentCards = paymentCards,
                        onSavePaymentCards = onSavePaymentCards,
                        categoryBudgets = categoryBudgets,
                        onSaveCategoryBudgets = onSaveCategoryBudgets,
                        onSavePaymentMethods = onSavePaymentMethods,
                        onSaveCategoryLabels = onSaveCategoryLabels,
                        categoryOrder = categoryOrder,
                        onSaveCategoryOrder = onSaveCategoryOrder,
                        monthlyIncomeTarget = monthlyIncomeTargets[uiState.month.toString()],
                        onSaveMonthlyIncomeTarget = { amount -> onSaveMonthlyIncomeTarget(uiState.month.toString(), amount) },
                        onDeleteCustomCategory = { key, labels ->
                            require(labels.values.all { it.isNotBlank() })
                            require(labels.values.map { it.trim().lowercase(java.util.Locale.ROOT) }.distinct().size == labels.size)
                            application.financeRepository.reassignDeletedCustomCategory(key)
                            application.preferencesRepository.saveCategoryLabels(labels)
                        },
                        recurringRules = recurringRules,
                        onAddRecurringRule = viewModel::addRecurringRule,
                        onEditRecurringRule = viewModel::editRecurringRule,
                        onSetRecurringRuleActive = viewModel::setRecurringRuleActive,
                        onDeleteRecurringRule = viewModel::deleteRecurringRule,
                        onAddInstallmentPlan = viewModel::addInstallmentPlan,
                        onDeleteInstallmentPlan = viewModel::deleteInstallmentPlan,
                        onClearLocalData = {
                            viewModel.clearAllLocalRecords { result ->
                                coroutineScope.launch {
                                    result.fold(
                                        onSuccess = { snackbarHostState.showSnackbar("기기 거래 데이터, 알림 후보, 반복 규칙을 삭제했어요.") },
                                        onFailure = { error -> snackbarHostState.showSnackbar(error.message ?: "데이터를 삭제하지 못했어요.") },
                                    )
                                }
                            }
                        },
                        darkTheme = darkTheme,
                        reduceMotion = reduceMotion,
                        onDarkThemeChanged = onDarkThemeChanged,
                        onReduceMotionChanged = onReduceMotionChanged,
                        onUpdateBudget = viewModel::updateBudget,
                        notificationAccessEnabled = notificationAccessEnabled,
                        appNotificationsEnabled = appNotificationsEnabled,
                        aiNotificationClassificationEnabled = aiNotificationClassificationEnabled,
                        onSetAiNotificationClassificationEnabled = onSetAiNotificationClassificationEnabled,
                        onOpenCandidates = { navigateTo(navController, ROUTE_NOTIFICATIONS) },
                        pendingCandidates = pendingCandidates,
                        notificationServiceConnectedAt = notificationServiceConnectedAt,
                        notificationServiceDisconnectedAt = notificationServiceDisconnectedAt,
                        notificationLastSeenAt = notificationLastSeenAt,
                        appLockEnabled = appLockEnabled,
                        onSetAppLockEnabled = onSetAppLockEnabled,
                        financeRemindersEnabled = financeRemindersEnabled,
                        onSetFinanceRemindersEnabled = onSetFinanceRemindersEnabled,
                        updateState = updateState,
                        onCheckForUpdate = {
                            updateState = UpdateCheckState.Checking
                            coroutineScope.launch {
                                AppUpdateManager.checkForUpdate()
                                    .onSuccess { release ->
                                        updateState = release?.let(UpdateCheckState::Available)
                                            ?: UpdateCheckState.UpToDate
                                    }
                                    .onFailure { error ->
                                        updateState = UpdateCheckState.Error(
                                            error.message ?: "업데이트 확인에 실패했어요.",
                                        )
                                    }
                            }
                        },
                        onInstallUpdate = ::downloadAndInstallUpdate,
                        onContinueInstall = ::continueWithDownloadedUpdate,
                        onOpenNotificationSettings = {
                            NotificationAccess.openSettings(context)
                        },
                        onOpenAppNotificationSettings = {
                            NotificationAccess.openAppNotificationSettings(context)
                        },
                    )
                }
            }
        }
    }

    if (addOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { closeAdd() },
            sheetState = sheetState,
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
        ) {
            AddTransactionSheet(
                mode = addMode,
                onModeChange = { if (!savingTransaction) addModeName = it.name },
                onDismiss = { closeAdd() },
                aiState = aiState,
                paymentMethods = paymentMethods,
                accounts = accounts,
                saving = savingTransaction,
                onParseAi = { text ->
                    aiState = AiParseState.Loading
                    coroutineScope.launch {
                        val result = aiClient.parseTransaction(text)
                        aiState = result.fold(
                            onSuccess = { candidate -> AiParseState.Success(candidate) },
                            onFailure = { error -> AiParseState.Error(error.message ?: "AI 해석에 실패했어요.") },
                        )
                    }
                },
                onConfirmAi = { amount, type, merchant, categoryKey, memo, paymentMethod, occurredAt ->
                    val alreadySaving = savingTransaction
                    if (!alreadySaving) savingTransaction = true
                    val saved = !alreadySaving && viewModel.addTransaction(
                        amount, type, merchant, categoryKey, memo, occurredAt, paymentMethod,
                        onComplete = { result -> completeTransactionSave(result, "AI 거래 후보를 저장했어요") },
                    )
                    if (!saved && !alreadySaving) savingTransaction = false
                    saved
                },
                onStartVoiceInput = ::startVoiceInput,
                onPickReceipt = ::pickReceiptPhoto,
                onTakeReceipt = ::takeReceiptPhoto,
                photoImportState = photoImportState,
                onPickMorePhotos = { unselectedIds ->
                    (photoImportState as? PhotoImportState.Review)?.let { review ->
                        photoImportState = review.copy(unselectedCandidateIds = unselectedIds)
                    }
                    pickReceiptPhoto()
                },
                onAddPhotoCandidates = { candidates ->
                    if (!savingTransaction && candidates.isNotEmpty()) {
                        val shouldShare = shareNewRecords && sharedSnapshot.ledgerId != null
                        savingTransaction = true
                        coroutineScope.launch {
                            val result = try {
                                Result.success(viewModel.importPhotoTransactions(candidates))
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                Result.failure(error)
                            }
                            savingTransaction = false
                            result.onSuccess { saved ->
                                closeAdd()
                                if (shouldShare && saved.insertedIds.isNotEmpty()) {
                                    sharedBusy = true
                                    sharedError = null
                                    try {
                                        sharedSnapshot = application.sharedLedgerRepository.shareTransactions(saved.insertedIds)
                                        snackbarHostState.showSnackbar("${saved.insertedCount}건 저장 후 함께 공유했어요${if (saved.duplicateCount > 0) " · 중복 ${saved.duplicateCount}건 제외" else ""}")
                                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        sharedError = error.message ?: "공유를 동기화하지 못했어요."
                                        snackbarHostState.showSnackbar("개인 장부에 ${saved.insertedCount}건 저장했어요. 함께 공유는 연결 후 동기화돼요.")
                                    } finally {
                                        sharedBusy = false
                                    }
                                } else {
                                    snackbarHostState.showSnackbar(buildString {
                                        append("${saved.insertedCount}건 추가")
                                        if (saved.duplicateCount > 0) append(" · 중복 ${saved.duplicateCount}건 제외")
                                    })
                                }
                            }.onFailure { error -> snackbarHostState.showSnackbar(error.message ?: "사진 내역을 저장하지 못했어요.") }
                        }
                    }
                },
                canShareOnSave = authState.user != null && sharedSnapshot.ledgerId != null,
                shareOnSave = shareNewRecords,
                onShareOnSaveChange = { shareNewRecords = it },
                speechResult = speechResult,
                onSpeechResultConsumed = { speechResult = null },
                prefillText = sharedPrefillText,
                prefillKey = sharedPrefillKey,
                transactions = uiState.transactions,
                onAddBatch = { candidates, method ->
                    if (savingTransaction) false else {
                        savingTransaction = true
                        coroutineScope.launch {
                            val result = runCatching { viewModel.addBatchTransactions(candidates, method) }
                            savingTransaction = false
                            result.onSuccess { count ->
                                closeAdd()
                                snackbarHostState.showSnackbar("거래 ${count}건을 추가했어요")
                            }.onFailure { error -> snackbarHostState.showSnackbar(error.message ?: "일괄 추가에 실패했어요.") }
                        }
                        true
                    }
                },
                onParseBatchWithAi = { text -> aiClient.parseBatchCommand(text, uiState.transactions) },
                onApplyBatch = { action, ids, edit ->
                    if (savingTransaction || ids.isEmpty()) false else {
                        savingTransaction = true
                        coroutineScope.launch {
                            val result = runCatching {
                                when (action) {
                                    com.moasseum.app.data.BatchCommandAction.DELETE -> viewModel.deleteBatchTransactions(ids)
                                    com.moasseum.app.data.BatchCommandAction.UPDATE -> viewModel.editBatchTransactions(ids, requireNotNull(edit))
                                    com.moasseum.app.data.BatchCommandAction.ADD -> error("추가 작업에는 거래 미리보기를 사용해 주세요.")
                                }
                            }
                            savingTransaction = false
                            result.onSuccess { count ->
                                closeAdd()
                                val deleted = action == com.moasseum.app.data.BatchCommandAction.DELETE
                                val snackbar = snackbarHostState.showSnackbar(
                                    message = if (deleted) "거래 ${count}건을 삭제했어요" else "거래 ${count}건을 수정했어요",
                                    actionLabel = if (deleted) "실행 취소" else null,
                                    withDismissAction = deleted,
                                )
                                if (deleted && snackbar == SnackbarResult.ActionPerformed) {
                                    val restored = runCatching { viewModel.restoreBatchTransactions(ids) }
                                    restored.onFailure { error -> snackbarHostState.showSnackbar(error.message ?: "거래 복원에 실패했어요.") }
                                }
                            }.onFailure { error -> snackbarHostState.showSnackbar(error.message ?: "일괄 처리에 실패했어요.") }
                        }
                        true
                    }
                },
                onSave = { amount, type, merchant, categoryKey, memo, paymentMethod, accountId, occurredAt, installmentCount ->
                    if (savingTransaction) {
                        false
                    } else if (installmentCount != null) {
                        val total = parseAmount(amount)
                        if (type != com.moasseum.app.domain.TransactionType.EXPENSE || total == null ||
                            installmentCount !in 2..60 || total < installmentCount
                        ) {
                            false
                        } else {
                            savingTransaction = true
                            coroutineScope.launch {
                                val result = runCatching {
                                    viewModel.addInstallmentPlan(total, installmentCount, occurredAt, categoryKey, merchant, memo, paymentMethod)
                                }
                                savingTransaction = false
                                result.onSuccess {
                                    closeAdd()
                                    snackbarHostState.showSnackbar("할부 ${installmentCount}개월을 등록했어요")
                                }.onFailure { error -> snackbarHostState.showSnackbar(error.message ?: "할부를 저장하지 못했어요.") }
                            }
                            true
                        }
                    } else {
                        savingTransaction = true
                        val saved = viewModel.addTransaction(
                            amount, type, merchant, categoryKey, memo,
                            occurredAt = occurredAt,
                            paymentMethod = paymentMethod,
                            accountId = accountId,
                            onComplete = { result -> completeTransactionSave(result, "거래가 저장됐어요") },
                        )
                        if (!saved) savingTransaction = false
                        saved
                    }
                },
            )
        }
    }

    if (showAccounts) com.moasseum.app.ui.screens.AccountsDialog(accounts, uiState.transactions, application.financeRepository) { showAccounts = false }
    if (showAuth) com.moasseum.app.ui.screens.AuthDialog(application.authRepository) { showAuth = false }
    backupToRestore?.let { backup ->
        AlertDialog(onDismissRequest = { if (!restoringBackup) backupToRestore = null }, title = { Text("백업을 복원할까요?") },
            text = { Column {
                Text("거래 ${backup.transactions.count { it.deletedAt == null }}건 · 계좌 ${backup.accounts.size}개 · 예산 ${backup.budgets.size}개월 · 반복 ${backup.recurringRules.size}개")
                Text(if (backup.legacy) "이전 버전의 거래 백업입니다. 기존 내역을 유지하고 중복을 제외한 거래를 추가해요." else "이 기기의 가계부와 앱 설정을 백업 내용으로 교체합니다. 현재 상태는 먼저 안전 백업되며, 관리에서 내보낼 수 있어요. 알림 권한·AI 전송 동의는 바꾸지 않습니다.")
                if (restoringBackup) androidx.compose.material3.LinearProgressIndicator()
            } },
            confirmButton = { TextButton(enabled = !restoringBackup, onClick = {
                restoringBackup = true
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { application.backupManager.restore(backup) } }
                    restoringBackup = false
                    if (result.isSuccess) backupToRestore = null
                    snackbarHostState.showSnackbar(if (result.isSuccess) "${result.getOrNull()}건의 거래를 복원했어요." else result.exceptionOrNull()?.message ?: "복원에 실패했어요.")
                }
            }) { Text("복원") } }, dismissButton = { TextButton(enabled = !restoringBackup, onClick = { backupToRestore = null }) { Text("취소") } })
    }
    if (showFirstRunGuide) {
        FirstRunGuide(
            notificationAccessEnabled = notificationAccessEnabled,
            appNotificationsEnabled = appNotificationsReady,
            onOpenNotificationSettings = {
                notificationSettingsInProgress = true
                NotificationAccess.openSettings(context)
            },
            onOpenAppNotificationSettings = {
                notificationSettingsInProgress = true
                NotificationAccess.openAppNotificationSettings(context)
            },
            onFinish = ::closeFirstRunGuide,
        )
    }
    if (showQuickHelp && !showFirstRunGuide) {
        QuickHelpDialog(
            onDismiss = { showQuickHelp = false },
            onAdd = {
                showQuickHelp = false
                addOpen = true
                addModeName = AddMode.MENU.name
            },
            onHistory = { showQuickHelp = false; navigateTo(navController, ROUTE_HISTORY) },
            onManage = { showQuickHelp = false; navigateTo(navController, ROUTE_MANAGE) },
            onTogether = { showQuickHelp = false; navigateTo(navController, ROUTE_TOGETHER) },
            onFirstRunGuide = { showQuickHelp = false; guideOpenedManually = true },
        )
    }

    notificationPromptIds.firstNotNullOfOrNull { id -> pendingCandidates.firstOrNull { it.id == id } }?.let { candidate ->
        val saving = notificationSaving
        val direction = if (candidate.type == com.moasseum.app.domain.TransactionType.INCOME) "입금" else "지출"
        AlertDialog(
            onDismissRequest = { if (!saving.busy) notificationPromptIds = notificationPromptIds.filterNot { it == candidate.id } },
            title = { Text("거래를 인식했어요. 추가할까요?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$direction · ${com.moasseum.app.domain.formatWon(candidate.amount)}", style = MaterialTheme.typography.titleLarge)
                    Text(candidate.merchant)
                    saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving.busy, onClick = {
                    saving.save({ viewModel.acceptNotificationCandidate(candidate.id) }) {
                        notificationPromptIds = notificationPromptIds.filterNot { it == candidate.id }
                        PaymentNotificationNotifier.cancel(context, candidate.id)
                        coroutineScope.launch { snackbarHostState.showSnackbar("거래를 추가했어요") }
                    }
                }) { Text("추가") }
            },
            dismissButton = {
                TextButton(enabled = !saving.busy, onClick = { notificationPromptIds = notificationPromptIds.filterNot { it == candidate.id } }) { Text("나중에") }
            },
        )
    }

    if (showNotificationAccessPrompt) {
        NotificationSetupDialog(
            notificationAccessEnabled = notificationAccessEnabled,
            appNotificationsEnabled = appNotificationsReady,
            aiNotificationClassificationEnabled = aiNotificationClassificationEnabled,
            onOpenNextSetting = {
                showNotificationAccessPrompt = false
                notificationSetupDismissedThisSession = true
                notificationSettingsInProgress = true
                when {
                    !notificationAccessEnabled -> NotificationAccess.openSettings(context)
                    !appNotificationsReady -> NotificationAccess.openAppNotificationSettings(context)
                }
            },
            onDismiss = {
                showNotificationAccessPrompt = false
                notificationSetupDismissedThisSession = true
            },
        )
    }

    if (appLockEnabled && !appLockReleased) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = com.moasseum.app.ui.theme.LocalFinanceColors.current.accent, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.size(16.dp))
                    Text("모아씀 잠금", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.size(8.dp))
                    Text("계속하려면 본인 인증이 필요해요.", color = com.moasseum.app.ui.theme.LocalFinanceColors.current.textSecondary)
                    Spacer(Modifier.size(20.dp))
                    androidx.compose.material3.Button(onClick = onAuthenticateAppLock) { Text("다시 인증") }
                }
            }
        }
    }

    if (showWhatsNew && !showFirstRunGuide && !showNotificationAccessPrompt && !addOpen &&
        !showAccounts && !showAuth && notificationPromptIds.isEmpty() && !(appLockEnabled && !appLockReleased)
    ) {
        WhatsNewDialog(versionName = BuildConfig.VERSION_NAME) {
            showWhatsNew = false
            coroutineScope.launch { application.preferencesRepository.markReleaseNotesSeen(BuildConfig.VERSION_NAME) }
        }
    }
}

@Composable
private fun NotificationSetupDialog(
    notificationAccessEnabled: Boolean,
    appNotificationsEnabled: Boolean,
    aiNotificationClassificationEnabled: Boolean,
    onOpenNextSetting: () -> Unit,
    onDismiss: () -> Unit,
) {
    val nextSettingLabel = when {
        !notificationAccessEnabled -> "알림 읽기 설정 열기"
        !appNotificationsEnabled -> "알림 표시 설정 열기"
        else -> "확인"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(tonalElevation = 3.dp) {
                Icon(
                    Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        title = { Text("결제 알림 설정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NotificationSetupStep(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "알림 읽기",
                    message = if (notificationAccessEnabled) "카드·은행 알림 접근 허용됨" else "Galaxy 설정에서 모아씀을 켜야 해요",
                    enabled = notificationAccessEnabled,
                )
                NotificationSetupStep(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "인식 결과 알림",
                    message = if (appNotificationsEnabled) "인식되면 바로 알려드려요" else "모아씀 알림 권한이 필요해요",
                    enabled = appNotificationsEnabled,
                )
                Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("AI 알림 오탐 줄이기", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Text(
                                if (aiNotificationClassificationEnabled) {
                                    "켜짐"
                                } else {
                                    "꺼짐 · 관리에서 동의 후 사용 가능"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Text("감지된 거래는 확인 후 저장됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenNextSetting) { Text(nextSettingLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("나중에") }
        },
    )
}

@Composable
private fun NotificationSetupStep(
    icon: ImageVector,
    title: String,
    message: String,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(tonalElevation = 2.dp) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            if (enabled) "완료" else "필요",
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun navigateTo(navController: NavHostController, route: String) {
    if (navController.currentDestination?.route == route) return
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun hasValidatedInternet(connectivityManager: ConnectivityManager): Boolean {
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
