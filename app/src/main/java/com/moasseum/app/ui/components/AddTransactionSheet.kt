package com.moasseum.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChartOutline
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.moasseum.app.ui.theme.LocalFinanceColors
import java.time.LocalDate

enum class AddMode {
    MENU,
    DIRECT,
    AI_INPUT,
    AI_NOTICE,
    RECEIPT_NOTICE,
}

@Composable
fun AddTransactionSheet(
    mode: AddMode,
    onModeChange: (AddMode) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType, String, String, String) -> Boolean,
    aiState: AiParseState = AiParseState.Idle,
    onParseAi: (String) -> Unit = {},
    onConfirmAi: (String, TransactionType, String, String, String, java.time.LocalDate) -> Boolean = { _, _, _, _, _, _ -> false },
) {
    when (mode) {
        AddMode.MENU -> AddMenu(onModeChange = onModeChange)
        AddMode.DIRECT -> DirectTransactionForm(onModeChange = onModeChange, onDismiss = onDismiss, onSave = onSave)
        AddMode.AI_INPUT -> AiInputForm(
            aiState = aiState,
            onModeChange = onModeChange,
            onParseAi = onParseAi,
            onConfirm = onConfirmAi,
        )
        AddMode.AI_NOTICE -> FeatureNotice(
            icon = Icons.Rounded.AutoAwesome,
            title = "AI 문장 입력은 준비 중이에요",
            message = "서버 주소와 AI 키가 연결되기 전까지는 거래를 자동으로 해석하지 않아요. 지금은 직접 입력으로 안전하게 기록할 수 있습니다.",
            onBack = { onModeChange(AddMode.MENU) },
        )
        AddMode.RECEIPT_NOTICE -> FeatureNotice(
            icon = Icons.Rounded.CameraAlt,
            title = "영수증 인식은 준비 중이에요",
            message = "카메라 권한과 로컬 OCR 검토 화면은 다음 단계에서 연결됩니다. 원본 이미지가 서버로 전송되는 일은 아직 없어요.",
            onBack = { onModeChange(AddMode.MENU) },
        )
    }
}

@Composable
private fun AddMenu(onModeChange: (AddMode) -> Unit) {
    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("새 기록", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("남기는 방법을 골라주세요.", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(2.dp))
        AddActionRow(
            icon = Icons.Rounded.TouchApp,
            title = "직접 입력",
            message = "금액과 카테고리를 바로 선택해요.",
            onClick = { onModeChange(AddMode.DIRECT) },
        )
        AddActionRow(
            icon = Icons.Rounded.AutoAwesome,
            title = "AI 문장으로 입력",
            message = "‘어제 점심 8천원’처럼 적어요.",
            onClick = { onModeChange(AddMode.AI_INPUT) },
        )
        AddActionRow(
            icon = Icons.Rounded.KeyboardVoice,
            title = "음성으로 입력",
            message = "말한 내용을 거래 후보로 만들어요.",
            onClick = { onModeChange(AddMode.AI_NOTICE) },
        )
        AddActionRow(
            icon = Icons.Rounded.CameraAlt,
            title = "영수증 가져오기",
            message = "촬영 후 금액을 직접 확인해요.",
            onClick = { onModeChange(AddMode.RECEIPT_NOTICE) },
        )
    }
}

