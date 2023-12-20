package practice.flow.terminal_operators

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.single

suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val items = flow1.fold(initial = 5) { accumulator, emittedItem ->
        // accumulator
        // reducer와 유사 : 발행된 데이터를 사용할 때마다 accumulator 값을 같이 활용할 수 있다.
        // 중요 : 처음으로 발행된 값의 경우, accumulator의 값은 initial로 설정한 값이다.
        // round1 - accumulator : 5, emittedItem : 1 => 6
        // round2 - accumulator : 6, emittedItem : 2 => 8 => 최종 결과
        accumulator + emittedItem // new accumulator value
    }
    println(items)

}