package com.moasseum.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moasseum.app.R

private val GoogleButtonFont = FontFamily(Font(R.font.google_sans, weight = FontWeight.Medium))

@Composable
internal fun GoogleSignInButton(enabled: Boolean, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fill = Color(if (dark) 0xFF131314 else 0xFFFFFFFF)
    val text = Color(if (dark) 0xFFE3E3E3 else 0xFF1F1F1F)
    val stroke = Color(if (dark) 0xFF8E918F else 0xFF747775)
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, stroke),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = fill, contentColor = text),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Image(painterResource(R.drawable.google_g), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Google로 계속하기", fontFamily = GoogleButtonFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
internal fun KakaoSignInButton(available: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = available && enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFEE500),
            contentColor = Color(0xFF191919),
            disabledContainerColor = Color(0xFFFEE500).copy(alpha = 0.42f),
            disabledContentColor = Color(0xFF191919).copy(alpha = 0.6f),
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text("카카오로 계속하기", fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
