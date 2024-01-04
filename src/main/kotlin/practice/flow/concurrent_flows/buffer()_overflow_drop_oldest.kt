package practice.flow.concurrent_flows

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow

suspend fun main() = coroutineScope {

    val flow = flow {
        repeat(5) {
            val idx = it + 1
            println("Emitter : Start Cooking pancake $idx")
            // 하나의 데이터를 발행하기까지 100ms 소요
            delay(100L)
            println("Emitter : Pancake $idx is ready")
            emit(idx)
        }
    }.buffer(
        capacity = 1, // 극단적인 상황 연출을 위하여 buffer size를 1로 설정
        onBufferOverflow = BufferOverflow.DROP_OLDEST // 버퍼가 찬 상태에서 새로운 데이터 발행 시도 시, 버퍼에 공간이 생길 때까지 대기
    )

    flow.collect {
        println("Collector : Start eating pancake $it")
        // 하나의 데이터를 소비하기까지 300ms 소요
        delay(300)
        println("Collector : Finished eating pancake $it")
    }
}
