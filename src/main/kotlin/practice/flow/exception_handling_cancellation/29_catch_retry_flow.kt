package practice.flow.exception_handling_cancellation

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry

suspend fun main(): Unit = coroutineScope {

    flow {
        repeat(5) { index ->
            delay(100) // network call

            if (index < 4) {
                emit("data")
            } else throw NetworkException("network failure")
        }
    }.retry { cause ->
        // predicate : true일 때만 재시도
        println("retry block")
        // 즉시 재시도를 한다면, 서버에 무리가 가므로 delay를 두는 것이 권장된다.
        delay(1000)
        cause is NetworkException
    }.collect {
        println("collect $it")
    }
}

class NetworkException(override val message: String): Exception(message)