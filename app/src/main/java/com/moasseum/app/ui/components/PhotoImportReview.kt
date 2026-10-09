package com.moasseum.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private data class PhotoImportDraft(val candidate: PhotoTransactionCandidate, val selected: Boolean)

@Composable
fun PhotoImportReview(
    state: PhotoImportState,
    saving: Boolean,
    onPickMore: () -> Unit,
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
                OutlinedButton(onClick = onPickMore, modifier = Modifier.fillMaxWidth()) { Text("다른 사진 선택") }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("닫기") }
            }
        }
        is PhotoImportState.Review -> {
            var drafts by remember(state) { mutableStateOf(state.candidates.map { PhotoImportDraft(it, true) }) }
            var editingIndex by remember(state) { mutableStateOf<Int?>(null) }
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
                                if (state.duplicateCount > 0) add("이미 기록된 ${state.duplicateCount}건 제외")
                                if (state.failedImageCount > 0) add("${state.failedImageCount}장 인식 실패")
                            }.joinToString(" · "), modifier = Modifier.padding(start = 7.dp), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (drafts.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("추가할 내역", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(enabled = !saving, onClick = {
                            val selectAll = drafts.any { !it.selected }
                            drafts = drafts.map { it.copy(selected = selectAll) }
                        }) { Text(if (drafts.all { it.selected }) "전체 해제" else "전체 선택") }
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
                            )
                        }
                    }
                } else {
                    Surface(color = colors.surfaceRaised, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("새로 추가할 내역이 없어요", fontWeight = FontWeight.SemiBold)
                            Text("이미 기록된 거래는 자동으로 제외했어요.", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                OutlinedButton(enabled = !saving, onClick = onPickMore, modifier = Modifier.fillMaxWidth()) { Text("사진 더 선택") }
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
        }
    }
}

@Composable
private fun PhotoCandidateCard(
    draft: PhotoImportDraft,
    enabled: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Surface(color = colors.surfaceRaised, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle).padding(start = 5.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft.selected, onCheckedChange = { onToggle() }, enabled = enabled)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(draft.candidate.transaction.merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatDate(draft.candidate.transaction.occurredDate)} · ${draft.candidate.paymentMethod} · ${categoryLabel(draft.candidate.transaction.categoryKey)}",
                    style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(formatWon(draft.candidate.transaction.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = if (draft.candidate.transaction.type.name == "INCOME") colors.income else colors.expense)
                TextButton(enabled = enabled, onClick = onEdit, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "인식 결과 수정", modifier = Modifier.size(14.dp))
                    Text("수정", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
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
    val parsedAmount = amount.filter(Char::isDigit).toLongOrNull()?.takeIf { it in 1..1_000_000_000_000L }
    val parsedDate = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("인식 결과 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceTextField(value = merchant, onValueChange = { merchant = it.take(80) }, label = { Text("가맹점") }, singleLine = true)
                FinanceTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(13) }, label = { Text("금액") }, singleLine = true)
                FinanceTextField(value = date, onValueChange = { date = it.take(10) }, label = { Text("날짜 (YYYY-MM-DD)") }, singleLine = true)
                Text(if (original.type.name == "INCOME") "수입" else "지출", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            TextButton(enabled = merchant.isNotBlank() && parsedAmount != null && parsedDate != null, onClick = {
                val candidate = draft.candidate.copy(transaction = original.copy(
                    merchant = merchant.trim(), amount = requireNotNull(parsedAmount), occurredDate = requireNotNull(parsedDate),
                ))
                onSave(draft.copy(candidate = candidate))
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
