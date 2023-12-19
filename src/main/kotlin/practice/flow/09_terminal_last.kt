package practice.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last

suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val items = flow1.last()
    println(items)

}