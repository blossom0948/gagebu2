package com.moasseum.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moasseum.app.auth.AuthRepository
import com.moasseum.app.ui.components.FinanceTextField
import com.moasseum.app.ui.theme.LocalFinanceColors
import kotlinx.coroutines.launch

@Composable
fun AuthDialog(repository: AuthRepository, onDismiss: () -> Unit) {
    val state by repository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("LOGIN") }
    var localError by remember { mutableStateOf<String?>(null) }
    var showLogout by rememberSaveable { mutableStateOf(false) }
    var showCode by rememberSaveable { mutableStateOf(false) }
    val colors = LocalFinanceColors.current
    val context = LocalContext.current
    fun switch(value: String) { mode = value; password = ""; confirmation = ""; code = ""; localError = null; showCode = false }
    LaunchedEffect(state.recoveryEmail) {
        state.recoveryEmail?.let { email = it; switch("NEW_PASSWORD") }
    }
    fun close() { if (!state.busy) scope.launch { repository.cancelRecovery(); repository.cancelGoogleLogin(); onDismiss() } }
    AlertDialog(onDismissRequest = { close() }, title = { Text("로그인·계정") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (!repository.configured) Text("로그인 서버에 연결되지 않았습니다.", style = MaterialTheme.typography.bodyMedium, color = colors.expense)
            state.user?.takeIf { mode != "NEW_PASSWORD" }?.let { user ->
                Text("로그인됨 · ${user.email}", style = MaterialTheme.typography.titleSmall)
                TextButton(enabled = !state.busy, onClick = { showLogout = true }) { Text("로그아웃") }
            } ?: run {
                if (mode in listOf("LOGIN", "SIGNUP") && repository.supportsGoogleLogin) {
                    GoogleSignInButton(enabled = repository.configured && !state.busy, onClick = {
                        localError = null
                        password = ""; confirmation = ""; code = ""
                        scope.launch {
                            repository.beginGoogleLogin().onSuccess { url ->
                                try {
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addCategory(android.content.Intent.CATEGORY_BROWSABLE))
                                } catch (_: android.content.ActivityNotFoundException) {
                                    repository.cancelGoogleLogin(); localError = "로그인할 브라우저가 없어요. 브라우저를 설치하거나 이메일 로그인을 사용해 주세요."
                                } catch (_: SecurityException) {
                                    repository.cancelGoogleLogin(); localError = "브라우저를 열지 못했어요. 이메일 로그인을 사용해 주세요."
                                }
                            }
                        }
                    })
                    Text("이메일·기본 프로필만 사용합니다.", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.15f))
                }
                if (mode != "NEW_PASSWORD") Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(mode == "LOGIN", { switch("LOGIN") }, enabled = !state.busy, label = { Text("로그인") })
                    FilterChip(mode == "SIGNUP", { switch("SIGNUP") }, enabled = !state.busy, label = { Text("회원가입") })
                }
                FinanceTextField(email, { email = it.take(254) }, label = { Text("이메일") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), enabled = !state.busy && mode != "NEW_PASSWORD")
                if (mode in listOf("LOGIN", "SIGNUP", "NEW_PASSWORD")) {
                    FinanceTextField(password, { password = it.take(256) }, label = { Text(when (mode) { "NEW_PASSWORD" -> "새 비밀번호 · 8자 이상"; "SIGNUP" -> "비밀번호 · 8자 이상"; else -> "비밀번호" }) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), enabled = !state.busy)
                    if (mode != "LOGIN") FinanceTextField(confirmation, { confirmation = it.take(256) }, label = { Text("비밀번호 확인") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), enabled = !state.busy)
                }
                if (repository.configured && mode in listOf("SIGNUP", "RECOVER", "VERIFY", "RECOVERY_CODE")) Text("이메일 인증은 등록된 본인 이메일만 가능하며, 발송은 시간당 2회로 제한됩니다.", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                if (mode in listOf("VERIFY", "RECOVERY_CODE")) {
                    Text("이 휴대폰에서 인증 메일의 링크를 열어 주세요. 유효 시간은 1시간입니다.", style = MaterialTheme.typography.labelMedium)
                    TextButton(enabled = !state.busy, onClick = { showCode = !showCode }) { Text(if (showCode) "인증 링크로 진행하기" else "메일에 인증 번호가 있는 경우") }
                    if (showCode) FinanceTextField(code, { code = it.filter(Char::isDigit).take(10) }, label = { Text("인증 번호") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.busy)
                }
                if (mode !in listOf("VERIFY", "RECOVERY_CODE") || showCode) Button(enabled = repository.configured && !state.busy, onClick = {
                    if (mode in listOf("SIGNUP", "NEW_PASSWORD") && password != confirmation) { localError = "비밀번호 확인이 일치하지 않아요."; return@Button }
                    localError = null
                    val operation = mode
                    val submittedEmail = email
                    val submittedPassword = password
                    val submittedCode = code
                    scope.launch {
                        val result = when (operation) {
                            "LOGIN" -> repository.login(submittedEmail, submittedPassword)
                            "SIGNUP" -> repository.signup(submittedEmail, submittedPassword)
                            "VERIFY" -> repository.verifySignup(submittedEmail, submittedCode)
                            "RECOVER" -> repository.recover(submittedEmail)
                            "RECOVERY_CODE" -> repository.verifyRecovery(submittedEmail, submittedCode)
                            else -> repository.resetPassword(submittedPassword)
                        }
                        if (result.isSuccess) when (operation) {
                            "SIGNUP" -> switch("VERIFY")
                            "RECOVER" -> switch("RECOVERY_CODE")
                            "RECOVERY_CODE" -> switch("NEW_PASSWORD")
                            "NEW_PASSWORD" -> switch("LOGIN")
                            else -> { password = ""; confirmation = ""; code = "" }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(when (mode) { "LOGIN" -> "로그인"; "SIGNUP" -> "회원가입"; "RECOVER" -> "재설정 메일 보내기"; "NEW_PASSWORD" -> "비밀번호 변경"; else -> "인증 번호 확인" })
                }
                if (mode == "VERIFY") TextButton(enabled = repository.configured && !state.busy, onClick = { scope.launch { repository.resend(email) } }) { Text("인증 메일 다시 보내기") }
                TextButton(enabled = !state.busy, onClick = {
                    if (mode == "NEW_PASSWORD") scope.launch { repository.cancelRecovery(); switch("LOGIN") }
                    else switch(if (mode == "LOGIN") "RECOVER" else "LOGIN")
                }) { Text(if (mode == "LOGIN") "비밀번호를 잊으셨나요?" else "로그인으로 돌아가기") }
            }
            Text("인증 정보는 로그인 서버로 전송됩니다. 비밀번호는 저장하지 않으며 가계부는 자동 공유되지 않습니다.", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            (localError ?: state.error)?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            state.message?.let { Text(it, color = colors.accent, style = MaterialTheme.typography.labelMedium) }
        }
    }, confirmButton = { TextButton(enabled = !state.busy, onClick = { close() }) { Text("닫기") } })
    if (showLogout) AlertDialog(onDismissRequest = { showLogout = false }, title = { Text("로그아웃할까요?") }, text = { Text("이 기기의 로그인 정보를 지웁니다. 기기에 저장된 가계부 기록은 유지돼요.") },
        confirmButton = { TextButton(onClick = { showLogout = false; scope.launch { repository.logout() } }) { Text("로그아웃") } }, dismissButton = { TextButton(onClick = { showLogout = false }) { Text("취소") } })
}
