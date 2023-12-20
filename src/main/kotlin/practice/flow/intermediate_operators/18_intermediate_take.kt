package practice.flow.intermediate_operators

import kotlinx.coroutines.flow.*

suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
//        .take(count = 3)
        .takeWhile { it < 3 } // 처음으로 3 이상이 나올 때 수집 종료
        .collect { collectedValue ->
            println(collectedValue)
        }
}