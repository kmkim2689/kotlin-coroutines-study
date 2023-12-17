package practice.flow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.math.BigInteger

fun main() = runBlocking {
    val startTime = System.currentTimeMillis()
    launch {
        calculateFactorialOf(5).collect {
            println("$it elapsed time : ${System.currentTimeMillis() - startTime}")
        }
    }
    println("ready for the next work")
}

private fun calculateFactorialOf(number: Int): Flow<BigInteger> = flow {
    var factorial = BigInteger.ONE
    for (i in 1..number) {
        delay(10)
        factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
        // yield : add value to sequence
        emit(factorial)
    }
}.flowOn(Dispatchers.Default)