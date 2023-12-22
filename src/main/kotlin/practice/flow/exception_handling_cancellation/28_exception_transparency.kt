package practice.flow.exception_handling_cancellation

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

suspend fun main(): Unit = coroutineScope {
    flow {
//        try {
//            emit(1)
//        } catch (e: Exception) {
//            println("catch exception in flow builder")
//            emit(2)
//        }
        emit(1)
    }.catch {
        println("in catch operator")
    }.collect { emittedValue ->
        throw Exception("Exception in collect")
    }
}