package practice.flow.intermediate_operators

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
//        .map { it * 10 }
        .map { "emission $it" }
        .collect { collectedValue ->
            println(collectedValue)
        }

    flowOf(1, 2, 3, 4, 5)
//        .map { it * 10 }
        .mapNotNull {
            if (it % 2 == 0) null else it
        }
        .collect { collectedValue ->
            println(collectedValue)
        }
}