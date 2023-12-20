package practice.flow.intermediate_operators

import kotlinx.coroutines.flow.*

suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
        // .drop(count = 3) // 처음 발행되는 3개에 대한 수집을 드롭한다.
        .dropWhile { it < 2 }
        .collect { collectedValue ->
            println(collectedValue)
        }
}