package com.moasseum.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moasseum.app.ui.components.AddMode
import com.moasseum.app.ui.components.AddTransactionSheet
import com.moasseum.app.ui.components.LocalCategoryLabels
import com.moasseum.app.ui.components.BottomNavBar
import com.moasseum.app.ui.components.ROUTE_HISTORY
import com.moasseum.app.ui.components.ROUTE_HOME
import com.moasseum.app.ui.components.ROUTE_MANAGE
import com.moasseum.app.ui.components.ROUTE_NOTIFICATIONS
import com.moasseum.app.ui.screens.HistoryScreen
import com.moasseum.app.ui.screens.HomeScreen
import com.moasseum.app.ui.screens.ManageScreen
import com.moasseum.app.ui.screens.NotificationsScreen
import com.moasseum.app.ui.theme.MoasseumTheme
import com.moasseum.app.data.AiClient
import com.moasseum.app.data.CsvBackup
import com.moasseum.app.data.DEFAULT_CATEGORY_LABELS
import com.moasseum.app.data.DEFAULT_PAYMENT_METHODS
import com.moasseum.app.data.JsonBackup
import com.moasseum.app.data.ReceiptOcr
import com.moasseum.app.domain.AiParseState
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.notification.NotificationAccess
import com.moasseum.app.notification.EXTRA_NOTIFICATION_CANDIDATE_ID
import com.moasseum.app.notification.PaymentNotificationNotifier
import com.moasseum.app.update.AppUpdateManager
import com.moasseum.app.update.UpdateCheckState
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.SnackbarResult

