package com.moasseum.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChartOutline
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberDatePickerState
import com.moasseum.app.ui.components.FinanceTextField as OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.AiParseState
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatWon
import com.moasseum.app.domain.parseAmount
import com.moasseum.app.domain.Transaction
import com.moasseum.app.data.PhotoImportState
import com.moasseum.app.data.PhotoTransactionCandidate
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.theme.LocalFinanceColors
import java.time.LocalDate
import java.time.ZoneOffset

enum class AddMode {
    MENU,
    DIRECT,
    AI_INPUT,
    BATCH,
    AI_NOTICE,
    RECEIPT_NOTICE,
    PHOTO_REVIEW,
}

@Composable
fun AddTransactionSheet(
    mode: AddMode,
    paymentMethods: List<String>,
    accounts: List<com.moasseum.app.domain.Account>,
    saving: Boolean = false,
    onModeChange: (AddMode) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType, String, String, String, String, String?, LocalDate, Int?) -> Boolean,
    aiState: AiParseState = AiParseState.Idle,
    onParseAi: (String) -> Unit = {},
    onConfirmAi: (String, TransactionType, String, String, String, String, java.time.LocalDate) -> Boolean = { _, _, _, _, _, _, _ -> false },
    onStartVoiceInput: () -> Unit = {},
    onPickReceipt: () -> Unit = {},
    onTakeReceipt: () -> Unit = {},
    speechResult: String? = null,
    onSpeechResultConsumed: () -> Unit = {},
    prefillText: String? = null,
    prefillKey: String = "",
    transactions: List<Transaction> = emptyList(),
    onAddBatch: (List<AiTransactionCandidate>, String) -> Boolean = { _, _ -> false },
    onParseBatchWithAi: suspend (String) -> Result<com.moasseum.app.data.BatchCommandPlan> = {
        Result.failure(IllegalStateException("AI 일괄 해석을 사용할 수 없어요."))
    },
    onApplyBatch: (com.moasseum.app.data.BatchCommandAction, List<Long>, com.moasseum.app.data.BatchEditValues?) -> Boolean = { _, _, _ -> false },
    photoImportState: PhotoImportState = PhotoImportState.Idle,
    onPickMorePhotos: (Set<String>) -> Unit = { onPickReceipt() },
    onAddPhotoCandidates: (List<PhotoTransactionCandidate>) -> Unit = {},
    canShareOnSave: Boolean = false,
    shareOnSave: Boolean = false,
    onShareOnSaveChange: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(mode) { scrollState.scrollTo(0) }
    Column(Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        if (saving) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
        if (canShareOnSave && mode in setOf(AddMode.DIRECT, AddMode.AI_INPUT, AddMode.PHOTO_REVIEW)) {
            SaveDestinationSelector(shareOnSave, onShareOnSaveChange)
        }
        when (mode) {
            AddMode.MENU -> AddMenu(onModeChange = onModeChange, onStartVoiceInput = onStartVoiceInput)
            AddMode.DIRECT -> DirectTransactionForm(paymentMethods = paymentMethods, accounts = accounts, saving = saving, onModeChange = onModeChange, onDismiss = onDismiss, onSave = onSave)
            AddMode.AI_INPUT, AddMode.AI_NOTICE -> AiInputForm(
                paymentMethods = paymentMethods,
                aiState = aiState,
                saving = saving,
                onModeChange = onModeChange,
                onParseAi = onParseAi,
                onConfirm = onConfirmAi,
                onStartVoiceInput = onStartVoiceInput,
                speechResult = speechResult,
                onSpeechResultConsumed = onSpeechResultConsumed,
                prefillText = prefillText,
                prefillKey = prefillKey,
            )
            AddMode.RECEIPT_NOTICE -> ReceiptInputNotice(
                aiState = aiState,
                onPickReceipt = onPickReceipt,
                onTakeReceipt = onTakeReceipt,
                onBack = { onModeChange(AddMode.MENU) },
            )
            AddMode.PHOTO_REVIEW -> PhotoImportReview(
                state = photoImportState,
                saving = saving,
                onPickMore = { unselectedIds -> onPickMorePhotos(unselectedIds) },
                onSave = onAddPhotoCandidates,
                onCancel = onDismiss,
            )
            AddMode.BATCH -> BatchCommandSheet(
                transactions = transactions,
                paymentMethods = paymentMethods,
                saving = saving,
                onDismiss = onDismiss,
                onAdd = onAddBatch,
                onParseWithAi = onParseBatchWithAi,
                onApply = onApplyBatch,
            )
        }
    }
}

