package com.moasseum.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moasseum.app.data.PhotoImportState
import com.moasseum.app.data.PhotoTransactionCandidate
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatWon
import com.moasseum.app.ui.theme.LocalFinanceColors
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PhotoImportDraft(val candidate: PhotoTransactionCandidate, val selected: Boolean)

@Composable
fun PhotoImportReview(
    state: PhotoImportState,
    saving: Boolean,
    onPickMore: (List<PhotoTransactionCandidate>, Set<String>) -> Unit,
    onSave: (List<PhotoTransactionCandidate>) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    when (state) {
        PhotoImportState.Idle -> Unit
        is PhotoImportState.Loading -> {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("사진에서 거래를 읽고 있어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${state.completedImages}/${state.totalImages}장 처리", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        is PhotoImportState.Error -> {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("내역을 읽지 못했어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(state.message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { onPickMore(emptyList(), emptySet()) }, modifier = Modifier.fillMaxWidth()) { Text("다른 사진 선택") }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("닫기") }
            }
        }
        is PhotoImportState.Review -> {
            var drafts by remember(state.candidates, state.unselectedCandidateIds) {
                mutableStateOf(state.candidates.map {
                    PhotoImportDraft(
                        it,
                        selected = it.candidateCanBeSaved() && it.id !in state.unselectedCandidateIds,
                    )
                })
            }
            var editingIndex by remember(state) { mutableStateOf<Int?>(null) }
            var evidenceIndex by remember(state) { mutableStateOf<Int?>(null) }
            val allDraftsSelected = drafts.isNotEmpty() && drafts.all { it.selected }
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("사진 내역 확인", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${state.imageCount}장 · 새 내역 ${state.candidates.size}건", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                }
                if (state.duplicateCount > 0 || state.failedImageCount > 0) {
                    Surface(color = colors.accentSoft, shape = RoundedCornerShape(13.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.accent, modifier = Modifier.size(17.dp))
                            Text(buildList {
                                if (state.duplicateCount > 0) add("중복 ${state.duplicateCount}건 제외")
                                if (state.failedImageCount > 0) add("${state.failedImageCount}장 인식 실패")
                            }.joinToString(" · "), modifier = Modifier.padding(start = 7.dp), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (drafts.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("추가할 내역", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(enabled = !saving && drafts.isNotEmpty(), onClick = {
                            val selectAll = drafts.any { !it.selected }
                            drafts = drafts.map { it.copy(selected = selectAll) }
                        }) { Text(if (allDraftsSelected) "전체 해제" else "전체 선택") }
                    }
                    if (drafts.any { !it.candidate.candidateCanBeSaved() }) {
                        Text("확인 필요 내역도 선택할 수 있어요. 사진을 보고 필요하면 수정하세요.", color = colors.warning, style = MaterialTheme.typography.labelSmall)
                    }
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 390.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        drafts.forEachIndexed { index, draft ->
                            PhotoCandidateCard(
                                draft = draft,
                                enabled = !saving,
                                onToggle = { drafts = drafts.toMutableList().also { it[index] = it[index].copy(selected = !it[index].selected) } },
                                onEdit = { editingIndex = index },
                                onViewEvidence = { evidenceIndex = index },
                            )
                        }
                    }
                } else {
                    Surface(color = colors.surfaceRaised, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("새로 추가할 내역이 없어요", fontWeight = FontWeight.SemiBold)
                            Text("사진에서 새 거래를 찾지 못했어요.", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                OutlinedButton(enabled = !saving, onClick = {
                    onPickMore(
                        drafts.map { it.candidate },
                        drafts.filterNot { it.selected }.mapTo(mutableSetOf()) { it.candidate.id },
                    )
                }, modifier = Modifier.fillMaxWidth()) { Text("사진 더 선택") }
                Button(
                    enabled = !saving && drafts.any { it.selected },
                    onClick = { onSave(drafts.filter { it.selected }.map { it.candidate }) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (saving) "저장 중…" else "신규 ${drafts.count { it.selected }}건 추가", fontWeight = FontWeight.Bold)
                }
                TextButton(enabled = !saving, onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("취소") }
            }

            editingIndex?.let { index ->
                val draft = drafts.getOrNull(index)
                if (draft != null) PhotoCandidateEditDialog(
                    draft = draft,
                    onDismiss = { editingIndex = null },
                    onSave = { updated ->
                        drafts = drafts.toMutableList().also { it[index] = updated }
                        editingIndex = null
                    },
                )
            }
            evidenceIndex?.let { index ->
                drafts.getOrNull(index)?.let { draft ->
                    PhotoEvidenceDialog(
                        candidate = draft.candidate,
                        onDismiss = { evidenceIndex = null },
                        onEdit = {
                            evidenceIndex = null
                            editingIndex = index
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCandidateCard(
    draft: PhotoImportDraft,
    enabled: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onViewEvidence: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Surface(color = colors.surfaceRaised, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle).padding(start = 5.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft.selected, onCheckedChange = { onToggle() }, enabled = enabled)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(draft.candidate.transaction.merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${if (draft.candidate.transaction.type.name == "INCOME") "수입" else "지출"} · ${formatDate(draft.candidate.transaction.occurredDate)} · ${draft.candidate.paymentMethod} · ${categoryLabel(draft.candidate.transaction.categoryKey)}",
                    style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (draft.candidate.transaction.needsConfirmation.isNotEmpty()) {
                    Text("인식 결과 확인 필요", style = MaterialTheme.typography.labelSmall, color = colors.warning)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(formatWon(draft.candidate.transaction.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = if (draft.candidate.transaction.type.name == "INCOME") colors.income else colors.expense)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (draft.candidate.sourceImageUri != null || draft.candidate.sourceEvidenceImagePath != null) {
                        TextButton(enabled = enabled, onClick = onViewEvidence, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = "원본 거래 부분 보기", modifier = Modifier.size(14.dp))
                            Text("사진", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    TextButton(enabled = enabled, onClick = onEdit, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "인식 결과 수정", modifier = Modifier.size(14.dp))
                        Text("수정", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoEvidenceDialog(
    candidate: PhotoTransactionCandidate,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    var evidenceBitmap by remember(candidate.sourceEvidenceImagePath) { mutableStateOf<Bitmap?>(null) }
    var sourceBitmap by remember(candidate.sourceImageUri) { mutableStateOf<Bitmap?>(null) }
    var showFullSource by remember(candidate.id) { mutableStateOf(false) }
    LaunchedEffect(candidate.sourceEvidenceImagePath, candidate.sourceImageUri) {
        evidenceBitmap = withContext(Dispatchers.IO) {
            candidate.sourceEvidenceImagePath?.let(BitmapFactory::decodeFile)
        }
    }
    LaunchedEffect(showFullSource, candidate.sourceImageUri) {
        if (showFullSource && sourceBitmap == null) {
            sourceBitmap = withContext(Dispatchers.IO) {
                candidate.sourceImageUri?.let { rawUri ->
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(rawUri))?.use(BitmapFactory::decodeStream)
                    }.getOrNull()
                }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("원본 거래 부분") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val bitmap = if (showFullSource) sourceBitmap else evidenceBitmap
                if (bitmap == null) {
                    Text(
                        if (showFullSource) "전체 캡처를 불러오지 못했어요. 수정에서 내용을 직접 고칠 수 있어요."
                        else "이 거래의 부분 캡처를 만들지 못했어요. 수정에서 내용을 확인할 수 있어요.",
                        color = LocalFinanceColors.current.textSecondary,
                    )
                    if (!showFullSource && candidate.sourceImageUri != null) {
                        TextButton(onClick = { showFullSource = true }) { Text("원본 전체 보기") }
                    }
                } else {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = if (showFullSource) "선택한 금융앱 캡처 원본" else "거래 인식에 사용된 캡처 부분",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text("수정하기") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun PhotoCandidateEditDialog(
    draft: PhotoImportDraft,
    onDismiss: () -> Unit,
    onSave: (PhotoImportDraft) -> Unit,
) {
    val original = draft.candidate.transaction
    var merchant by remember(draft) { mutableStateOf(original.merchant) }
    var amount by remember(draft) { mutableStateOf(original.amount.toString()) }
    var date by remember(draft) { mutableStateOf(original.occurredDate.toString()) }
    var type by remember(draft) { mutableStateOf(original.type) }
    var categoryKey by remember(draft) { mutableStateOf(original.categoryKey) }
    var paymentMethod by remember(draft) { mutableStateOf(draft.candidate.paymentMethod) }
    var memo by remember(draft) { mutableStateOf(original.memo) }
    val parsedAmount = amount.filter(Char::isDigit).toLongOrNull()?.takeIf { it in 1..1_000_000_000_000L }
    val parsedDate = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("인식 결과 수정") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type.name == "EXPENSE", onClick = { type = com.moasseum.app.domain.TransactionType.EXPENSE }, label = { Text("지출") })
                    FilterChip(selected = type.name == "INCOME", onClick = { type = com.moasseum.app.domain.TransactionType.INCOME }, label = { Text("수입") })
                }
                FinanceTextField(value = merchant, onValueChange = { merchant = it.take(80) }, label = { Text("가맹점") }, singleLine = true)
                FinanceTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(13) }, label = { Text("금액") }, singleLine = true)
                FinanceTextField(value = date, onValueChange = { date = it.take(10) }, label = { Text("날짜 (YYYY-MM-DD)") }, singleLine = true)
                Text("카테고리", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allCategorySpecs().forEach { spec ->
                        FilterChip(
                            selected = categoryKey == spec.key,
                            onClick = { categoryKey = spec.key },
                            label = { Text(categoryLabel(spec.key)) },
                            leadingIcon = { Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(15.dp)) },
                        )
                    }
                }
                FinanceTextField(value = paymentMethod, onValueChange = { paymentMethod = it.take(40) }, label = { Text("결제 수단") }, singleLine = true)
                FinanceTextField(value = memo, onValueChange = { memo = it.take(120) }, label = { Text("메모") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = merchant.isNotBlank() && parsedAmount != null && parsedDate != null, onClick = {
                val candidate = draft.candidate.copy(transaction = original.copy(
                    merchant = merchant.trim(), amount = requireNotNull(parsedAmount), occurredDate = requireNotNull(parsedDate),
                    type = type,
                    categoryKey = categoryKey,
                    memo = memo.trim(),
                    needsConfirmation = emptyList(),
                    categoryConfidence = 1.0,
                ), paymentMethod = paymentMethod.trim().ifBlank { "금융앱 캡처" })
                onSave(draft.copy(candidate = candidate, selected = draft.selected || original.needsConfirmation.isNotEmpty()))
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

private fun PhotoTransactionCandidate.candidateCanBeSaved(): Boolean = transaction.needsConfirmation.isEmpty()
