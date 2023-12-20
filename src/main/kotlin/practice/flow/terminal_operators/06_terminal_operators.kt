package practice.flow.terminal_operators

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    flow1.collect { receivedValue ->
        println(receivedValue)

    }
}