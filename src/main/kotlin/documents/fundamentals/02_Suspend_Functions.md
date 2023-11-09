## 2. Suspend Funtions

### suspend function
* 일반 함수 앞에 suspend 키워드를 붙여 만들 수 있음
* suspend 키워드를 발견하면, 해당 코드는 longer-running operation을 하는 것이라고 간주해볼 수 있음
  * 오래 걸리는 작업을 regular function으로 정의한다면, 해당 작업을 마칠 때까지 앱은 멈추게 될 것이며 유저는 아무 것도 하지 못하기 때문
* 이 함수는 코틀린의 coroutine machinery에 의해 메인 쓰레드에 대한 간섭 없이 정지되고 이어서 실행하도록 할 수 있음

### 특징
* suspend function은 coroutine scope 혹은 "또 다른" suspend function 내에서만 실행될 수 있다.
```
fun coroutine(number: Int, delay: Long) { // 오류 발생
    println("Coroutine $number starts working")
    delay(delay) // delay() is a suspend function!!!
    println("Coroutine $number ends")
}
```

* 일반 함수에서 suspend function을 실행시키고자 할 때에는, 새로운 Coroutine Scope이 필요하다.
  * 여기서는 async scope가 사용됨
```
fun main() = runBlocking {
    println("main starts")
    joinAll(
        async { coroutine(1, 500) },
        async { coroutine(2, 300) }
    )
    println("main ends")
}
```

* suspend function(A)은 내부에 또 다른 suspend function(B)이 호출될 때, (A는) 그 동작을 멈추게 된다.
  * 다른 suspend function이 실행되는 곳은 suspension point로서, suspend function의 정지가 일어남. -> 코드의 좌측에 멈춤 아이콘이 표출되는데, 이것은 매우 유용한 표식
