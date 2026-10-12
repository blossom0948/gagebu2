package com.moasseum.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.FinanceTextField
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.data.ManageSettingSearchOption
import com.moasseum.app.data.ManageSettingSearchResult
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

data class ManageSearchEntry(
    val id: String,
    val title: String,
    val description: String = "",
    val aliases: List<String> = emptyList(),
    val actionLabel: String = "설정 열기",
    val onSelect: () -> Unit,
)

@Composable
fun ManageNaturalSearch(
    entries: List<ManageSearchEntry>,
    aiAvailable: Boolean,
    onAiSearch: suspend (String, List<ManageSettingSearchOption>) -> Result<ManageSettingSearchResult>,
    recentIds: List<String> = emptyList(),
    onRememberSelected: suspend (String) -> Unit = {},
    onClearRecent: suspend () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var aiQuery by rememberSaveable { mutableStateOf("") }
    var aiRankedIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var aiIntent by rememberSaveable { mutableStateOf("") }
    var aiUnavailable by rememberSaveable { mutableStateOf(false) }
    var aiSearchSucceeded by rememberSaveable { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    val colors = LocalFinanceColors.current
    val scope = rememberCoroutineScope()
    val normalizedQuery = query.trim()
    val isSearchableQuery = normalizeSearchText(normalizedQuery).length >= 2
    val localResults = remember(normalizedQuery, entries) { rankManageSearch(normalizedQuery, entries) }
    val entriesById = remember(entries) { entries.associateBy { it.id } }
    val recentEntries = remember(recentIds, entriesById) { recentIds.distinct().mapNotNull(entriesById::get).take(5) }
    val results = remember(normalizedQuery, localResults, entriesById, aiQuery, aiRankedIds) {
        if (normalizedQuery == aiQuery && aiRankedIds.isNotEmpty()) {
            (aiRankedIds.mapNotNull(entriesById::get) + localResults.filterNot { it.id in aiRankedIds }).take(5)
        } else localResults.take(5)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        FinanceTextField(
            value = query,
            onValueChange = {
                query = it.take(100)
                selectedId = null
                aiQuery = ""
                aiRankedIds = emptyList()
                aiIntent = ""
                aiUnavailable = false
                aiSearchSucceeded = false
            },
            modifier = Modifier.fillMaxWidth().testTag("manage_natural_search_input"),
            singleLine = true,
            label = null,
            placeholder = { Text("예: 생활비가 얼마나 남았지?") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = {
                        query = ""
                        selectedId = null
                        aiQuery = ""
                        aiRankedIds = emptyList()
                        aiIntent = ""
                        aiUnavailable = false
                        aiSearchSucceeded = false
                    }) { Icon(Icons.Rounded.Close, contentDescription = "검색 지우기") }
                }
            } else null,
        )
        if (normalizedQuery.isBlank() && recentEntries.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("최근 찾은 메뉴", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { onClearRecent() } }) { Text("지우기") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                recentEntries.forEach { entry ->
                    AssistChip(
                        onClick = {
                            query = entry.title
                            selectedId = entry.id
                            aiQuery = ""
                            aiRankedIds = emptyList()
                            aiIntent = ""
                            aiUnavailable = false
                            aiSearchSucceeded = false
                        },
                        modifier = Modifier.testTag("manage_recent_${entry.id}"),
                        label = { Text(entry.title, maxLines = 1) },
                    )
                }
            }
        }
        if (isSearchableQuery) {
            OutlinedButton(
                onClick = {
                    val submittedQuery = query.trim()
                    scope.launch {
                        aiLoading = true
                        aiUnavailable = false
                        try {
                            onAiSearch(
                                submittedQuery,
                                entries.map { ManageSettingSearchOption(it.id, it.title, it.aliases) },
                            ).fold(
                                onSuccess = { result ->
                                    aiQuery = submittedQuery
                                    aiRankedIds = result.rankedIds
                                    aiIntent = result.intent
                                    aiSearchSucceeded = true
                                },
                                onFailure = {
                                    aiQuery = submittedQuery
                                    aiRankedIds = emptyList()
                                    aiIntent = ""
                                    aiUnavailable = true
                                    aiSearchSucceeded = false
                                },
                            )
                        } catch (_: Exception) {
                            aiQuery = submittedQuery
                            aiRankedIds = emptyList()
                            aiIntent = ""
                            aiUnavailable = true
                            aiSearchSucceeded = false
                        } finally {
                            aiLoading = false
                        }
                    }
                },
                enabled = aiAvailable && !aiLoading && !aiSearchSucceeded,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (aiLoading) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    when {
                        !aiAvailable -> "AI 검색은 로그인 후 사용 가능"
                        aiSearchSucceeded -> "AI 검색 완료"
                        else -> "AI로 의미 검색"
                    },
                )
            }
            Text(
                "AI 검색 시 검색어·키워드는 무료 Gemini로 전송돼 제품 개선·사람 검토에 쓰일 수 있어요. 거래·계좌 정보는 입력하지 마세요.",
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
            )
            if (aiLoading) {
                Text("표현의 뜻과 관련 메뉴를 비교하고 있어요", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            } else if (aiQuery == normalizedQuery && aiIntent.isNotBlank()) {
                Text("이렇게 이해했어요 · $aiIntent", color = colors.accent, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            } else if (aiQuery == normalizedQuery && aiUnavailable) {
                Text("AI 연결을 완료하지 못해 키워드 결과를 보여줘요.", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (normalizedQuery.isNotBlank() && !isSearchableQuery) {
            Text("두 글자 이상 입력해 주세요.", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
        } else if (isSearchableQuery) {
            FinanceCard {
                if (results.isEmpty()) {
                    Text(
                        "찾는 설정을 못 찾았어요. 다른 표현으로 검색해 보세요.",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    )
                } else {
                    Column {
                        results.forEachIndexed { index, entry ->
                            if (index > 0) androidx.compose.material3.HorizontalDivider(
                                color = colors.divider.copy(alpha = 0.55f),
                                modifier = Modifier.padding(horizontal = 14.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable(role = Role.Button) { selectedId = if (selectedId == entry.id) null else entry.id }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (entry.description.isNotBlank()) {
                                        Text(entry.description, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                if (aiQuery == normalizedQuery && entry.id in aiRankedIds) {
                                    Text("AI 추천", color = colors.accent, style = MaterialTheme.typography.labelSmall)
                                    Spacer(Modifier.width(5.dp))
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = if (selectedId == entry.id) "선택됨" else "기능 설명 보기",
                                    tint = if (selectedId == entry.id) colors.accent else colors.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
            entriesById[selectedId]?.let { selected ->
                FinanceCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("선택한 기능", color = colors.accent, style = MaterialTheme.typography.labelMedium)
                        Text(selected.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (selected.description.isNotBlank()) {
                            Text(selected.description, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        onRememberSelected(selected.id)
                                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        // A failed local history write must not block the requested feature action.
                                    }
                                    selected.onSelect()
                                    selectedId = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(selected.actionLabel) }
                    }
                }
            }
        }
    }
}

fun rankManageSearch(query: String, entries: List<ManageSearchEntry>): List<ManageSearchEntry> {
    val compactQuery = normalizeSearchText(query)
    if (compactQuery.length < 2) return emptyList()
    val queryTokens = query.lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .map(::normalizeSearchText)
        .filter { it.length >= 2 }

    return entries.mapIndexedNotNull { index, entry ->
        val fields = listOf(entry.title, entry.description) + entry.aliases
        val compactFields = fields.map(::normalizeSearchText).filter { it.length >= 2 }
        var score = 0

        compactFields.forEach { field ->
            if (field == compactQuery) score = maxOf(score, 110)
            else if (compactQuery.contains(field)) score = maxOf(score, 28 + field.length.coerceAtMost(18))
            else if (field.contains(compactQuery)) score = maxOf(score, 22 + compactQuery.length.coerceAtMost(12))
        }

        val searchableTokens = fields.flatMap { field ->
            field.lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+"))
                .map(::normalizeSearchText).filter { it.length >= 2 }
        }.distinct()
        var matchedTokens = 0
        queryTokens.forEach { queryToken ->
            val tokenScore = searchableTokens.maxOfOrNull { word -> tokenSimilarity(queryToken, word) } ?: 0
            if (tokenScore > 0) {
                matchedTokens++
                score += tokenScore
            }
        }
        if (queryTokens.size >= 2 && matchedTokens >= 2) score += matchedTokens * 2
        if (score < 6) null else Triple(index, score, entry)
    }.sortedWith(compareByDescending<Triple<Int, Int, ManageSearchEntry>> { it.second }.thenBy { it.first })
        .map { it.third }
}

private fun tokenSimilarity(query: String, candidate: String): Int {
    if (query == candidate) return 20
    if (query.startsWith(candidate) || candidate.startsWith(query) || query.contains(candidate) || candidate.contains(query)) {
        return if (minOf(query.length, candidate.length) >= 2) 12 else 0
    }
    if (minOf(query.length, candidate.length) < 3) return 0
    val allowedDistance = if (maxOf(query.length, candidate.length) >= 7) 2 else 1
    val distance = editDistanceAtMost(query, candidate, allowedDistance) ?: return 0
    return when (distance) {
        0 -> 20
        1 -> 6
        else -> 3
    }
}

private fun editDistanceAtMost(left: String, right: String, maxDistance: Int): Int? {
    if (kotlin.math.abs(left.length - right.length) > maxDistance) return null
    var previous = IntArray(right.length + 1) { it }
    for (i in left.indices) {
        val current = IntArray(right.length + 1)
        current[0] = i + 1
        var rowMinimum = current[0]
        for (j in right.indices) {
            current[j + 1] = minOf(
                previous[j + 1] + 1,
                current[j] + 1,
                previous[j] + if (left[i] == right[j]) 0 else 1,
            )
            rowMinimum = minOf(rowMinimum, current[j + 1])
        }
        if (rowMinimum > maxDistance) return null
        previous = current
    }
    return previous[right.length].takeIf { it <= maxDistance }
}

private fun normalizeSearchText(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT)
    .filter { it.isLetterOrDigit() }
