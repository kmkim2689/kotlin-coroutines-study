# 13. Exception Transparency

> 요약 : flow collector의 다운스트림(collect)에서 예외가 발생하였을 때, 그 예외는 반드시 flow collector로 전이되어야 한다.
> 또한, 만약 다운스트림에서 발생한 예외가 업스트림(collector)에서 포착하지 못하는 것이라면, 해당 flow는 더 이상 추가적으로 값을 발행하면 안된다.
> 소비자 역시 값을 제공받는 것을 멈추어야 한다.
> 이것을 예방하는 좋은 방법은 flow collector에서 try-catch를 활용하는 대신 catch operator를 사용하는 것이다.

* the core concept when it comes to Exception Handling

* 아래의 코드를 실행하면 어떤 일이 일어나는가?
  * collect에서 exception이 throw되었기 때문에 앱이 crash될 것인가?
  * 해당 오류가 flow 빌더의 catch 블록에서 처리될 것인가?
```
suspend fun main(): Unit = coroutineScope {
    flow { 
        try {
            emit(1)
        } catch (e: Exception) {
            println("catch exception in flow builder")
        }
    }.collect { emittedValue ->
        throw Exception("Exception in collect")
    }
}
```

* 정답은 해당 예외가 flow 빌더의 catch 블록에서 처리된다는 것이다.
  * `catch exception in flow builder`

## Exception Transparency : Flow Under the Hood

* Flow Collector(emitter)와의 interaction
  * Flow 빌더 함수 내부에 있는 코드와 flow collector는 내부적으로 상호작용한다.
    * by a simple function call
  * 만약 emit()이 호출은 -> 내부적으로 해당 emit된 데이터에 대해 collect 블록의 실행을 야기한다.
  * 예를 들어, 위의 코드에서 single value 1이 발행되면, 1은 collect 블록으로 넘겨지게 된다.

* 결국, 아래와 같이 동작 => collect하는 곳의 코드는 결국 flow collector에서 가시적이다.
  * emit()의 각 호출은 collect의 호출을 야기
  * 내부적으로 inline된다.
```
suspend fun main(): Unit = coroutineScope {
    
    flow { 
        emit(1)
        emit(2)
        emit(3)
    }.collect {
        println("collect $it")
    }
}

// 위의 코드는 이렇게 동작한다.
val inlinedFlow = flow<Int> {
    println("collect 1")
    println("collect 2")
    println("collect 3")
}
```

## So, collect block is handled by the try-catch in the flow builder!
* emit의 호출 -> collect 블록의 호출
* 첫 번째의 코드에서, 
  * 1이 발행 -> collect의 실행 -> 예외를 발생시킴 -> 사실 내부적으로는 다운스트림인 collect의 코드는 flow collector 블록에 인라인되기 때문에 예외를 catch
* 즉, 핵심은 다운 스트림의 예외가 업스트림에서 처리된다는 것이다.
  * 문제점 : flow의 소비자로서, collect 블록에서 예외를 다루어야 함에도 업스트림에서 이미 예외를 처리하기 때문에 이것을 어떻게 다뤄야 할지?
  

### Exception Transparency Rules
1. 다운스트림에서 발생하는 예외는 반드시 collector(FlowCollector 블록)에게 전이되어야 한다.
2. 다운스트림에서 포착하지 못하는 예외가 발생하였을 때, flow는 다른 값을 추가로 발행할 수 없다.
   * 추가적인 값을 발행하면 안되고 즉시 종료되어야 한다.

* 아래의 코드는 Exception Transparency를 위배한다.
```
suspend fun main(): Unit = coroutineScope {
    flow {
        try {
            emit(1)
        } catch (e: Exception) {
            println("catch exception in flow builder")
            // 예외가 발생했음에도 추가적으로 발행 시도
            emit(2)
        }
    }.collect { emittedValue ->
        throw Exception("Exception in collect")
    }
}
```

* 해결책 : catch operator
```
suspend fun main(): Unit = coroutineScope {
    flow {
        emit(1)
    }.catch {
        println("in catch operator")
    }.collect { emittedValue ->
        throw Exception("Exception in collect")
    }
}
```

## retry() operator : 예외가 발생했음에도 불구하고 계속 값을 제공받아야 하는 경우 활용
* retry가 받는 블록 : Throwable을 매개변수로 가지는 람다함수
  * 어떤 오류인지 알려주기 위한 목적의 변수
  * **업스트림**에서 오류가 발생하였을 때만 호출된다.
* 블록 부분에서, boolean 값을 반환한다.
  * true를 반환한다면, 재시도하고 false를 반환한다면 종료한다.

```
suspend fun main(): Unit = coroutineScope {

    flow {
        repeat(5) { index ->
            delay(100) // network call
            
            if (index < 4) {
                emit("data")
            } else throw NetworkException("network failure")
        }
      // *** retries : 한 번의 collect 시도에서 3회 예외가 발생한다면, 더 이상 재시도하지 않고 종료한다.
      // 기본값은 최대값이다.(MAX_VALUE) -> (거의) 무한으로 시도
    }.retry(retries = 3) { cause ->
        // predicate : true일 때만 재시도
        // 즉시 재시도를 한다면, 서버에 무리가 가므로 delay를 두는 것이 권장된다.
        delay(1000)
        cause is NetworkException
    }.collect {
        println("collect $it")
    }
}

class NetworkException(override val message: String): Exception(message)
```

/**
* Retries collection of the given flow up to [retries] times when an exception that matches the
* given [predicate] occurs in the upstream flow. This operator is *transparent* to exceptions that occur
* in downstream flow and does not retry on exceptions that are thrown to cancel the flow.
*
* See [catch] for details on how exceptions are caught in flows.
*
* The default value of [retries] parameter is [Long.MAX_VALUE]. This value effectively means to retry forever.
* This operator is a shorthand for the following code (see [retryWhen]). Note that `attempt` is checked first
* and [predicate] is not called when it reaches the given number of [retries]:
*
* ```
* retryWhen { cause, attempt -> attempt < retries && predicate(cause) }
* ```
*
* The [predicate] parameter is always true by default. The [predicate] is a suspending function,
* so it can be also used to introduce delay before retry, for example:
*
* ```
* flow.retry(3) { e ->
*     // retry on any IOException but also introduce delay if retrying
*     (e is IOException).also { if (it) delay(1000) }
* }
* ```
*
* @throws IllegalArgumentException when [retries] is not positive.
  */
```
public fun <T> Flow<T>.retry(
    retries: Long = Long.MAX_VALUE,
    predicate: suspend (cause: Throwable) -> Boolean = { true }
): Flow<T> {
    require(retries > 0) { "Expected positive amount of retries, but had $retries" }
    return retryWhen { cause, attempt -> attempt < retries && predicate(cause) }
}
```

* 보다 섬세한 예외 처리 : retryWhen 
  * throwable 객체뿐만 아니라, 몇 번째 시도인지(attempt: Long)까지 정보를 얻을 수 있다.
```
public fun <T> Flow<T>.retryWhen(predicate: suspend FlowCollector<T>.(cause: Throwable, attempt: Long) -> Boolean): Flow<T> =
    flow {
        var attempt = 0L
        var shallRetry: Boolean
        do {
            shallRetry = false
            val cause = catchImpl(this)
            if (cause != null) {
                if (predicate(cause, attempt)) {
                    shallRetry = true
                    attempt++
                } else {
                    throw cause
                }
            }
        } while (shallRetry)
    } 
```