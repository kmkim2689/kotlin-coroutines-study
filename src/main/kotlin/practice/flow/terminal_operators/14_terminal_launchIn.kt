package practice.flow.terminal_operators

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val scope = CoroutineScope(EmptyCoroutineContext)

//    flow1
//        .onEach {
//            println("launchIn()1 - $it")
//        }
//        .launchIn(scope)

//    flow1
//        .onEach {
//            println("launchIn()2 - $it")
//        }
//        .launchIn(scope)

    scope.launch {
        flow1.collect {
            println("collect 1 - $it")
        }

        flow1.collect {
            println("collect 2 - $it")
        }
    }



    Thread.sleep(1000)
}