class MainActivity : ComponentActivity() {
    private val incomingNotificationCandidateId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingNotificationCandidateId.value = intentCandidateId(intent)
        val application = application as FinanceApplication
        consumeAuthIntent(intent)
        setContent {
            val viewModel: LedgerViewModel = viewModel(
                factory = LedgerViewModel.Factory(application.financeRepository),
            )
            val darkTheme by application.preferencesRepository.isDarkTheme.collectAsStateWithLifecycle(initialValue = true)
            val reduceMotion by application.preferencesRepository.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
            val categoryLabels by application.preferencesRepository.categoryLabels.collectAsStateWithLifecycle(initialValue = DEFAULT_CATEGORY_LABELS)
            val paymentMethods by application.preferencesRepository.paymentMethods.collectAsStateWithLifecycle(initialValue = DEFAULT_PAYMENT_METHODS)
            val paymentCards by application.preferencesRepository.paymentCards.collectAsStateWithLifecycle(initialValue = emptyList())
            val categoryBudgets by application.preferencesRepository.categoryBudgets.collectAsStateWithLifecycle(initialValue = emptyMap())
            val postNotificationPermissionPromptShown by application.preferencesRepository.notificationPostPermissionPromptShown.collectAsStateWithLifecycle(initialValue = false)
            val aiNotificationClassificationEnabled by application.preferencesRepository.aiNotificationClassificationEnabled.collectAsStateWithLifecycle(initialValue = false)
            val notificationServiceConnectedAt by application.preferencesRepository.notificationServiceConnectedAt.collectAsStateWithLifecycle(initialValue = null)
            val notificationServiceDisconnectedAt by application.preferencesRepository.notificationServiceDisconnectedAt.collectAsStateWithLifecycle(initialValue = null)
            val notificationLastSeenAt by application.preferencesRepository.notificationLastSeenAt.collectAsStateWithLifecycle(initialValue = null)
            val notificationLastCandidateAt by application.preferencesRepository.notificationLastCandidateAt.collectAsStateWithLifecycle(initialValue = null)
            val candidateIdFromNotification by incomingNotificationCandidateId.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalCategoryLabels provides categoryLabels) {
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
                        onMarkNotificationPostPermissionPromptShown = {
                            viewModel.performOperation("알림 안내 설정을 저장하지 못했어요.") { application.preferencesRepository.setNotificationPostPermissionPromptShown() }
                        },
                        aiNotificationClassificationEnabled = aiNotificationClassificationEnabled,
                        incomingNotificationCandidateId = candidateIdFromNotification,
                        onIncomingNotificationCandidateConsumed = { id ->
                            if (incomingNotificationCandidateId.value == id) incomingNotificationCandidateId.value = null
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingNotificationCandidateId.value = intentCandidateId(intent)
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
}

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
    onSetAiNotificationClassificationEnabled: (Boolean) -> Unit,
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    notificationPostPermissionPromptShown: Boolean,
    onMarkNotificationPostPermissionPromptShown: () -> Unit,
    aiNotificationClassificationEnabled: Boolean,
    notificationServiceConnectedAt: Long?,
    notificationServiceDisconnectedAt: Long?,
    notificationLastSeenAt: Long?,
    notificationLastCandidateAt: Long?,
    incomingNotificationCandidateId: Long?,
    onIncomingNotificationCandidateConsumed: (Long) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.date.collectAsStateWithLifecycle()
    val pendingCandidates by viewModel.pendingNotificationCandidates.collectAsStateWithLifecycle()
    val recurringRules by viewModel.recurringRules.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationAccessEnabled by remember { mutableStateOf(NotificationAccess.isEnabled(context)) }
    var appNotificationsEnabled by remember { mutableStateOf(NotificationAccess.areAppNotificationsEnabled(context)) }
    var showNotificationAccessPrompt by remember { mutableStateOf(false) }
    var notificationSetupDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var notificationSettingsInProgress by rememberSaveable { mutableStateOf(false) }
    var notificationPromptIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var postNotificationPermissionRequestStarted by rememberSaveable { mutableStateOf(false) }
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
    LaunchedEffect(notificationAccessEnabled, appNotificationsReady, notificationSetupDismissedThisSession) {
        val needsNotificationSetup = !notificationAccessEnabled || !appNotificationsReady
        showNotificationAccessPrompt = needsNotificationSetup && !notificationSetupDismissedThisSession
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
    ) {
        if (notificationAccessEnabled && postNotificationPermissionMissing && !notificationPostPermissionPromptShown &&
            !postNotificationPermissionRequestStarted && notificationSetupDismissedThisSession &&
            !notificationSettingsInProgress && !showNotificationAccessPrompt
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
    val notificationSaving = com.moasseum.app.ui.components.rememberSaveActionState()
    val coroutineScope = rememberCoroutineScope()
    val aiClient = remember { application.aiClient }
    val accounts by application.financeRepository.observeAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    var showAccounts by rememberSaveable { mutableStateOf(false) }
    var showAuth by rememberSaveable { mutableStateOf(false) }
    val authState by application.authRepository.state.collectAsStateWithLifecycle()
    LaunchedEffect(authState.linkEvent) {
        if (authState.linkEvent > 0) showAuth = true
    }
    var aiState by remember { mutableStateOf<AiParseState>(AiParseState.Idle) }
    var aiAnalysisState by remember { mutableStateOf<SpendingAnalysisState>(SpendingAnalysisState.Idle) }
    var aiQuestionState by remember { mutableStateOf<SpendingQuestionState>(SpendingQuestionState.Idle) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var savingTransaction by remember { mutableStateOf(false) }
    var addModeName by rememberSaveable { mutableStateOf(AddMode.MENU.name) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
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
    fun recognizeReceipt(uri: android.net.Uri, temporaryFile: java.io.File? = null) {
            aiState = AiParseState.Loading
            addOpen = true
            addModeName = AddMode.RECEIPT_NOTICE.name
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val text = ReceiptOcr.recognize(context, uri)
                        ReceiptOcr.candidateFromText(text)
                            ?: error("금액을 찾지 못했어요. 선명한 영수증 사진을 다시 선택해 주세요.")
                    }
                }
                result.fold(
                    onSuccess = { candidate ->
                        aiState = AiParseState.Success(candidate)
                        addModeName = AddMode.AI_INPUT.name
                    },
                    onFailure = { error -> aiState = AiParseState.Error(error.message ?: "영수증을 읽지 못했어요.") },
                )
                temporaryFile?.delete()
            }
    }
    val receiptPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) recognizeReceipt(uri)
    }
    var receiptCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val receiptCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = receiptCameraPath?.let { java.io.File(it) }
        receiptCameraPath = null
        if (file != null && success) recognizeReceipt(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file), file)
        else file?.delete()
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
        addModeName = AddMode.RECEIPT_NOTICE.name
        aiState = AiParseState.Idle
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
        aiState = AiParseState.Idle
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
                onNavigate = { route -> navigateTo(navController, route) },
                pendingCount = pendingCandidates.size,
                onAdd = {
                    addOpen = true
                    addModeName = AddMode.MENU.name
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
                        onAdd = { mode ->
                            addModeName = mode.name
                            addOpen = true
                        },
                        onOpenManage = { navigateTo(navController, ROUTE_MANAGE) },
                        onOpenHistory = { navigateTo(navController, ROUTE_HISTORY) },
                        onOpenHelp = { showHelpDialog = true },
                        onOpenNotifications = { navigateTo(navController, ROUTE_NOTIFICATIONS) },
                        onStartVoiceInput = ::startVoiceInput,
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
                    )
                }
                composable(ROUTE_NOTIFICATIONS) {
                    NotificationsScreen(
                        candidates = pendingCandidates,
                        onReview = { candidate ->
                            notificationPromptIds = listOf(candidate.id) + notificationPromptIds.filterNot { it == candidate.id }
                        },
                        onDismiss = { id ->
                            viewModel.dismissNotificationCandidate(id)
                            notificationPromptIds = notificationPromptIds.filterNot { it == id }
                            PaymentNotificationNotifier.cancel(context, id)
                        },
                        onOpenSettings = { navigateTo(navController, ROUTE_MANAGE) },
                    )
                }
                composable(ROUTE_MANAGE) {
                    ManageScreen(
                        onOpenAuth = { showAuth = true },
                        accountStatus = authState.user?.email ?: if (application.authRepository.configured) "로그인 안 됨" else "서버 연결 필요",
                        aiLoginRequired = authState.user == null,
                        onSetBudgetRollover = viewModel::setBudgetRollover,
                        onExportBackup = { exportJsonLauncher.launch("moasseum-full-${java.time.LocalDate.now()}.json") },
                        onRestoreBackup = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                        onExportSafetyBackup = { exportSafetyLauncher.launch("moasseum-before-restore.json") },
                        onExportPdf = { exportPdfLauncher.launch("moasseum-report-${uiState.month}.pdf") },
                        onOpenAccounts = { showAccounts = true },
                        uiState = uiState,
                        paymentMethods = paymentMethods,
                        paymentCards = paymentCards,
                        onSavePaymentCards = onSavePaymentCards,
                        categoryBudgets = categoryBudgets,
                        onSaveCategoryBudgets = onSaveCategoryBudgets,
                        onSavePaymentMethods = onSavePaymentMethods,
                        onSaveCategoryLabels = onSaveCategoryLabels,
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
                    val saved = !alreadySaving && viewModel.addTransaction(amount, type, merchant, categoryKey, memo, occurredAt, paymentMethod, onComplete = { result ->
                        savingTransaction = false
                        if (result.isSuccess) closeAdd()
                        coroutineScope.launch { snackbarHostState.showSnackbar(if (result.isSuccess) "AI 거래 후보를 저장했어요" else result.exceptionOrNull()?.message ?: "저장에 실패했어요.") }
                    })
                    if (!saved && !alreadySaving) savingTransaction = false
                    saved
                },
                onStartVoiceInput = ::startVoiceInput,
                onPickReceipt = ::pickReceiptPhoto,
                onTakeReceipt = {
                    runCatching {
                        val folder = java.io.File(context.cacheDir, "receipt-capture").apply { mkdirs() }
                        val file = java.io.File.createTempFile("receipt-", ".jpg", folder)
                        receiptCameraPath = file.absolutePath
                        receiptCameraLauncher.launch(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                    }.onFailure { error -> coroutineScope.launch { snackbarHostState.showSnackbar(error.message ?: "카메라를 열 수 없어요.") } }
                },
                speechResult = speechResult,
                onSpeechResultConsumed = { speechResult = null },
                onSave = { amount, type, merchant, categoryKey, memo, paymentMethod, accountId ->
                    val alreadySaving = savingTransaction
                    if (!alreadySaving) savingTransaction = true
                    val saved = !alreadySaving && viewModel.addTransaction(amount, type, merchant, categoryKey, memo, paymentMethod = paymentMethod, accountId = accountId, onComplete = { result ->
                        savingTransaction = false
                        if (result.isSuccess) closeAdd()
                        coroutineScope.launch { snackbarHostState.showSnackbar(if (result.isSuccess) "거래가 저장됐어요" else result.exceptionOrNull()?.message ?: "저장에 실패했어요.") }
                    })
                    if (!saved && !alreadySaving) savingTransaction = false
                    saved
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
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("모아씀 사용 안내") },
            text = {
                Text("+ 거래 기록\n내역: 검색·달력·필터\n관리: 예산·계좌·백업·알림 설정\n\n결제 알림은 확인 후 저장됩니다.")
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("확인") } },
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
