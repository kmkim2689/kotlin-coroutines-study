### 5. Coroutines on MultiThread

> 코루틴은 여러 쓰레드에 걸쳐 동작할 수 있다.

* A coroutine can execute operations on different threads

```
fun main() = runBlocking {
    println("main starts")
    joinAll(
        async {
            threadSwitchingCoroutine(1, 500)
        },
        async {
            threadSwitchingCoroutine(2, 300)
        }
    )
    println("main ends")
}

suspend fun threadSwitchingCoroutine(number: Int, delay: Long) {
    println("Coroutine $number starts working ${Thread.currentThread().name}")
    delay(delay) 
    withContext(Dispatchers.Default) { // switching context(thread)
        println("Coroutine $number ends ${Thread.currentThread().name}")
    }
}
```

```
main starts
Coroutine 1 starts working main
Coroutine 2 starts working main
Coroutine 2 ends DefaultDispatcher-worker-1
Coroutine 1 ends DefaultDispatcher-worker-1
main ends
```

* 시작 statement 출력과 종료 statement 출력이 다른 Thread에서 일어남
* withContext 블록을 활용하여, 해당 코루틴 코드가 실행되는 Thread를 변경
* 왜 쓰는가?
  * Main Thread에서 하기에 많은 부담이 되는 작업(networking calls..)들을 백그라운드 Thread에서 할 수 있도록 하면 Main Thread의 부담을 줄일 수 있기 때문
  * 오랜 시간이 걸리는 작업을 UI 쓰레드에서 진행하면, UI가 멈추고 응답을 잘 하지 못하는 문제
  