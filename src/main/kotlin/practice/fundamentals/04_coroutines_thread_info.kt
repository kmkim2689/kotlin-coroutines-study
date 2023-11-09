package practice.fundamentals

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("main starts")
    joinAll(
        async {
            coroutineWithThreadInfo(1, 500)
        },
        async {
            coroutineWithThreadInfo(2, 300)
        }
    )
    println("main ends")
}

suspend fun coroutineWithThreadInfo(number: Int, delay: Long) {
    println("Coroutine $number starts working ${Thread.currentThread().name}")
    delay(delay) // Thread의 sleep 대신 delay 활용
    println("Coroutine $number ends")
}