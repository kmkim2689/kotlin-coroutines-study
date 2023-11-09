## 1. What is a Coroutine?

### In Terms of "Terms"
> Co + Routine

### 일반적인 Routines이라는 것은?
  > a **sequence** of program instructions that **performs a specific task, packaged as a Unit**
  > That Unit then **can be used** in programs **wherever** that particular task should be performed
  * 흔히 Functions, Methods, Subroutines, Procedures으로 일컬어지는 것
  * 즉, Routine이라는 것은 특정 일을 수행하는 코드 베이스를 빌드하는 일반적인 블록

  * 예시 : 외부 시스템으로부터 응답을 기다리는 역할을 하는 루틴을 만듦
  ```
  fun main() {
    println("main starts")
    routine(1, 500)
    routine(2, 300)
    println("main ends")
  }
  
  // routine 생성 = kotlin에서의 일반적인 함수
  fun routine(number: Int, delay: Long) {
    println("Routine $number started to work")
    Thread.sleep(delay)
    println("Routine $number finished")
  }
  ```
  
  * 결과
  ```
  main starts
  Routine 1 started to work
  Routine 1 finished
  Routine 2 started to work
  Routine 2 finished
  main ends
  ```
  
  * 동작 방식(제어 흐름을 가지고 있는 주체의 변화에 따라)
    * main 함수가 초기에 control flow를 가짐
    * 첫 루틴(routine 함수)를 실행시키면, control flow는 routine 함수로 넘어가게 됨
      * routine 함수의 body 부분이 실행되고, 이것들이 '완전히' 실행을 마치고 나서야, control flow가 다시 main 함수로 넘어옴
    * 같은 일이 routine2에서도 실행
    
  > 이것이 바로 일반적인 routine이 실행되는 방식
  
### Coroutine은 무엇이 다른가?
* Routine의 동작과 비슷한 구조로 구현해보기

```
fun main() = runBlocking {
    println("main starts")
    joinAll(
        async { coroutine(1, 500) },
        async { coroutine(2, 300) }
    )
    println("main ends")
}

suspend fun coroutine(number: Int, delay: Long) {
    println("Coroutine $number starts working")
    delay(delay) // Thread의 sleep 대신 delay 활용
    println("Coroutine $number ends")
}
```

* 결과
  * 특이사항 : 일반적인 Routine과는 달리, 두 Coroutine 중 하나가 다 끝나지도 않았음에도 다른 코루틴의 동작이 시작되었음 
```
main starts
Coroutine 1 starts working
Coroutine 2 starts working
Coroutine 2 ends
Coroutine 1 ends
main ends
```

* 왜 그런 것인가? => 비동기와 관련
  * 보기에는 순차적으로 coroutine이라는 이름의 함수를 호출한 것으로 보임
  * main 함수가 첫 번째 coroutine을 실행시키고, 시작하는 statement를 출력
  * 그 후 제어 흐름이 **첫 번째 coroutine에 계속 주어지는 것이 아니라 다시 main으로 복귀**
  * main 함수는 두 번째 coroutine을 실행시키고 시작하는 statement 출력
  * 그 후 곧바로 제어 흐름이 다시 main으로 넘어오게 됨
  * 이후 3ms 후 두 번째 coroutine의 종료 statement 출력 / 5ms 후, 첫 번째 coroutine의 두 번째 종료 statement 출력

* 결론
  * Coroutine은 Coroutine끼리 이리저리 제어 흐름을 넘길 수 있음
  * "Co"는 cooperative를 가리킴 
  * routine은 일반적인 함수를 가리킴
  * 결국, 코루틴은 협력적으로 자기들끼리 제어 흐름을 넘길 수 있는 루틴
    * 협력적이라는 측면에서, 코루틴은 비선점형 멀티태스킹의 일종