package practice.flow.channels

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun main() = coroutineScope {
    // channel을 생성하기 위해, produce 메소드 활용
    val channel = produce<Int> {
        println("sending 10")
        // 발행을 위해 send 활용
        send(10)

        println("sending 20")
        // 발행을 위해 send 활용
        send(20)
    }

    launch {
        channel.consumeEach { receivedValue ->
            println("consumer1 : $receivedValue")
        }
    }
    launch {
        channel.consumeEach { receivedValue ->
            println("consumer2 : $receivedValue")
        }
    }
}