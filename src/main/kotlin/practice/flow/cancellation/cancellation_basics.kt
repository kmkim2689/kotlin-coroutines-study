package practice.flow.cancellation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

suspend fun main() {
    val scope = CoroutineScope(EmptyCoroutineContext)

    scope.launch {
        intFlow()
            .onCompletion { throwable ->
                // to check if the flow got cancelled
                if (throwable is CancellationException) {
                    println("Flow Got Cancelled")
                }
            }
            .collect {
                println("collect $it")
            }
    }.join() // main function should wait until the coroutine completes
}

private fun intFlow() = flow {
    emit(1)
    emit(2)
    emit(3)
}