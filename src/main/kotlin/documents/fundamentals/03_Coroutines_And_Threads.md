## 3. Coroutines and Threads

### 왜 같은 작업을 할 수 있음에도 불구하고 Thread가 아닌 Coroutine을 사용하는 것인가?

* Thread로 구현
```
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
```

* 왜 Coroutine을 써야 하는가?
  * Thread에는 효율성과 성능 측면에서 Coroutine에 비해 좋지 않기 때문
  * 각 Thread는 만들어지고 사용되는 데 많은 양의 메모리 자원이 필요하다.
  * 또한, Thread 간 제어 흐름 변경 역시 많은 자원들이 필요하다.
  * 반면 Coroutine의 경우, 같은 동시다발적 실행을 구현하지만 Thread와는 달리 여러 개를 만든다고 해도 새로운 Thread를 만들고 Thread 간 제어흐름을 변경할 필요가 없다.
  * 즉, 여러 개의 Coroutine이 같은 Thread 내에서 동작하기 때문에 매우 효율적인 것

  * 코루틴이 동작하고 있는 쓰레드의 이름으로 이 사실을 알 수 있음.
  ```
  fun main() = runBlocking {
    println("main starts")
    joinAll(
        async { coroutineWithThreadInfo(1, 500) },
        async { coroutineWithThreadInfo(2, 300) }
    )
    println("main ends")
    }
    
    suspend fun coroutineWithThreadInfo(number: Int, delay: Long) {
        println("Coroutine $number starts working ${Thread.currentThread().name}")
        delay(delay) // Thread의 sleep 대신 delay 활용
        println("Coroutine $number ends")
    }
  ```
  ```
  main starts
  Coroutine 1 starts working main
  Coroutine 2 starts working main
  Coroutine 2 ends
  Coroutine 1 ends
  main ends
  ```
  
* 따라서, Coroutine은 경량화된 Thread "같은 것"이라고 생각해볼 수 있다.
  * 아래의 코드는 100만개의 코루틴을 실행시키는 것인데, Thread로는 같은 작업을 하는 것은 컴퓨터에 많은 부하를 일으킨다.
  * 하지만, 코루틴으로 구현하면 큰 무리 없이 5초만에 100만개의 문자열을 출력함을 알 수 있다.
```
fun main() = runBlocking {
    repeat(1_000_000) {
        launch {
            delay(5000)
            println(".")
        }
    }
}
```

* 쓰레드로 같은 기능을 재현하면 다음과 같은 에러가 나타난다
  * Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
  * 시스템의 메모리 사용이 한도를 넘어서 추가적으로 더 이상 쓰레드를 만들 수 없게 됨
```
fun main() {
    repeat(1_000_000) {
        thread {
            Thread.sleep(5000)
            println(".")
        }
    }
}
```