package practice.flow.intermediate_operators

import kotlinx.coroutines.flow.*

suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
        .withIndex()
        .collect { collectedValue ->
            println(collectedValue)
        }
}