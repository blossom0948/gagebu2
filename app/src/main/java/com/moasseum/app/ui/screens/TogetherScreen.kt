package com.moasseum.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("함께", style = MaterialTheme.typography.headlineSmall)
                Text("공유할 기록만 골라서, 더 편하게 맞춰보세요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            FinanceCard(highlighted = true) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = colors.accent, shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Rounded.Group, contentDescription = null, tint = Color(0xFF06332B), modifier = Modifier.padding(9.dp).size(21.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("아직 연결된 사람이 없어요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("개인 기록은 계속 비공개로 안전하게 남아요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(
                        "공동 가계부를 연결하면 선택한 지출만 함께 보고, 공동 목표와 예산을 맞출 수 있어요.",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onShowUnavailable,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF06332B)),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("초대 코드 만들기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            FinanceCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("함께 쓰기의 약속", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    PrivacyPromise(Icons.Rounded.Lock, "개인 기록은 기본 비공개", "공유로 바꾼 거래만 공동 화면에 보여요.")
                    PrivacyPromise(Icons.Rounded.Shield, "데이터 경계를 먼저 확인", "연결 전에도 개인 원장은 그대로 유지돼요.")
                    PrivacyPromise(Icons.Rounded.SwapHoriz, "필요할 때만 연결 해제", "연결 해제 정책을 확인한 뒤 진행할 수 있어요.")
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onShowUnavailable,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("QR로 연결하기 · 서버 연결 후 사용")
            }
        }
    }
}

@Composable
private fun PrivacyPromise(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
) {
    val colors = LocalFinanceColors.current
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
