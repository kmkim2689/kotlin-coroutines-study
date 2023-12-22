package practice.flow.stateflow_sharedflow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun main() {

    val sharedFlow = MutableSharedFlow<Int>()

    val scope = CoroutineScope(Dispatchers.Default)
    scope.launch {
        // let sharedflow emit some value(s) : emit()
        repeat(5) {
            println("sharedflow emits $it")
            sharedFlow.emit(it)
            delay(200)
        }
    }

    scope.launch {
        sharedFlow.collect {
            println("collected $it in 1")
        }
    }

    scope.launch {
        sharedFlow.collect {
            println("collected $it in 2")
        }
    }

    Thread.sleep(1500)
}