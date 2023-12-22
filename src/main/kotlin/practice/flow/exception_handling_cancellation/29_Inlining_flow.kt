package practice.flow.exception_handling_cancellation

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow

suspend fun main(): Unit = coroutineScope {

    flow {
        emit(1)
        emit(2)
        emit(3)
    }.collect {
        println("collect $it")
    }
}

val inlinedFlow = flow<Int> {
    println("collect 1")
    println("collect 2")
    println("collect 3")
}