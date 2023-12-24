# 14. Cancellation

> Coroutine과 Flow에서의 취소에서 핵심은 취소 시 관련 코드들은 협력적이어야 한다는 것이다.
> flow를 collect하는 코루틴이 취소된다면, 그 이후 그 코루틴에서의 작업들은 모두 취소되어야 한다는 것이다.
> flow를 수집하는 코루틴에서 비용이 많이 드는 flow 빌더 상의 코드가 계속 실행되지 않도록 적절히 cancel하는 작업이 필요하다.
> 예를 들어, flow에서 값을 발행하기 위해 에서 네트워크 통신이 이뤄져야 하는 경우, 그것을 수집하는 코루틴이 취소되면 해당 네트워크 작업이 이뤄지지 않도록 해야 효율적일 것이다.

## Flow의 cancel 여부를 확인하는 방법 : onCompletion 연산자 활용(lifecycle operator)
```
suspend fun main() {
    val scope = CoroutineScope(EmptyCoroutineContext)
    
    scope.launch { 
        intFlow()
            .onCompletion { throwable ->
                // to check if the flow got cancelled
                if (throwable is CancellationException) {
                    println("Flow Got Cancelled")
                }
            }
            .collect {
                println("collect $it")
            }
    }.join() // main function should wait until the coroutine completes
}

private fun intFlow() = flow { 
    emit(1)
    emit(2)
    emit(3)
}
```

* 요구사항 : 특정 값이 수신된다면 flow를 취소하고자 함
  * 방법 1 : flow로 값을 수집하는 Job(scope.launch로 생성된 Job)을 취소시키기
  * 방법 2 : 해당 scope 내부의 코드에서 직접 전체 scope의 동작을 취소하는 메소드 사용. cancel()
    * collect()에서 수신되는 값을 체크하여 조건문 처리
    * 2까지 수집되고, 취소 -> 3은 발행되지 않았음.
      * flow가 취소되면 이후에 관련된 모든 동작(이후의 값 발행 등)은 이뤄지지 않아야 하는 것이 맞다.
      *  코루틴이 취소되면 자동으로 flow 동작이 취소되어야 하는 것을 목표로 하고 있다.
        * 하지만 여기서 해당 flow를 취소하는 별도의 코드는 작성되지 않았다.
  ```
  suspend fun main() {
    val scope = CoroutineScope(EmptyCoroutineContext)

    scope.launch {
        intFlow()
            .onCompletion { throwable ->
                // to check if the flow got cancelled
                if (throwable is CancellationException) {
                    println("Flow Got Cancelled")
                }
            }
            .collect {
                println("collect $it")
                
                // 2일때 취소
                if (it == 2) cancel() // coroutine이 취소되면 수집중인 flow의 동작 역시 취소된다.
            }
    }.join() // main function should wait until the coroutine completes
  }
    
  private fun intFlow() = flow {
      emit(1)
      emit(2)
      emit(3)
  }
  ```

  * 문제점 : coroutine(scope)이 취소가 되었음에도 emit(3) 이전에 시간이 오래 걸리는 코드가 실행된다
  ```
  suspend fun main() {
      val scope = CoroutineScope(EmptyCoroutineContext)
  
      scope.launch {
          intFlow()
              .onCompletion { throwable ->
                  // to check if the flow got cancelled
                  if (throwable is CancellationException) {
                      println("Flow Got Cancelled")
                  }
              }
              .collect {
                  println("collect $it")
  
                  // 2일때 취소
                  if (it == 2) cancel()
              }
      }.join() // main function should wait until the coroutine completes
  }
  
  private fun intFlow() = flow {
      emit(1)
      emit(2)
  
      // 시간이 오래 걸리는 헤비한 작업을 가정하기 위함
      println("start")
      calculateFactorial(1_000)
      println("end")
  
      emit(3)
  }
  
  private fun calculateFactorial(number: Int): BigInteger {
      var factorial = BigInteger.ONE
      for (i in 1..number) {
          factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
      }
      return factorial
  }
  ```
  
  * 2 수집 시 코루틴이 취소되었음에도 factorial 연산이 이뤄졌음을 확인 가능
  * cooperative해야 한다는 코루틴 취소에 대한 요건을 위배
  ```
  collect 1
  collect 2
  start
  end
  Flow Got Cancelled
  ```
  
  * flow 빌더 내부에서의 해결방안 : currentCoroutineContext().ensureActive()
  ```
  Ensures that job in the current context is active.
  If the job is no longer active, throws CancellationException. If the job was cancelled, thrown exception contains the original cancellation cause. This function does not do anything if there is no Job in the context, since such a coroutine cannot be cancelled.
  This method is a drop-in replacement for the following code, but with more precise exception:
  if (!isActive) {
    throw CancellationException()
  }
  ```
  ```
  suspend fun main() {
      val scope = CoroutineScope(EmptyCoroutineContext)
  
      scope.launch {
          intFlow()
              .onCompletion { throwable ->
                  // to check if the flow got cancelled
                  if (throwable is CancellationException) {
                      println("Flow Got Cancelled")
                  }
              }
              .collect {
                  println("collect $it")
  
                  // 2일때 취소
                  if (it == 2) cancel()
              }
      }.join() // main function should wait until the coroutine completes
  }
  
  private fun intFlow() = flow {
      emit(1)
      emit(2)
  
      // getting the context of the coroutine - to figure out which context the flow is running in
      // currentCoroutineContext()
      currentCoroutineContext().ensureActive()
      
      // 시간이 오래 걸리는 헤비한 작업을 가정하기 위함
      println("start")
      calculateFactorial(1_000)
      println("end")
  
      emit(3)
  }
  
  private fun calculateFactorial(number: Int): BigInteger {
      var factorial = BigInteger.ONE
      for (i in 1..number) {
          factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
      }
      return factorial
  }
  ```
  ```
  collect 1
  collect 2
  Flow Got Cancelled
  ```
  
  * 문제점 : 만약 flow 내부에서 실행하는 함수가 진행되는 동안 flow를 수집하는 coroutine의 취소가 발생하면, 해당 함수는 실행을 멈추지 않게 된다.
    * 따라서, flow 빌더 함수 내부에서 cancel을 위한 코드를 작성하기보다는, cooperative한 취소를 위하여 내부 function에서 직접 취소
    * 이를 위하여, 해당 함수 역시 suspend function이어야 하며 코루틴 스코프 내부에서 실행되도록 해야 한다.
  ```
  suspend fun main() {
      val scope = CoroutineScope(EmptyCoroutineContext)
  
      scope.launch {
          intFlow()
              .onCompletion { throwable ->
                  // to check if the flow got cancelled
                  if (throwable is CancellationException) {
                      println("Flow Got Cancelled")
                  }
              }
              .collect {
                  println("collect $it")
  
                  // 2일때 취소
                  if (it == 2) cancel()
              }
      }.join() // main function should wait until the coroutine completes
  }
  
  private fun intFlow() = flow {
      emit(1)
      emit(2)
  
      // getting the context of the coroutine - to figure out which context the flow is running in
      // currentCoroutineContext()
      // currentCoroutineContext().ensureActive()
  
      // 시간이 오래 걸리는 헤비한 작업을 가정하기 위함
      println("start")
      calculateFactorial(1_000)
      println("end")
  
      emit(3)
  }
  
  private suspend fun calculateFactorial(number: Int): BigInteger = coroutineScope {
      var factorial = BigInteger.ONE
      for (i in 1..number) {
          factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
          ensureActive()
      }
      factorial
  }
  ```
  
  ```
  collect 1
  collect 2
  start
  Flow Got Cancelled // 함수의 동작이 다 끝나지 않고 취소됨 -> 취소와 관련하여 cooperative
  ```
  
