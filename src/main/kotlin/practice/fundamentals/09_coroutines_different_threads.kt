package practice.fundamentals

import kotlinx.coroutines.*

fun main() = runBlocking {
    println("main starts")
    joinAll(
        async {
            threadSwitchingCoroutine(1, 500)
        },
        async {
            threadSwitchingCoroutine(2, 300)
        }
    )
    println("main ends")
}

suspend fun threadSwitchingCoroutine(number: Int, delay: Long) {
    println("Coroutine $number starts working ${Thread.currentThread().name}")
    delay(delay) // Thread의 sleep 대신 delay 활용
    withContext(Dispatchers.Default) {
        println("Coroutine $number ends ${Thread.currentThread().name}")
    }
}