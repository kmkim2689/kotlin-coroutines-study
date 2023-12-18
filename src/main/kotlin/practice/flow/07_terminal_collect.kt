package practice.flow

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

    val list1 = buildList {
        add(1)
        println("first value")
        add(2)
        println("second value")
    }
}