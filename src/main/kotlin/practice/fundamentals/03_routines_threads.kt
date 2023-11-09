package practice.fundamentals

import kotlin.concurrent.thread

fun main() {
    println("main starts")
    threadRoutine(1, 500)
    threadRoutine(2, 300)
    Thread.sleep(1000)
    println("main ends")
}

// routine 생성 = kotlin에서의 일반적인 함수
fun threadRoutine(number: Int, delay: Long) {
    thread {
        println("Routine $number started to work")
        Thread.sleep(delay)
        println("Routine $number finished")
    }
}