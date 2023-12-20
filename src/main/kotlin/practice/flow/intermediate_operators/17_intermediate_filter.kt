package practice.flow.intermediate_operators

import kotlinx.coroutines.flow.*

suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
//        .filter { it % 2 == 0 }
//        .filterNot { it > 3 }
        .filterIsInstance<Int>()
        .collect { collectedValue ->
            println(collectedValue)
        }

}