@Composable
private fun SaveDestinationSelector(
    shareOnSave: Boolean,
    onSelectShared: (Boolean) -> Unit,
) {
    val colors = LocalFinanceColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("저장 위치", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !shareOnSave, onClick = { onSelectShared(false) }, label = { Text("개인") })
            FilterChip(selected = shareOnSave, onClick = { onSelectShared(true) }, label = { Text("함께 공유") })
        }
        if (shareOnSave) Text("저장 후 이 거래가 파트너와 공유됩니다.", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
private fun AddMenu(onModeChange: (AddMode) -> Unit, onStartVoiceInput: () -> Unit) {
    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("새 기록", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        AddActionRow(
            icon = Icons.Rounded.TouchApp,
            title = "직접 입력",
            message = "",
            onClick = { onModeChange(AddMode.DIRECT) },
        )
        AddActionRow(
            icon = Icons.Rounded.AutoAwesome,
            title = "AI 문장으로 입력",
            message = "예: 점심 8천원",
            onClick = { onModeChange(AddMode.AI_INPUT) },
        )
        AddActionRow(
            icon = Icons.Rounded.Edit,
            title = "문장으로 여러 건 처리",
            message = "추가 · 수정 · 삭제",
            onClick = { onModeChange(AddMode.BATCH) },
        )
        AddActionRow(
            icon = Icons.Rounded.KeyboardVoice,
            title = "음성으로 입력",
            message = "",
            onClick = onStartVoiceInput,
        )
        AddActionRow(
            icon = Icons.Rounded.CameraAlt,
            title = "사진에서 거래 가져오기",
            message = "",
            onClick = { onModeChange(AddMode.RECEIPT_NOTICE) },
        )
    }
}

@Composable
private fun AiInputForm(
    paymentMethods: List<String>,
    aiState: AiParseState,
    saving: Boolean,
    onModeChange: (AddMode) -> Unit,
    onParseAi: (String) -> Unit,
    onConfirm: (String, TransactionType, String, String, String, String, java.time.LocalDate) -> Boolean,
    onStartVoiceInput: () -> Unit,
    speechResult: String?,
    onSpeechResultConsumed: () -> Unit,
    prefillText: String?,
    prefillKey: String,
) {
    val colors = LocalFinanceColors.current
    val focusManager = LocalFocusManager.current
    var input by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var categoryKey by rememberSaveable { mutableStateOf("OTHER") }
    var typeName by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var paymentMethod by rememberSaveable { mutableStateOf(paymentMethods.firstOrNull().orEmpty()) }
    var showConfirmError by rememberSaveable { mutableStateOf(false) }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val candidate = (aiState as? AiParseState.Success)?.candidate

    LaunchedEffect(candidate) {
        if (candidate != null) {
            amount = candidate.amount.toString()
            merchant = candidate.merchant
            memo = candidate.memo
            categoryKey = candidate.categoryKey
            typeName = candidate.type.name
            dateText = candidate.occurredDate.toString()
            if (paymentMethod !in paymentMethods) paymentMethod = paymentMethods.firstOrNull().orEmpty()
            showConfirmError = false
        }
    }

    LaunchedEffect(speechResult) {
        if (!speechResult.isNullOrBlank()) {
            input = speechResult
            showConfirmError = false
            onSpeechResultConsumed()
        }
    }

    LaunchedEffect(prefillKey) {
        if (!prefillText.isNullOrBlank()) {
            input = prefillText.take(4000)
            showConfirmError = false
        }
    }

    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AI로 빠르게 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(enabled = !saving, onClick = { onModeChange(AddMode.MENU) }) { Text("방법 바꾸기") }
        }
        Text("입력 문장을 AI 서버로 전송합니다. 저장 전 확인할 수 있습니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; showConfirmError = false },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            singleLine = true,
            placeholder = { Text("예: 어제 친구랑 치킨 24000원") },
            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent) },
            trailingIcon = {
                IconButton(enabled = !saving, onClick = onStartVoiceInput) {
                    Icon(Icons.Rounded.KeyboardVoice, contentDescription = "음성으로 입력", tint = colors.accent)
                }
            },
            shape = RoundedCornerShape(15.dp),
        )
        Button(
            onClick = { focusManager.clearFocus(); onParseAi(input) },
            enabled = !saving && input.isNotBlank() && aiState !is AiParseState.Loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(15.dp),
        ) {
            if (aiState is AiParseState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("거래 후보 해석하기", fontWeight = FontWeight.Bold)
            }
        }
        when (aiState) {
            AiParseState.Idle -> Unit

            AiParseState.Loading -> {
                Text("거래 후보를 만드는 중이에요…", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }

            is AiParseState.Error -> {
                Surface(color = colors.expense.copy(alpha = 0.12f), shape = RoundedCornerShape(13.dp)) {
                    Text(aiState.message, color = colors.expense, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                }
            }

            is AiParseState.Success -> {
                CandidateReview(
                    candidate = candidate!!,
                    saving = saving,
                    amount = amount,
                    merchant = merchant,
                    memo = memo,
                    categoryKey = categoryKey,
                    type = TransactionType.valueOf(typeName),
                    paymentMethods = paymentMethods,
                    paymentMethod = paymentMethod,
                    onAmountChange = { amount = it.take(24); showConfirmError = false },
                    onMerchantChange = { merchant = it; showConfirmError = false },
                    onMemoChange = { memo = it },
                    onCategoryChange = { categoryKey = it },
                    onTypeChange = { typeName = it.name },
                    onPaymentMethodChange = { paymentMethod = it },
                    showError = showConfirmError,
                    dateText = dateText,
                    onDateChange = { dateText = it; showConfirmError = false },
                    onConfirm = {
                        val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                        if (date == null || !onConfirm(amount, TransactionType.valueOf(typeName), merchant, categoryKey, memo, paymentMethod, date)) {
                            showConfirmError = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CandidateReview(
    candidate: AiTransactionCandidate,
    saving: Boolean,
    amount: String,
    merchant: String,
    memo: String,
    categoryKey: String,
    type: TransactionType,
    paymentMethods: List<String>,
    paymentMethod: String,
    onAmountChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    showError: Boolean,
    dateText: String,
    onDateChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("확인할 거래 후보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (candidate.source.name == "SERVER") "서버 AI가 구조화했어요" else "기기 안에서 해석했어요",
                    color = colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            TextButton(onClick = { onTypeChange(if (type == TransactionType.EXPENSE) TransactionType.INCOME else TransactionType.EXPENSE) }) {
                Text(if (type == TransactionType.EXPENSE) "지출" else "수입")
            }
        }
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("금액") },
            suffix = { Text("원") },
            singleLine = true,
            isError = showError && parseAmount(amount) == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(15.dp),
        )
        OutlinedTextField(
            value = merchant,
            onValueChange = onMerchantChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("가맹점") },
            singleLine = true,
            isError = showError && merchant.isBlank(),
            shape = RoundedCornerShape(15.dp),
        )
        OutlinedTextField(
            value = dateText,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("거래일 (YYYY-MM-DD)") },
            singleLine = true,
            isError = showError && runCatching { LocalDate.parse(dateText) }.isFailure,
            shape = RoundedCornerShape(15.dp),
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            allCategorySpecs().forEach { spec ->
                FilterChip(
                    selected = categoryKey == spec.key,
                    onClick = { onCategoryChange(spec.key) },
                    label = { Text(categoryLabel(spec.key)) },
                    leadingIcon = { Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(15.dp)) },
                )
            }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            paymentMethods.forEach { method ->
                FilterChip(selected = paymentMethod == method, onClick = { onPaymentMethodChange(method) }, label = { Text(method) })
            }
        }
        OutlinedTextField(
            value = memo,
            onValueChange = onMemoChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("메모") },
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
        )
        if (candidate.needsConfirmation.isNotEmpty()) {
            Text("카테고리나 날짜를 한 번 확인해 주세요.", color = colors.warning, style = MaterialTheme.typography.labelMedium)
        }
        if (showError) Text("금액, 가맹점, 거래일을 확인해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        Button(
            onClick = onConfirm,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(15.dp),
        ) { Text("확인하고 저장", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AddActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onClick: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Surface(
        onClick = onClick,
        color = colors.surfaceRaised,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = colors.accentSoft, shape = RoundedCornerShape(13.dp)) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (message.isNotBlank()) Text(message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textSecondary)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DirectTransactionForm(
    paymentMethods: List<String>,
    accounts: List<com.moasseum.app.domain.Account>,
    saving: Boolean,
    onModeChange: (AddMode) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType, String, String, String, String, String?, LocalDate, Int?) -> Boolean,
) {
    val colors = LocalFinanceColors.current
    var typeName by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var categoryKey by rememberSaveable { mutableStateOf("FOOD") }
    var paymentMethod by rememberSaveable { mutableStateOf(paymentMethods.firstOrNull().orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    var occurredDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var isInstallment by rememberSaveable { mutableStateOf(false) }
    var installmentCountText by rememberSaveable { mutableStateOf("3") }
    var accountId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("거래 기록", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(enabled = !saving, onClick = { onModeChange(AddMode.MENU) }) { Text("방법 바꾸기") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = type == TransactionType.EXPENSE,
                onClick = { typeName = TransactionType.EXPENSE.name },
                label = { Text("지출") },
                leadingIcon = { Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = type == TransactionType.INCOME,
                onClick = { typeName = TransactionType.INCOME.name; isInstallment = false },
                label = { Text("수입") },
                leadingIcon = { Icon(Icons.Rounded.PieChartOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.take(24); showError = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("금액") },
            placeholder = { Text("0") },
            suffix = { Text("원") },
            singleLine = true,
            isError = showError && amount.isBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(15.dp),
        )
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it; showError = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("가맹점") },
            placeholder = { Text("어디에 썼나요?") },
            singleLine = true,
            isError = showError && merchant.isBlank(),
            shape = RoundedCornerShape(15.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("카테고리", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                allCategorySpecs().forEach { spec ->
                    FilterChip(
                        selected = categoryKey == spec.key,
                        onClick = { categoryKey = spec.key },
                        label = { Text(categoryLabel(spec.key)) },
                        leadingIcon = { Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(15.dp)) },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("결제수단", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                paymentMethods.forEach { method ->
                    FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method }, label = { Text(method) })
                }
            }
        }
        if (type == TransactionType.EXPENSE) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("할부로 기록", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("월별 납부액으로 나눠 등록", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked = isInstallment,
                    onCheckedChange = {
                        isInstallment = it
                        if (it) accountId = null
                        showError = false
                    },
                    enabled = !saving,
                )
            }
            if (isInstallment) {
                OutlinedTextField(
                    value = installmentCountText,
                    onValueChange = { installmentCountText = it.filter(Char::isDigit).take(2); showError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("할부 개월") },
                    suffix = { Text("개월 · 2~60") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    shape = RoundedCornerShape(15.dp),
                )
                Text("총액은 회차별로 나뉘며 합계는 원 단위까지 맞춰져요.", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (accounts.any { !it.archived } && !isInstallment) com.moasseum.app.ui.screens.AccountChips("잔액에 반영할 계좌 (선택)", accounts.filterNot { it.archived }, accountId, true) { accountId = it }
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("메모 (선택)") },
            placeholder = { Text("함께한 사람이나 기억할 내용을 적어보세요") },
            minLines = 1,
            maxLines = 2,
            shape = RoundedCornerShape(15.dp),
        )
        Surface(onClick = { showDatePicker = true }, color = colors.surfaceOverlay, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("거래일 · ${runCatching { formatDate(LocalDate.parse(occurredDateText)) }.getOrDefault(occurredDateText)}", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("변경", color = colors.accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (showError) {
            Text("금액·가맹점·날짜 또는 할부 개월을 확인해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }
        Button(
            onClick = {
                val parsedAmount = parseAmount(amount)
                val occurredDate = runCatching { LocalDate.parse(occurredDateText) }.getOrNull()
                val count = installmentCountText.toIntOrNull()
                val invalidInstallment = isInstallment && (count == null || count !in 2..60 || parsedAmount == null || parsedAmount < count)
                if (parsedAmount == null || merchant.isBlank() || occurredDate == null || invalidInstallment ||
                    !onSave(amount, type, merchant, categoryKey, memo, paymentMethod, accountId, occurredDate, count.takeIf { isInstallment })
                ) showError = true
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(15.dp),
        ) {
            Text("거래 저장", fontWeight = FontWeight.Bold)
        }
        TextButton(enabled = !saving, onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("취소") }
    }

    if (showDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(occurredDateText).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        occurredDateText = java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("선택") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun ReceiptInputNotice(
    aiState: AiParseState,
    onPickReceipt: () -> Unit,
    onTakeReceipt: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(color = colors.accentSoft, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = colors.accent, modifier = Modifier.padding(14.dp).size(26.dp))
        }
        Text("사진에서 거래 가져오기", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("영수증이나 금융앱 내역을 여러 장 선택할 수 있어요. 사진은 기기에서만 처리됩니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        when (aiState) {
            AiParseState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("사진을 기기 안에서 읽고 있어요…", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            is AiParseState.Error -> Text(aiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            else -> Unit
        }
        OutlinedButton(onClick = onPickReceipt, enabled = aiState !is AiParseState.Loading, modifier = Modifier.fillMaxWidth()) {
            Text("앨범에서 여러 장 선택")
        }
        OutlinedButton(onClick = onTakeReceipt, enabled = aiState !is AiParseState.Loading, modifier = Modifier.fillMaxWidth()) { Text("카메라로 촬영") }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("뒤로") }
    }
}
