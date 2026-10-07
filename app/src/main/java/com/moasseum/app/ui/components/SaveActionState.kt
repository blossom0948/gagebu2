package com.moasseum.app.ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** Keep the form open on failure; never report success before the write completes. */
class SaveActionState(private val scope: CoroutineScope) {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun save(block: suspend () -> Unit, onSuccess: () -> Unit = {}) {
        if (busy) return
        busy = true
        error = null
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                block()
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "저장하지 못했어요. 입력 내용을 확인하고 다시 시도해 주세요."
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun rememberSaveActionState(): SaveActionState {
    val scope = rememberCoroutineScope()
    return remember(scope) { SaveActionState(scope) }
}
