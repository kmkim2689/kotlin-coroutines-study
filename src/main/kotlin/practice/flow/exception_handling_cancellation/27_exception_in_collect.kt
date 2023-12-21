package practice.flow.exception_handling_cancellation

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()

        stocksFlow
            .onCompletion { cause ->
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            .onEach {
                throw Exception("exception in collect")
            }
            // catch operator : **terminal operator 이전에 위치해야 한다.**
            // try - catch와 비슷하게, **업스트림** flow에서 발생하는 모든 예외를 처리하는 역할의 연산자
            .catch { throwable ->
                println("handle the exception in catch : $throwable")
            }
            // 새로운 flow에서의 예외 발생 대비 : 밑에 또 하나의 catch 연산자 활용
            .launchIn(this)

    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}
