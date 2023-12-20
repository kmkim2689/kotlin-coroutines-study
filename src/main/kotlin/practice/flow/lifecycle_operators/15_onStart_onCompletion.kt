package practice.flow.lifecycle_operators

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.coroutines.EmptyCoroutineContext

fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    flow1
        .onStart {
            println("started")
        }
        .onEach {
            println("$it")
        }
        .onStart {
            println("started 2")
        }
        .onCompletion { cause ->
            if (cause != null) {
                println("flow completed with exception : ${cause.message}")
                return@onCompletion
            }
            println("flow completed")
        }
        .launchIn(CoroutineScope(EmptyCoroutineContext))

    Thread.sleep(1000)
}