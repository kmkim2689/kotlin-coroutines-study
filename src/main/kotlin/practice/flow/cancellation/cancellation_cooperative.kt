package practice.flow.cancellation

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import java.math.BigInteger
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

                // 2일때 취소
                if (it == 2) cancel()
            }
    }.join() // main function should wait until the coroutine completes
}

private fun intFlow() = flow {
    emit(1)
    emit(2)

    // getting the context of the coroutine - to figure out which context the flow is running in
    // currentCoroutineContext()
    // currentCoroutineContext().ensureActive()

    // 시간이 오래 걸리는 헤비한 작업을 가정하기 위함
    println("start")
    calculateFactorial(1_000)
    println("end")

    emit(3)
}

private suspend fun calculateFactorial(number: Int): BigInteger = coroutineScope {
    var factorial = BigInteger.ONE
    for (i in 1..number) {
        factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
        ensureActive()
    }
    factorial
}