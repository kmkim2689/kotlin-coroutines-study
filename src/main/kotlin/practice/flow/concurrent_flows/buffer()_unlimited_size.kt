package practice.flow.concurrent_flows

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
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
        capacity = UNLIMITED
    )

    flow.collect {
        println("Collector : Start eating pancake $it")
        // 하나의 데이터를 소비하기까지 300ms 소요
        delay(300)
        println("Collector : Finished eating pancake $it")
    }
}
