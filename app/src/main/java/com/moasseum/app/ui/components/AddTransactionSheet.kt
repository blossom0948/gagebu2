package com.moasseum.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChartOutline
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.moasseum.app.domain.parseQuickEntry
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.CurrencyConversion
import com.moasseum.app.domain.ExchangeRateQuote
import com.moasseum.app.domain.quickEntrySuggestions
import com.moasseum.app.domain.matchingQuickEntrySuggestions
import com.moasseum.app.data.PhotoImportState
import com.moasseum.app.data.PhotoTransactionCandidate
import com.moasseum.app.data.convertForeignAmountToKrw
import com.moasseum.app.data.currencyFractionDigits
import com.moasseum.app.data.parseForeignAmount
import com.moasseum.app.data.supportedForeignCurrencies
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

private fun currencyLabelForInput(code: String): String = when (code) {
    "KRW" -> "원"
    "USD" -> "미국 달러"
    "JPY" -> "일본 엔"
    "EUR" -> "유로"
    "CNY" -> "중국 위안"
    "GBP" -> "영국 파운드"
    "TWD" -> "대만 달러"
    "HKD" -> "홍콩 달러"
    "THB" -> "태국 바트"
    "VND" -> "베트남 동"
    "SGD" -> "싱가포르 달러"
    "AUD" -> "호주 달러"
    "CAD" -> "캐나다 달러"
    "CHF" -> "스위스 프랑"
    "NZD" -> "뉴질랜드 달러"
    "PHP" -> "필리핀 페소"
    "IDR" -> "인도네시아 루피아"
    "MYR" -> "말레이시아 링깃"
    "INR" -> "인도 루피"
    "AED" -> "아랍에미리트 디르함"
    "KWD" -> "쿠웨이트 디나르"
    else -> code
}

