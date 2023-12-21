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
            // catch operator : **terminal operator 이전에 위치해야 한다.**
            // try - catch와 비슷하게, **업스트림** flow에서 발생하는 모든 예외를 처리하는 역할의 연산자
            .catch { throwable ->
                println("handle the exception in catch : $throwable")
                // 예외 발생 시 새로운 flow를 수집
                emitAll(fallbackFlow())
            }
            // 새로운 flow에서의 예외 발생 대비 : 밑에 또 하나의 catch 연산자 활용
            .catch {  throwable ->
                println("handle the exception in catch : $throwable")
            }
            .collect { stock ->
                println(stock)
            }

    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}

private fun fallbackFlow(): Flow<String> = flow {
    // 새로운 flow 실행
    emit("fallback stock")

    // 새로운 flow에서도 예외 발생 상황 가정
    throw Exception("exception in fallback flow!!!")
}