@Composable
private fun AddModePill(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val colors = LocalFinanceColors.current
    Surface(
        modifier = Modifier.height(32.dp),
        onClick = onClick,
        color = if (selected) colors.accent else colors.surfaceRaised,
        contentColor = if (selected) Color(0xFF06332B) else colors.textSecondary,
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AiInputForm(
    aiState: AiParseState,
    onModeChange: (AddMode) -> Unit,
    onParseAi: (String) -> Unit,
    onConfirm: (String, TransactionType, String, String, String, java.time.LocalDate) -> Boolean,
) {
    val colors = LocalFinanceColors.current
    var input by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var categoryKey by rememberSaveable { mutableStateOf("OTHER") }
    var typeName by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var showConfirmError by rememberSaveable { mutableStateOf(false) }
    val candidate = (aiState as? AiParseState.Success)?.candidate

    LaunchedEffect(candidate) {
        if (candidate != null) {
            amount = candidate.amount.toString()
            merchant = candidate.merchant
            memo = candidate.memo
            categoryKey = candidate.categoryKey
            typeName = candidate.type.name
            showConfirmError = false
        }
    }

    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AI로 빠르게 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { onModeChange(AddMode.MENU) }) { Text("방법 바꾸기") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AddModePill(label = "개인")
            AddModePill(label = "AI", selected = true)
            AddModePill(label = "직접") { onModeChange(AddMode.DIRECT) }
        }
        Text("문장을 보내면 거래 후보를 만들고, 확인한 뒤에만 저장해요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; showConfirmError = false },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            placeholder = { Text("예: 어제 친구랑 치킨 24000원") },
            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent) },
            shape = RoundedCornerShape(15.dp),
        )
        Button(
            onClick = { onParseAi(input) },
            enabled = input.isNotBlank() && aiState !is AiParseState.Loading,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF06332B)),
            shape = RoundedCornerShape(15.dp),
        ) {
            if (aiState is AiParseState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF06332B), strokeWidth = 2.dp)
            } else {
                Text("거래 후보 해석하기", fontWeight = FontWeight.Bold)
            }
        }
        when (aiState) {
            AiParseState.Idle -> {
                Surface(color = colors.surfaceOverlay, shape = RoundedCornerShape(13.dp)) {
                    Text(
                        "AI API 주소가 없으면 기기 안의 안전한 기본 파서가 작동해요. 서버 AI를 쓰려면 별도 Worker 주소를 설정하세요.",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

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
                    amount = amount,
                    merchant = merchant,
                    memo = memo,
                    categoryKey = categoryKey,
                    type = TransactionType.valueOf(typeName),
                    onAmountChange = { amount = it.filter(Char::isDigit); showConfirmError = false },
                    onMerchantChange = { merchant = it; showConfirmError = false },
                    onMemoChange = { memo = it },
                    onCategoryChange = { categoryKey = it },
                    onTypeChange = { typeName = it.name },
                    showError = showConfirmError,
                    onConfirm = {
                        if (!onConfirm(amount, TransactionType.valueOf(typeName), merchant, categoryKey, memo, candidate.occurredDate)) {
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
    amount: String,
    merchant: String,
    memo: String,
    categoryKey: String,
    type: TransactionType,
    onAmountChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    showError: Boolean,
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
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = colors.surfaceOverlay, shape = RoundedCornerShape(11.dp)) {
                Text("${formatDate(candidate.occurredDate)}", color = colors.textSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            }
            Text("신뢰도 ${((candidate.amountConfidence + candidate.dateConfidence + candidate.categoryConfidence) / 3 * 100).toInt()}%", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CategorySpecs.forEach { spec ->
                FilterChip(
                    selected = categoryKey == spec.key,
                    onClick = { onCategoryChange(spec.key) },
                    label = { Text(spec.label) },
                    leadingIcon = { Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(15.dp)) },
                )
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
        if (showError) Text("금액과 가맹점을 확인해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF06332B)),
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
                Text(message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textSecondary)
        }
    }
}

@Composable
private fun DirectTransactionForm(
    onModeChange: (AddMode) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType, String, String, String) -> Boolean,
) {
    val colors = LocalFinanceColors.current
    var typeName by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var categoryKey by rememberSaveable { mutableStateOf("FOOD") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    val today = LocalDate.now()

    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("거래 기록", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { onModeChange(AddMode.MENU) }) { Text("방법 바꾸기") }
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
                onClick = { typeName = TransactionType.INCOME.name },
                label = { Text("수입") },
                leadingIcon = { Icon(Icons.Rounded.PieChartOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter(Char::isDigit); showError = false },
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
                CategorySpecs.forEach { spec ->
                    FilterChip(
                        selected = categoryKey == spec.key,
                        onClick = { categoryKey = spec.key },
                        label = { Text(spec.label) },
                        leadingIcon = { Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(15.dp)) },
                    )
                }
            }
        }
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
        Surface(color = colors.surfaceOverlay, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("거래일 · ${formatDate(today)}", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (showError) {
            Text("금액과 가맹점을 입력해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }
        Button(
            onClick = {
                if (!onSave(amount, type, merchant, categoryKey, memo)) showError = true
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF06332B)),
            shape = RoundedCornerShape(15.dp),
        ) {
            Text("거래 저장", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("취소") }
    }
}

@Composable
private fun FeatureNotice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onBack: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(color = colors.accentSoft, shape = RoundedCornerShape(16.dp)) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.padding(14.dp).size(26.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(message, color = colors.textSecondary, style = MaterialTheme.typography.bodyLarge)
        Surface(color = colors.surfaceOverlay, shape = RoundedCornerShape(14.dp)) {
            Text(
                "현재도 직접 입력은 바로 사용할 수 있어요.",
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(13.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("다른 방법") }
        }
    }
}