@Composable
fun AddTransactionSheet(
    mode: AddMode,
    initialDate: LocalDate = LocalDate.now(),
    initialType: TransactionType = TransactionType.EXPENSE,
    paymentMethods: List<String>,
    accounts: List<com.moasseum.app.domain.Account>,
    hiddenSuggestionKeys: Set<String> = emptySet(),
    onHideSuggestion: (String) -> Unit = {},
    onFetchExchangeRate: suspend (String, LocalDate) -> Result<ExchangeRateQuote> = { _, _ -> Result.failure(IllegalStateException("환율 정보를 불러올 수 없어요.")) },
    saving: Boolean = false,
    onModeChange: (AddMode) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType, String, String, String, String, String?, LocalDate, Int?, CurrencyConversion?) -> Boolean,
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
    onPickMorePhotos: (List<com.moasseum.app.data.PhotoTransactionCandidate>, Set<String>) -> Unit = { _, _ -> onPickReceipt() },
    onAddPhotoCandidates: (List<PhotoTransactionCandidate>) -> Unit = {},
    canShareOnSave: Boolean = false,
    shareOnSave: Boolean = false,
    onShareOnSaveChange: (Boolean) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(mode) { scrollState.scrollTo(0) }
    Column(Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        if (saving) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
        if (canShareOnSave && mode in setOf(AddMode.MENU, AddMode.DIRECT, AddMode.AI_INPUT, AddMode.PHOTO_REVIEW)) {
            SaveDestinationSelector(shareOnSave, onShareOnSaveChange)
        }
        when (mode) {
            AddMode.MENU -> AddMenu(
                paymentMethods = paymentMethods,
                saving = saving,
                onModeChange = onModeChange,
                onStartVoiceInput = onStartVoiceInput,
                speechResult = speechResult,
                onSpeechResultConsumed = onSpeechResultConsumed,
                onParseAi = onParseAi,
                onQuickSave = { input ->
                    val candidate = parseQuickEntry(input) ?: return@AddMenu false
                    val category = com.moasseum.app.notification.PaymentNotificationParser.inferCategoryFrom(candidate.merchant)
                    onSave(
                        candidate.amount.toString(), candidate.type, candidate.merchant, category, "",
                        paymentMethods.firstOrNull() ?: "카드", null, initialDate, null, null,
                    )
                },
                onPickReceipt = onPickReceipt,
                onTakeReceipt = onTakeReceipt,
            )
            AddMode.DIRECT -> DirectTransactionForm(
                initialDate = initialDate, initialType = initialType, paymentMethods = paymentMethods, accounts = accounts,
                transactions = transactions, hiddenSuggestionKeys = hiddenSuggestionKeys, onHideSuggestion = onHideSuggestion,
                onFetchExchangeRate = onFetchExchangeRate, saving = saving, onModeChange = onModeChange,
                onDismiss = onDismiss, onSave = onSave,
            )
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
                onPickMore = { candidates, unselectedIds -> onPickMorePhotos(candidates, unselectedIds) },
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
private fun AddMenu(
    paymentMethods: List<String>,
    saving: Boolean,
    onModeChange: (AddMode) -> Unit,
    onStartVoiceInput: () -> Unit,
    speechResult: String?,
    onSpeechResultConsumed: () -> Unit,
    onParseAi: (String) -> Unit,
    onQuickSave: (String) -> Boolean,
    onPickReceipt: () -> Unit,
    onTakeReceipt: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val focusRequester = remember { FocusRequester() }
    var aiMode by rememberSaveable { mutableStateOf(true) }
    var input by rememberSaveable { mutableStateOf("") }
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(speechResult) {
        if (!speechResult.isNullOrBlank()) {
            input = speechResult.take(240)
            aiMode = true
            error = null
            onSpeechResultConsumed()
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("빠른 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            FilterChip(selected = aiMode, onClick = { aiMode = true; error = null }, enabled = !saving, label = { Text("AI") }, leadingIcon = {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
            })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = !aiMode, onClick = { aiMode = false; error = null }, enabled = !saving, label = { Text("직접") }, leadingIcon = {
                Icon(Icons.Rounded.TouchApp, contentDescription = null, modifier = Modifier.size(15.dp))
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(240); error = null },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text(if (aiMode) "예: 점심 8천원" else "가게 이름과 금액 · 예: 점심 8천원") },
                singleLine = true,
                enabled = !saving,
                trailingIcon = if (!aiMode) ({
                    TextButton(enabled = !saving && input.isNotBlank(), onClick = {
                        if (onQuickSave(input)) { input = ""; error = null } else error = "가게 이름과 금액을 확인해 주세요."
                    }) { Text("저장", fontWeight = FontWeight.Bold, color = colors.accent) }
                }) else null,
                shape = RoundedCornerShape(15.dp),
            )
            if (aiMode) {
                Button(
                    enabled = !saving && input.isNotBlank(),
                    onClick = { onParseAi(input); onModeChange(AddMode.AI_INPUT) },
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 11.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("분석", fontWeight = FontWeight.Bold) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(enabled = !saving, onClick = onStartVoiceInput) {
                Icon(Icons.Rounded.KeyboardVoice, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("음성")
            }
            TextButton(enabled = !saving, onClick = onTakeReceipt) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("카메라")
            }
            TextButton(enabled = !saving, onClick = onPickReceipt) {
                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("사진")
            }
            Spacer(Modifier.weight(1f))
            TextButton(enabled = !saving, onClick = { moreOpen = !moreOpen }) {
                Text(if (moreOpen) "접기" else "더 보기")
            }
        }
        if (error != null) Text(error!!, color = colors.expense, style = MaterialTheme.typography.labelMedium)
        if (moreOpen) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !saving, onClick = { onModeChange(AddMode.DIRECT) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("상세 입력")
                }
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !saving, onClick = { onModeChange(AddMode.BATCH) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("여러 건 처리")
                }
            }
            Text("직접 입력은 이 기기 안에서 처리됩니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
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
            candidate.paymentMethod.takeIf { it in paymentMethods }?.let { paymentMethod = it }
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
        Text("문장과 날짜 해석에 필요한 오늘 날짜·시간대를 AI 서버로 보내요. 저장 전에 내용을 확인할 수 있어요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
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
            val confirmationLabels = candidate.needsConfirmation.joinToString(" · ") { field ->
                when (field) {
                    "amount" -> "금액"
                    "date" -> "날짜"
                    "category" -> "분류"
                    "merchant" -> "가맹점"
                    "paymentMethod" -> "결제수단"
                    else -> field
                }
            }
            Text("확인 필요 · $confirmationLabels", color = colors.warning, style = MaterialTheme.typography.labelMedium)
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
    initialDate: LocalDate,
    initialType: TransactionType,
    paymentMethods: List<String>,
    accounts: List<com.moasseum.app.domain.Account>,
    transactions: List<Transaction>,
    hiddenSuggestionKeys: Set<String>,
    onHideSuggestion: (String) -> Unit,
    onFetchExchangeRate: suspend (String, LocalDate) -> Result<ExchangeRateQuote>,
    saving: Boolean,
    onModeChange: (AddMode) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType, String, String, String, String, String?, LocalDate, Int?, CurrencyConversion?) -> Boolean,
) {
    val colors = LocalFinanceColors.current
    var typeName by rememberSaveable(initialType.name) { mutableStateOf(initialType.name) }
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var categoryKey by rememberSaveable(initialType.name) {
        mutableStateOf(if (initialType == TransactionType.INCOME) "OTHER" else "FOOD")
    }
    var paymentMethod by rememberSaveable { mutableStateOf(paymentMethods.firstOrNull().orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    var occurredDateText by rememberSaveable(initialDate.toString()) { mutableStateOf(initialDate.toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var isInstallment by rememberSaveable { mutableStateOf(false) }
    var installmentCountText by rememberSaveable { mutableStateOf("3") }
    var accountId by rememberSaveable { mutableStateOf<String?>(null) }
    var currencyCode by rememberSaveable { mutableStateOf("KRW") }
    var currencyMenuOpen by rememberSaveable { mutableStateOf(false) }
    var rateQuote by remember { mutableStateOf<ExchangeRateQuote?>(null) }
    var rateLoading by remember { mutableStateOf(false) }
    var rateError by remember { mutableStateOf<String?>(null) }
    var rateReloadKey by rememberSaveable { mutableIntStateOf(0) }
    var manageSuggestions by rememberSaveable { mutableStateOf(false) }
    val suggestions = remember(transactions, hiddenSuggestionKeys) {
        quickEntrySuggestions(transactions, hiddenSuggestionKeys)
    }
    val merchantMatches = remember(transactions, merchant, type) {
        matchingQuickEntrySuggestions(transactions, merchant, type)
    }

    LaunchedEffect(currencyCode, occurredDateText, rateReloadKey) {
        if (currencyCode == "KRW") {
            rateQuote = null
            rateError = null
            rateLoading = false
            return@LaunchedEffect
        }
        val date = runCatching { LocalDate.parse(occurredDateText) }.getOrNull()
        if (date == null) {
            rateQuote = null
            rateError = "거래일을 확인해 주세요."
            return@LaunchedEffect
        }
        rateLoading = true
        rateQuote = null
        rateError = null
        val result = onFetchExchangeRate(currencyCode, date)
        rateQuote = result.getOrNull()
        rateError = result.exceptionOrNull()?.message
        rateLoading = false
    }

    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("거래 기록", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(enabled = !saving, onClick = { onModeChange(AddMode.MENU) }) { Text("방법 바꾸기") }
        }
        if (suggestions.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("최근 내역", modifier = Modifier.weight(1f), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                TextButton(enabled = !saving, onClick = { manageSuggestions = !manageSuggestions }) {
                    Text(if (manageSuggestions) "완료" else "편집")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                suggestions.forEach { suggestion ->
                    Surface(color = colors.surfaceOverlay, shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier
                                .then(if (!manageSuggestions) Modifier.clickable {
                                    typeName = suggestion.type.name
                                    amount = suggestion.originalAmount ?: suggestion.amount.toString()
                                    merchant = suggestion.merchant
                                    categoryKey = suggestion.categoryKey
                                    paymentMethod = suggestion.paymentMethod
                                    currencyCode = suggestion.currency
                                    isInstallment = false
                                    showError = false
                                } else Modifier)
                                .padding(start = 10.dp, end = if (manageSuggestions) 0.dp else 10.dp, top = 7.dp, bottom = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Column {
                                Text(suggestion.merchant, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                Text("${suggestion.currency} ${suggestion.originalAmount ?: suggestion.amount}", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                            if (manageSuggestions) {
                                IconButton(onClick = { onHideSuggestion(suggestion.key) }, enabled = !saving, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "${suggestion.merchant} 추천 삭제", modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { currencyMenuOpen = true }, enabled = !saving) { Text(currencyCode) }
                DropdownMenu(expanded = currencyMenuOpen, onDismissRequest = { currencyMenuOpen = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                    (listOf("KRW") + supportedForeignCurrencies).forEach { code ->
                        DropdownMenuItem(
                            text = { Text("$code · ${currencyLabelForInput(code)}") },
                            onClick = {
                                currencyCode = code
                                currencyMenuOpen = false
                                if (code != "KRW") isInstallment = false
                                showError = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    val allowed = if (currencyCode == "KRW") input.all { it.isDigit() || it in ", " } else input.all { it.isDigit() || it in ", ." }
                    if (allowed) amount = input.take(24)
                    showError = false
                },
                modifier = Modifier.weight(1f),
                label = { Text("금액") },
                placeholder = { Text(if (currencyCode == "KRW") "0" else "0.${"0".repeat(currencyFractionDigits(currencyCode))}") },
                suffix = { Text(currencyCode) },
                singleLine = true,
                isError = showError && amount.isBlank(),
                keyboardOptions = KeyboardOptions(keyboardType = if (currencyCode == "KRW") KeyboardType.Number else KeyboardType.Decimal),
                shape = RoundedCornerShape(15.dp),
            )
        }
        if (currencyCode != "KRW") {
            val foreignAmount = parseForeignAmount(amount, currencyCode)
            val convertedAmount = foreignAmount?.let { value -> rateQuote?.let { convertForeignAmountToKrw(value, it) } }
            when {
                rateLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("거래일 환율 확인 중", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
                rateQuote != null -> {
                    val conversionLabel = when {
                        amount.isBlank() -> "금액을 입력하면 환산액이 표시돼요"
                        foreignAmount == null -> "금액 형식을 확인해 주세요"
                        convertedAmount == null -> "금액 범위를 초과했어요"
                        else -> "환산 ${formatWon(convertedAmount)}"
                    }
                    Text("$conversionLabel · 1 ${rateQuote!!.baseCurrency} = ${rateQuote!!.rate}원", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${rateQuote!!.date} 기준 일일 참고 환율 · 카드사 청구 환율과 수수료에 따라 실제 결제액은 다를 수 있어요.", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rateError ?: "환율을 불러올 수 없어요.", modifier = Modifier.weight(1f), color = colors.expense, style = MaterialTheme.typography.labelSmall)
                    TextButton(enabled = !saving, onClick = { rateReloadKey++ }) { Text("다시 시도") }
                }
            }
        }
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
        if (!manageSuggestions && merchantMatches.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("이전에 입력한 내역", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    merchantMatches.forEach { match ->
                        Surface(
                            modifier = Modifier.clickable {
                                typeName = match.type.name
                                amount = match.originalAmount ?: match.amount.toString()
                                merchant = match.merchant
                                categoryKey = match.categoryKey
                                paymentMethod = match.paymentMethod
                                currencyCode = match.currency
                                isInstallment = false
                                showError = false
                            },
                            color = colors.surfaceOverlay,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    if (match.currency == "KRW") formatWon(match.amount) else "${match.currency} ${match.originalAmount.orEmpty()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                )
                                Text(
                                    "${categoryLabel(match.categoryKey)} · ${match.paymentMethod}",
                                    color = colors.textSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
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
        if (type == TransactionType.EXPENSE && currencyCode == "KRW") {
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
                val parsedForeign = if (currencyCode == "KRW") null else parseForeignAmount(amount, currencyCode)
                val wonAmount = if (currencyCode == "KRW") parseAmount(amount) else parsedForeign?.let { value -> rateQuote?.let { convertForeignAmountToKrw(value, it) } }
                val currencyConversion = if (currencyCode == "KRW" || parsedForeign == null || rateQuote == null) null else CurrencyConversion(
                    currencyCode = currencyCode,
                    originalAmount = parsedForeign.stripTrailingZeros().toPlainString(),
                    rateToKrw = rateQuote!!.rate,
                    rateDate = rateQuote!!.date,
                )
                val occurredDate = runCatching { LocalDate.parse(occurredDateText) }.getOrNull()
                val count = installmentCountText.toIntOrNull()
                val invalidInstallment = isInstallment && (count == null || count !in 2..60 || wonAmount == null || wonAmount < count)
                if (wonAmount == null || merchant.isBlank() || occurredDate == null || invalidInstallment ||
                    !onSave(wonAmount.toString(), type, merchant, categoryKey, memo, paymentMethod, accountId, occurredDate, count.takeIf { isInstallment }, currencyConversion)
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
