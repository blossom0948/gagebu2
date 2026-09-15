package com.moasseum.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.moasseum.app.domain.AiParseState
import com.moasseum.app.notification.NotificationAccess
import com.moasseum.app.update.AppUpdateManager
import com.moasseum.app.update.InstallResult
import com.moasseum.app.update.UpdateCheckState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as FinanceApplication
        setContent {
            val viewModel: LedgerViewModel = viewModel(
                factory = LedgerViewModel.Factory(application.financeRepository),
            )
            val darkTheme by application.preferencesRepository.isDarkTheme.collectAsStateWithLifecycle(initialValue = true)
            val reduceMotion by application.preferencesRepository.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
            MoasseumTheme(darkTheme = darkTheme, reduceMotion = reduceMotion) {
                UpdateSystemBars(darkTheme)
                MoasseumApp(
                    viewModel = viewModel,
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
                )
            }
        }
    }
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
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    notificationPromptShown: Boolean,
    onMarkNotificationPromptShown: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.date.collectAsStateWithLifecycle()
    val pendingCandidates by viewModel.pendingNotificationCandidates.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationAccessEnabled by remember { mutableStateOf(NotificationAccess.isEnabled(context)) }
    var showNotificationAccessPrompt by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessEnabled = NotificationAccess.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(notificationPromptShown, notificationAccessEnabled) {
        showNotificationAccessPrompt = !notificationPromptShown && !notificationAccessEnabled
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val aiClient = remember { AiClient() }
    var aiState by remember { mutableStateOf<AiParseState>(AiParseState.Idle) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var addModeName by rememberSaveable { mutableStateOf(AddMode.MENU.name) }
    var unavailableMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var updateState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    val addMode = AddMode.valueOf(addModeName)

    fun closeAdd() {
        addOpen = false
        addModeName = AddMode.MENU.name
        aiState = AiParseState.Idle
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
                        selectedDate = selectedDate,
                        onSelectDate = viewModel::selectDate,
                        onSelectMonth = viewModel::selectMonth,
                        onDeleteTransaction = { id ->
                            viewModel.deleteTransaction(id)
                            coroutineScope.launch { snackbarHostState.showSnackbar("거래를 삭제했어요") }
                        },
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
                        darkTheme = darkTheme,
                        reduceMotion = reduceMotion,
                        onDarkThemeChanged = onDarkThemeChanged,
                        onReduceMotionChanged = onReduceMotionChanged,
                        onUpdateBudget = viewModel::updateBudget,
                        onShowUnavailable = { feature -> unavailableMessage = "$feature 기능은 다음 단계에서 연결됩니다." },
                        notificationAccessEnabled = notificationAccessEnabled,
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
                        onInstallUpdate = { release ->
                            updateState = UpdateCheckState.Downloading(release)
                            coroutineScope.launch {
                                AppUpdateManager.downloadApk(context, release)
                                    .onSuccess { apkFile ->
                                        when (val result = AppUpdateManager.install(context, apkFile)) {
                                            InstallResult.Started -> {
                                                updateState = UpdateCheckState.Installing(release)
                                            }
                                            InstallResult.PermissionRequired -> {
                                                updateState = UpdateCheckState.WaitingForInstallPermission
                                            }
                                            is InstallResult.Failed -> {
                                                updateState = UpdateCheckState.Error(result.message)
                                            }
                                        }
                                    }
                                    .onFailure { error ->
                                        updateState = UpdateCheckState.Error(
                                            error.message ?: "APK 다운로드에 실패했어요.",
                                        )
                                    }
                            }
                        },
                        onOpenNotificationSettings = {
                            NotificationAccess.openSettings(context)
                        },
                        onAcceptNotificationCandidate = { id ->
                            viewModel.acceptNotificationCandidate(id)
                            coroutineScope.launch { snackbarHostState.showSnackbar("알림을 거래로 저장했어요") }
                        },
                        onDismissNotificationCandidate = { id ->
                            viewModel.dismissNotificationCandidate(id)
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
                onConfirmAi = { amount, type, merchant, categoryKey, memo, occurredAt ->
                    val saved = viewModel.addTransaction(amount, type, merchant, categoryKey, memo, occurredAt)
                    if (saved) {
                        closeAdd()
                        coroutineScope.launch { snackbarHostState.showSnackbar("AI 거래 후보를 저장했어요") }
                    }
                    saved
                },
                onSave = { amount, type, merchant, categoryKey, memo ->
                    val saved = viewModel.addTransaction(amount, type, merchant, categoryKey, memo)
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

    if (showNotificationAccessPrompt) {
        AlertDialog(
            onDismissRequest = {
                showNotificationAccessPrompt = false
                onMarkNotificationPromptShown()
            },
            title = { Text("결제 알림을 자동으로 읽을까요?") },
            text = {
                Text("카드·은행 결제 알림을 기기 안에서 읽어 거래 후보로 모아드려요. 자동 저장하지 않고 확인한 뒤에만 가계부에 넣습니다.")
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
