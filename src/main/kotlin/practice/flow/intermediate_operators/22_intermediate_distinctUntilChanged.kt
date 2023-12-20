package practice.flow.intermediate_operators

import kotlinx.coroutines.flow.*

suspend fun main() {
    flowOf(1, 1, 2, 3, 4, 5)
        .distinctUntilChanged()
        .collect { collectedValue ->
            println(collectedValue)
        }
} // 1, 2, 3, 4, 5(두 번째 1은 수집되지 않는다.)