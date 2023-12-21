# 12. Exception Handling

* Exception Handling in Flow is Much Simpler than that in Pure Coroutines

* Flow에서 발생할 수 있는 상황
  * 데이터를 발행중인 상황
  * Flow가 완료(complete)된 상황
    * 성공적으로 완료 : 모든 것들이 exception 없이 발행 완료된 상황
    * 예외가 발생한 것으로 인하여 강제로 취소된 상황

* 성공적으로 완료된 상황
  * 어떻게 성공적으로 완료된 것임을 알 수 있는가?
  * lifecycle operator : onCompletion { cause -> ... }
```
suspend fun main(): Unit = coroutineScope {
    
    launch { 
        val stocksFlow = stocksFlow()
        
        stocksFlow
            .onCompletion { cause -> 
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            .collect { stock ->
                println(stock)
            }
    }
}

private fun stocksFlow(): Flow<String> = flow { 
    emit("Apple")
    emit("Microsoft")
}
```

* 발행 도중 예상치 못한 예외가 발생한 상황
  * 결과 : 수집 과정에서 에러 발생으로 flow가 종료되고, crash 메시지가 표출된다.
```
suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()

        stocksFlow
            .onCompletion { cause ->
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            .collect { stock ->
                println(stock)
            }
    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    // 예외를 뱉음
    throw Exception("network request failed")
}
```

### 방법 1. try - catch문 활용
```
suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()

        try {
            stocksFlow
                .onCompletion { cause ->
                    if (cause == null) println("successfully completed")
                    else println("$cause")
                }
                .collect { stock ->
                    println(stock)
                }
        } catch (e: Exception) {
            println("handle exception in catch block")
        }
    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}
```

### 방법 2. Catch operator

* 예외적으로 종료되는 flow에 대해서, try - catch문 역시 완벽히 잘 동작한다.
  * 하지만, **flow**에서는 **예외 처리를 위한 특별한 연산자를 제공**한다.
  * catch operator : try-catch와는 달리, 선언적으로 예외를 처리하는 좋은 방법이다.
    * 특징 : 예외가 발생될 때, 중간연산자 map은 예외에 대해 동작하지 않는다.
    * 동작 결과
    ```
    inside map
    kotlin.Unit
    inside map
    kotlin.Unit
    java.lang.Exception: network request failed
    handle the exception in catch : java.lang.Exception: network request failed
    ```

```
suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()
            .map { 
                println("inside map")
            }
        
        stocksFlow
            .onCompletion { cause ->
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            // catch operator : **terminal operator 이전에 위치해야 한다.**
            // try - catch와 비슷하게, **업스트림** flow에서 발생하는 모든 예외를 처리하는 역할의 연산자 
            .catch { throwable ->  
                println("handle the exception in catch : $throwable")
            }
            .collect { stock ->
                println(stock)
            }

    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}
```

* catch operator의 특징
  * 파이프라인 내부에서 catch operator의 위치는 매우 중요하다.
  * catch operator는 오직 업스트림에서 발생한 예외에 대해서만 추적할 수 있다.
    * catch 이후에 예외 발생의 소지가 있다면, 그 앱은 crash된다.

```
suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()
            // the app crashes!!!
            .catch { throwable ->
                println("handle the exception in catch : $throwable")
            }
            .map {
                println("inside map")
            }

        stocksFlow
            .onCompletion { cause ->
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            .collect { stock ->
                println(stock)
            }

    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}
```

* catch operator의 특징 : flow collector 블록을 매개변수로 받기 때문에, 블록 내부에서 suspend function 호출 가능
  * 따라서 **예외 발생 시 다른 값의 발행이 필요하다면, 발행할 수 있다!!**
  * 이렇게 함으로써, flow의 다운스트림에서 값이 필요한 경우 제공할 수 있도록 한다.

```
suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()

        stocksFlow
            .onCompletion { cause ->
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            // catch operator : **terminal operator 이전에 위치해야 한다.**
            // try - catch와 비슷하게, **업스트림** flow에서 발생하는 모든 예외를 처리하는 역할의 연산자
            .catch { throwable ->
                println("handle the exception in catch : $throwable")
                emit("default stock")
            }
            .collect { stock ->
                println(stock)
            }

    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}
```

```
Apple
Microsoft
java.lang.Exception: network request failed
handle the exception in catch : java.lang.Exception: network request failed
default stock
```

### 방법 3. 완전히 다른 flow로 스위치

* fallback
  * 예외 발생 시, 다른 flow를 실행시키도록 하는 방법
    * 그런데 만약 새로운 flow에서도 오류가 발생하는 것에 대응하고 싶다면?
      * 상위 flow의 catch operator 밑에 **또 하나의 catch operator를 명시하여 대응한다.**

```
suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()

        stocksFlow
            .onCompletion { cause ->
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            // catch operator : **terminal operator 이전에 위치해야 한다.**
            // try - catch와 비슷하게, **업스트림** flow에서 발생하는 모든 예외를 처리하는 역할의 연산자
            .catch { throwable ->
                println("handle the exception in catch : $throwable")
                // 예외 발생 시 새로운 flow를 수집
                emitAll(fallbackFlow())
            }
            // 새로운 flow에서의 예외 발생 대비 : 밑에 또 하나의 catch 연산자 활용
            .catch {  throwable ->
                println("handle the exception in catch : $throwable")
            }
            .collect { stock ->
                println(stock)
            }

    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}

private fun fallbackFlow(): Flow<String> = flow {
    // 새로운 flow 실행
    emit("fallback stock")

    // 새로운 flow에서도 예외 발생 상황 가정
    throw Exception("exception in fallback flow!!!")
}
```

```
Apple
Microsoft
java.lang.Exception: network request failed
handle the exception in catch : java.lang.Exception: network request failed
fallback stock
handle the exception in catch : java.lang.Exception: exception in ***fallback flow!!!***
```

### 2, 3번 방식의 문제점
* terminal operator에서 발생하는 예외에 대해서는 대응하지 못한다.
```
suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()

        stocksFlow
            .onCompletion { cause ->
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            .catch { throwable ->
                println("handle the exception in catch : $throwable")
            }
            .collect { stock ->
                // crash :  exception in terminal operator
                throw Exception("exception in collect")
                println(stock)
            }
    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}

```

* 적절한 해결책 : terminal 연산자에서 예외 발생의 소지가 있다면, 중간 연산자로 대체하는 것!
  * onEach + launchIn(collect 대신 활용) 연산자 활용
  * catch의 업스트림에 onEach 연산자를 활용하여 terminal operator의 예외를 잡을 수 있음

```
suspend fun main(): Unit = coroutineScope {

    launch {
        val stocksFlow = stocksFlow()

        stocksFlow
            .onCompletion { cause ->
                if (cause == null) println("successfully completed")
                else println("$cause")
            }
            .onEach {
                throw Exception("exception in collect")
            }
            // catch operator : **terminal operator 이전에 위치해야 한다.**
            // try - catch와 비슷하게, **업스트림** flow에서 발생하는 모든 예외를 처리하는 역할의 연산자
            .catch { throwable ->
                println("handle the exception in catch : $throwable")
            }
            // 새로운 flow에서의 예외 발생 대비 : 밑에 또 하나의 catch 연산자 활용
            .launchIn(this)

    }
}

private fun stocksFlow(): Flow<String> = flow {
    emit("Apple")
    emit("Microsoft")

    throw Exception("network request failed")
}
```