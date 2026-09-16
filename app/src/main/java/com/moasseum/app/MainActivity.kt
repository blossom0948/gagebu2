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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moasseum.app.ui.components.AddFloatingActionButton
import com.moasseum.app.ui.components.AddMode
import com.moasseum.app.ui.components.AddTransactionSheet
import com.moasseum.app.ui.components.LocalCategoryLabels
import com.moasseum.app.ui.components.BottomNavBar
import com.moasseum.app.ui.components.ROUTE_HISTORY
import com.moasseum.app.ui.components.ROUTE_HOME
import com.moasseum.app.ui.components.ROUTE_MANAGE
import com.moasseum.app.ui.components.ROUTE_TOGETHER
import com.moasseum.app.ui.screens.HistoryScreen
import com.moasseum.app.ui.screens.HomeScreen
import com.moasseum.app.ui.screens.ManageScreen
import com.moasseum.app.ui.screens.TogetherScreen
import com.moasseum.app.ui.theme.MoasseumTheme
import com.moasseum.app.data.AiClient
import com.moasseum.app.data.CsvBackup
import com.moasseum.app.data.DEFAULT_CATEGORY_LABELS
import com.moasseum.app.data.DEFAULT_PAYMENT_METHODS
import com.moasseum.app.data.ReceiptOcr
import com.moasseum.app.domain.AiParseState
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.SpendingAnalysisState
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
        setContent {
            val viewModel: LedgerViewModel = viewModel(
                factory = LedgerViewModel.Factory(application.financeRepository),
            )
            val darkTheme by application.preferencesRepository.isDarkTheme.collectAsStateWithLifecycle(initialValue = true)
            val reduceMotion by application.preferencesRepository.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
            val categoryLabels by application.preferencesRepository.categoryLabels.collectAsStateWithLifecycle(initialValue = DEFAULT_CATEGORY_LABELS)
            val paymentMethods by application.preferencesRepository.paymentMethods.collectAsStateWithLifecycle(initialValue = DEFAULT_PAYMENT_METHODS)
            val postNotificationPermissionPromptShown by application.preferencesRepository.notificationPostPermissionPromptShown.collectAsStateWithLifecycle(initialValue = false)
            val aiNotificationClassificationEnabled by application.preferencesRepository.aiNotificationClassificationEnabled.collectAsStateWithLifecycle(initialValue = false)
            val candidateIdFromNotification by incomingNotificationCandidateId.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalCategoryLabels provides categoryLabels) {
                MoasseumTheme(darkTheme = darkTheme, reduceMotion = reduceMotion) {
                    UpdateSystemBars(darkTheme)
                    MoasseumApp(
                        viewModel = viewModel,
                        application = application,
                        paymentMethods = paymentMethods,
                        onSavePaymentMethods = { methods ->
                            lifecycleScope.launch { application.preferencesRepository.savePaymentMethods(methods) }
                        },
                        onSaveCategoryLabels = { labels ->
                            lifecycleScope.launch { application.preferencesRepository.saveCategoryLabels(labels) }
                        },
                        onSetAiNotificationClassificationEnabled = { enabled ->
                            lifecycleScope.launch { application.preferencesRepository.setAiNotificationClassificationEnabled(enabled) }
                        },
                        darkTheme = darkTheme,
                        reduceMotion = reduceMotion,
                        onDarkThemeChanged = { enabled ->
                            lifecycleScope.launch { application.preferencesRepository.setDarkTheme(enabled) }
                        },
                        onReduceMotionChanged = { enabled ->
                            lifecycleScope.launch { application.preferencesRepository.setReduceMotion(enabled) }
                        },
                        notificationPromptShown = application.preferencesRepository.notificationAccessPromptShown.collectAsStateWithLifecycle(initialValue = false).value,
                        onMarkNotificationPromptShown = {
                            lifecycleScope.launch { application.preferencesRepository.setNotificationAccessPromptShown() }
                        },
                        notificationPostPermissionPromptShown = postNotificationPermissionPromptShown,
                        onMarkNotificationPostPermissionPromptShown = {
                            lifecycleScope.launch { application.preferencesRepository.setNotificationPostPermissionPromptShown() }
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
    onSavePaymentMethods: (List<String>) -> Unit,
    onSaveCategoryLabels: (Map<String, String>) -> Unit,
    onSetAiNotificationClassificationEnabled: (Boolean) -> Unit,
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    notificationPromptShown: Boolean,
    onMarkNotificationPromptShown: () -> Unit,
    notificationPostPermissionPromptShown: Boolean,
    onMarkNotificationPostPermissionPromptShown: () -> Unit,
    aiNotificationClassificationEnabled: Boolean,
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
    var notificationCandidatePrompt by remember { mutableStateOf<NotificationCandidate?>(null) }
    var postNotificationPermissionRequestStarted by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessEnabled = NotificationAccess.isEnabled(context)
                appNotificationsEnabled = NotificationAccess.areAppNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(notificationPromptShown, notificationAccessEnabled) {
        showNotificationAccessPrompt = !notificationPromptShown && !notificationAccessEnabled
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        appNotificationsEnabled = NotificationAccess.areAppNotificationsEnabled(context)
    }
    LaunchedEffect(notificationAccessEnabled, notificationPostPermissionPromptShown) {
        val permissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (notificationAccessEnabled && permissionMissing && !notificationPostPermissionPromptShown && !postNotificationPermissionRequestStarted) {
            postNotificationPermissionRequestStarted = true
            onMarkNotificationPostPermissionPromptShown()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(application.notificationCandidateEvents) {
        application.notificationCandidateEvents.collect { candidate -> notificationCandidatePrompt = candidate }
    }
    LaunchedEffect(incomingNotificationCandidateId, pendingCandidates) {
        val id = incomingNotificationCandidateId ?: return@LaunchedEffect
        val candidate = pendingCandidates.firstOrNull { it.id == id } ?: return@LaunchedEffect
        notificationCandidatePrompt = candidate
        onIncomingNotificationCandidateConsumed(id)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val aiClient = remember { AiClient() }
    var aiState by remember { mutableStateOf<AiParseState>(AiParseState.Idle) }
    var aiAnalysisState by remember { mutableStateOf<SpendingAnalysisState>(SpendingAnalysisState.Idle) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var addModeName by rememberSaveable { mutableStateOf(AddMode.MENU.name) }
    var unavailableMessage by rememberSaveable { mutableStateOf<String?>(null) }
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
    val receiptPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
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
            }
        }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val result = runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("선택한 위치에 CSV 파일을 쓸 수 없어요.")
                output.bufferedWriter(Charsets.UTF_8).use { it.write(CsvBackup.encode(uiState.transactions)) }
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) "${uiState.transactions.size}건을 CSV로 내보냈어요."
                    else result.exceptionOrNull()?.message ?: "CSV 내보내기에 실패했어요.",
                )
            }
        }
    }
    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val parsed = withContext(Dispatchers.IO) {
                    runCatching {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("선택한 CSV 파일을 열 수 없어요.")
                        val content = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        require(content.length <= 5_000_000) { "CSV 파일은 5MB 이하만 가져올 수 있어요." }
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
        aiAnalysisState = SpendingAnalysisState.Loading
        coroutineScope.launch {
            val result = aiClient.analyzeSpending(uiState)
            aiAnalysisState = result.fold(
                onSuccess = { analysis -> SpendingAnalysisState.Success(uiState.month, analysis) },
                onFailure = { error -> SpendingAnalysisState.Error(error.message ?: "AI 분석에 실패했어요. 잠시 후 다시 시도해 주세요.") },
            )
        }
    }

    fun closeAdd() {
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

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route -> navigateTo(navController, route) },
            )
        },
        floatingActionButton = {
            AddFloatingActionButton(
                expanded = addOpen,
                onClick = {
                    if (addOpen) closeAdd() else {
                        addOpen = true
                        addModeName = AddMode.AI_INPUT.name
                    }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(ROUTE_HOME) {
                    HomeScreen(
                        uiState = uiState,
                        aiAnalysisState = aiAnalysisState,
                        onGenerateAiAnalysis = ::generateSpendingAnalysis,
                        onExportCsv = { exportCsvLauncher.launch("moasseum-${java.time.LocalDate.now()}.csv") },
                        onAdd = { mode ->
                            addModeName = mode.name
                            addOpen = true
                        },
                        onOpenManage = { navigateTo(navController, ROUTE_MANAGE) },
                        onOpenHistory = { navigateTo(navController, ROUTE_HISTORY) },
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
                        onImportCsv = { importCsvLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel")) },
                    )
                }
                composable(ROUTE_TOGETHER) {
                    TogetherScreen(
                        onShowUnavailable = {
                            unavailableMessage = "공동 기능은 인증·서버 연결 후 사용할 수 있어요. 개인 기록은 지금도 기기에 안전하게 남습니다."
                        },
                    )
                }
                composable(ROUTE_MANAGE) {
                    ManageScreen(
                        uiState = uiState,
                        paymentMethods = paymentMethods,
                        onSavePaymentMethods = onSavePaymentMethods,
                        onSaveCategoryLabels = onSaveCategoryLabels,
                        onDeleteCustomCategory = { key, labels ->
                            coroutineScope.launch {
                                runCatching {
                                    application.financeRepository.reassignDeletedCustomCategory(key)
                                    application.preferencesRepository.saveCategoryLabels(labels)
                                }.onFailure { error ->
                                    snackbarHostState.showSnackbar(error.message ?: "카테고리를 삭제하지 못했어요.")
                                }
                            }
                        },
                        recurringRules = recurringRules,
                        onAddRecurringRule = viewModel::addRecurringRule,
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
                        pendingCandidates = pendingCandidates,
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
                        onAcceptNotificationCandidate = { id ->
                            viewModel.acceptNotificationCandidate(id)
                            PaymentNotificationNotifier.cancel(context, id)
                            coroutineScope.launch { snackbarHostState.showSnackbar("알림을 거래로 저장했어요") }
                        },
                        onDismissNotificationCandidate = { id ->
                            viewModel.dismissNotificationCandidate(id)
                            PaymentNotificationNotifier.cancel(context, id)
                            coroutineScope.launch { snackbarHostState.showSnackbar("알림 후보를 무시했어요") }
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
                onModeChange = { addModeName = it.name },
                onDismiss = { closeAdd() },
                aiState = aiState,
                paymentMethods = paymentMethods,
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
                    val saved = viewModel.addTransaction(amount, type, merchant, categoryKey, memo, occurredAt, paymentMethod)
                    if (saved) {
                        closeAdd()
                        coroutineScope.launch { snackbarHostState.showSnackbar("AI 거래 후보를 저장했어요") }
                    }
                    saved
                },
                onStartVoiceInput = ::startVoiceInput,
                onPickReceipt = ::pickReceiptPhoto,
                speechResult = speechResult,
                onSpeechResultConsumed = { speechResult = null },
                onSave = { amount, type, merchant, categoryKey, memo, paymentMethod ->
                    val saved = viewModel.addTransaction(amount, type, merchant, categoryKey, memo, paymentMethod = paymentMethod)
                    if (saved) {
                        closeAdd()
                        coroutineScope.launch { snackbarHostState.showSnackbar("거래가 저장됐어요") }
                    }
                    saved
                },
            )
        }
    }

    unavailableMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            unavailableMessage = null
        }
    }

    notificationCandidatePrompt?.let { candidate ->
        val direction = if (candidate.type == com.moasseum.app.domain.TransactionType.INCOME) "입금" else "지출"
        AlertDialog(
            onDismissRequest = { notificationCandidatePrompt = null },
            title = { Text("알림에서 거래를 인식했어요") },
            text = {
                Text("인식되었습니다. 추가할까요?\n$direction · ${candidate.merchant} · ${com.moasseum.app.domain.formatWon(candidate.amount)}")
            },
            confirmButton = {
                TextButton(onClick = {
                    notificationCandidatePrompt = null
                    viewModel.acceptNotificationCandidate(candidate.id)
                    PaymentNotificationNotifier.cancel(context, candidate.id)
                    coroutineScope.launch { snackbarHostState.showSnackbar("인식한 거래를 추가했어요") }
                }) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { notificationCandidatePrompt = null }) { Text("나중에") }
            },
        )
    }

    if (showNotificationAccessPrompt) {
        AlertDialog(
            onDismissRequest = {
                showNotificationAccessPrompt = false
                onMarkNotificationPromptShown()
            },
            title = { Text("결제 알림을 자동으로 읽을까요?") },
            text = {
                Text("카드·은행 결제 알림을 기기 안에서 읽어 후보로 모아요. 접근을 허용한 뒤에는 인식 결과를 알리기 위한 ‘모아씀 알림’ 권한도 한 번 확인해 주세요. 거래는 확인한 뒤에만 저장합니다.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationAccessPrompt = false
                    onMarkNotificationPromptShown()
                    NotificationAccess.openSettings(context)
                }) {
                    Text("설정 열기")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationAccessPrompt = false
                    onMarkNotificationPromptShown()
                }) {
                    Text("나중에")
                }
            },
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
