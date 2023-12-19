package practice.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.toSet

suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)

        println("third value")
        emit(2)
    }

    val setItems = flow1.toSet()
    println(setItems)

    val listItems = flow1.toList()
    println(listItems)

}