## 다른 flow 빌더 함수를 활용하였을 때의 취소
* flowOf()로 위의 코드와 같은 역할을 하도록 작성
```
suspend fun main() {
    val scope = CoroutineScope(EmptyCoroutineContext)

    scope.launch {
        flowOf(1, 2, 3)
            .onCompletion { throwable ->
                // to check if the flow got cancelled
                if (throwable is CancellationException) {
                    println("Flow Got Cancelled")
                }
            }
            .collect {
                println("collect $it")
            }
    }.join() // main function should wait until the coroutine completes
}
```

* 결과
  * 2 수집 시 취소되지 않음
```
collect 1
collect 2
collect 3
```

* 이유
  * flowOf 빌더는 내부적으로 flow를 수집하는 코루틴이 여전히 활성화 되어있는지 체크하지 않기 때문이다.
  * 따라서, flow { } 빌더가 아닌 빌더 함수를 활용할 때는 수동적으로 현재 코루틴이 활성화되어있는지 체크하는 과정이 필요하다.

* onEach 연산자를 사용하여, 각 수집 때마다 코루틴이 돌아가고 있는지 체크하여 돌아가고 있을 때만 flow가 지속되도록 한다.

```
suspend fun main() {
    val scope = CoroutineScope(EmptyCoroutineContext)

    scope.launch {
        flowOf(1, 2, 3)
            .onCompletion { throwable ->
                // to check if the flow got cancelled
                if (throwable is CancellationException) {
                    println("Flow Got Cancelled")
                }
            }
            // 수집 때마다 수동으로 검사하여 코루틴 취소 시 수동으로 취소
            .onEach {
                println("receive $it in onEach")

                // if (!currentCoroutineContext().job.isActive) {
                //     throw CancellationException()
                // }
                ensureActive() // shortcut of the code above
            }
            .collect {
                println("collect $it")
                
                if (it == 2) cancel()
            }
    }.join() // main function should wait until the coroutine completes
}
```

```
receive 1 in onEach
collect 1
receive 2 in onEach
collect 2
receive 3 in onEach
Flow Got Cancelled // collect 3이 실행되지 않음 -> 취소되어 수집이 이뤄지지 않음.
```

* 위 취소 관련 코드에 대한 또 다른 shorthand가 존재한다. : cancellable() operator
```
suspend fun main() {
    val scope = CoroutineScope(EmptyCoroutineContext)

    scope.launch {
        flowOf(1, 2, 3)
            .onCompletion { throwable ->
                // to check if the flow got cancelled
                if (throwable is CancellationException) {
                    println("Flow Got Cancelled")
                }
            }
            // 수집 때마다 수동으로 검사하여 코루틴 취소 시 수동으로 취소
            // 각 발행 시마다 코루틴의 활성화 여부를 검사
            .cancellable()
            .collect {
                println("collect $it")

                if (it == 2) cancel()
            }
    }.join() // main function should wait until the coroutine completes
}
```