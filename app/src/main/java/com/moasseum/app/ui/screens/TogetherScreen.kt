package com.moasseum.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.theme.LocalFinanceColors

@Composable
fun TogetherScreen(
    onShowUnavailable: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("우리 돈, 한눈에", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text("파트너와 연결하면 이런 걸 할 수 있어요", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(
                    "파트너 연결은 무료로 시작할 수 있어요.\n개인 기록은 계속 내 기기에 남아요.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(onClick = onShowUnavailable, shape = RoundedCornerShape(22.dp)) {
                    Text("커플 기능 미리보기", color = colors.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TogetherFeature(
                        icon = Icons.Rounded.AttachMoney,
                        title = "합산 대시보드",
                        message = "각자 지출을 한 화면에서 비교",
                        modifier = Modifier.weight(1f),
                    )
                    TogetherFeature(
                        icon = Icons.Rounded.Flag,
                        title = "공동 목표",
                        message = "여행, 집 보증금 함께 모으기",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TogetherFeature(
                        icon = Icons.Rounded.ChatBubbleOutline,
                        title = "AI 분석",
                        message = "커플 소비 패턴 코멘트",
                        modifier = Modifier.weight(1f),
                    )
                    TogetherFeature(
                        icon = Icons.Rounded.Description,
                        title = "월간 리포트",
                        message = "카테고리별 분석과 공유",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            FinanceCard {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("아래 버튼을 눌러 초대 코드를 생성하세요", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
        item {
            Button(
                onClick = onShowUnavailable,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF06332B)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("코드 생성하기", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider.copy(alpha = 0.7f))
                Text("또는", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider.copy(alpha = 0.7f))
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text("파트너의 코드가 있나요?", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onShowUnavailable, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("코드 입력하기")
                }
            }
        }
    }
}

@Composable
private fun TogetherFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Surface(color = colors.surfaceOverlay, shape = RoundedCornerShape(13.dp)) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.padding(9.dp).size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(message, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
