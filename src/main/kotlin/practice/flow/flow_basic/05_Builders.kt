package practice.flow.flow_basic

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

suspend fun main() {
    // flowOf()
    // 매개변수로 넣은 인자들이 발행된다.
    val firstFlow = flowOf<Int>(1, 2, 3, 4, 5).collect { emittedValue ->
        println("$emittedValue")
    }

    val secondFlow = flowOf(1, 2, 3, 4, 6)

    listOf("A", "B").asFlow().collect { emittedValue ->
        println(emittedValue)
    }

    flow {
        delay(2000)
        emit("item emitted")
//        secondFlow.collect { emittedValue ->
//            emit(emittedValue)
//        }
        emitAll(secondFlow)
    }.collect { emittedValue ->
        println(emittedValue)
    }
}