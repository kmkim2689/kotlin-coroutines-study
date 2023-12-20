package practice.flow.terminal_operators

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.reduce
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

    val items = flow1.reduce { accumulator, emittedItem ->
        // accumulator
        // 중요 : 처음으로 발행된 값의 경우, 계산이 불가능. 처음에 발행된 값의 경우 두번째 발행에서 accumulator의 값으로 활용됨
        // round1 -> x
        // round2 - accumulator : 1, emittedItem : 2 => 3 => 최종 결과
        accumulator + emittedItem // new accumulator value
    }
    println(items) // 3

}