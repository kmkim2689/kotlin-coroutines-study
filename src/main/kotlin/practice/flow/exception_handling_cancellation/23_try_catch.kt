package practice.flow.exception_handling_cancellation

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()

        try {
            stocksFlow
                .onCompletion { cause ->
                    if (cause == null) println("successfully completed")
                    else println("$cause")
                }
                .collect { stock ->
                    println(stock)
                }
        } catch (e: Exception) {
            println("handle exception in catch block")
        }
    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}