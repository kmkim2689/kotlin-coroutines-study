package practice.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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

    val items = flow1.firstOrNull()
    println(items)

    // predicate to first : 첫 번째로 발행될 아이템의 조건을 설정
    // 여기서는 1보다 큰 정수 중 첫 번째로 발행된 것을 출력하고자 함
    val item = flow1.first {
        // bigger than one
        it > 1
    }
    println(item)
}