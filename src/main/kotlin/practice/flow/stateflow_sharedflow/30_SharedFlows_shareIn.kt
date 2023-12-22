package practice.flow.stateflow_sharedflow

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() {

    val sharedFlow = flow {
        repeat(5) {
            emit(it)
            delay(100)
        }
    }.shareIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
    ) // cold -> shared(hot) flow


    Thread.sleep(1500)
}