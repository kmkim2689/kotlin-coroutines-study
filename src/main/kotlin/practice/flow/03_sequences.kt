package practice.flow

import java.math.BigInteger

fun main() {
    val startTime = System.currentTimeMillis()
    calculateFactorialOf(5).forEach {
        println("$it elapsed time : ${System.currentTimeMillis() - startTime}")
    }
    println("ready for the next work")
}

private fun calculateFactorialOf(number: Int): Sequence<BigInteger> = sequence {
    var factorial = BigInteger.ONE
    for (i in 1..number) {
        Thread.sleep(10)
        factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
        // yield : add value to sequence
        yield(factorial)
    }
}