package com.moasseum.app

import com.moasseum.app.ui.components.SaveActionState
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SaveActionStateTest {
    @Test fun successIsDeliveredOnlyAfterTheWriteCompletes() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val state = SaveActionState(this)
        var saved = false
        state.save({ gate.await() }) { saved = true }
        assertTrue(state.busy); assertFalse(saved)
        gate.complete(Unit); yield()
        assertTrue(saved); assertFalse(state.busy); assertNull(state.error)
    }

    @Test fun failedWriteKeepsTheFormAndDoesNotExposeRawErrors() = runBlocking {
        val state = SaveActionState(this)
        var dismissed = false
        state.save({ throw IOException("PRIVATE_DATABASE_PATH") }) { dismissed = true }
        assertFalse(dismissed); assertFalse(state.busy)
        assertNotNull(state.error); assertFalse(state.error!!.contains("PRIVATE_DATABASE_PATH"))
    }

    @Test fun repeatedTapDoesNotStartASecondWrite() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val state = SaveActionState(this)
        var writes = 0
        state.save({ writes++; gate.await() })
        state.save({ writes++ })
        assertEquals(1, writes)
        gate.complete(Unit); yield()
        assertFalse(state.busy)
    }

    @Test fun retryClearsThePreviousFailure() = runBlocking {
        val state = SaveActionState(this)
        state.save({ throw IOException() })
        assertNotNull(state.error)
        var saved = false
        state.save({}) { saved = true }
        assertTrue(saved); assertNull(state.error); assertFalse(state.busy)
    }

    @Test fun cancellationIsNotPresentedAsStorageFailureOrSuccess() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val state = SaveActionState(scope)
        var saved = false
        try {
            state.save({ throw CancellationException() }) { saved = true }
            assertFalse(saved); assertFalse(state.busy); assertNull(state.error)
        } finally { scope.cancel() }
    }

    @Test fun disposingTheFormCancelsThePendingOperationAndClearsBusy() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val state = SaveActionState(scope)
        var saved = false
        state.save({ awaitCancellation() }) { saved = true }
        assertTrue(state.busy)
        scope.cancel()
        assertFalse(saved); assertFalse(state.busy); assertNull(state.error)
    }
}
