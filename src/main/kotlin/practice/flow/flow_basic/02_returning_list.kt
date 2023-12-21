package practice.flow.flow_basic

import java.math.BigInteger

fun main() {
    val startTime = System.currentTimeMillis()
    calculateFactorialOf(5).forEach {
        println("$it, time : $startTime")
    }
}

private fun calculateFactorialOf(number: Int): List<BigInteger> = buildList {
    var factorial = BigInteger.ONE
    for (i in 1..number) {
        Thread.sleep(10)
        factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
        // add to list
        add(factorial)
    }
}