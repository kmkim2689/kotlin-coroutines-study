package practice.flow.cancellation

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.EmptyCoroutineContext

suspend fun main() {
    val scope = CoroutineScope(EmptyCoroutineContext)

    scope.launch {
        flowOf(1, 2, 3)
            .onCompletion { throwable ->
                // to check if the flow got cancelled
                if (throwable is CancellationException) {
                    println("Flow Got Cancelled")
                }
            }
            // 수집 때마다 수동으로 검사하여 코루틴 취소 시 수동으로 취소
//            .onEach {
//                println("receive $it in onEach")
//
////                if (!currentCoroutineContext().job.isActive) {
////                    throw CancellationException()
////                }
//                ensureActive() // shorthand of the code above
//            }
            // 각 발행 시마다 코루틴의 활성화 여부를 검사
            .cancellable()
            .collect {
                println("collect $it")

                if (it == 2) cancel()
            }
    }.join() // main function should wait until the coroutine completes
}