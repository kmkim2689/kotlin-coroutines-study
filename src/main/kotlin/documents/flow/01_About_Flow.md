# 01. What is a Flow?

> a **type** in the Kotlin Coroutine Library, introduced in 2019

* a **stream** of value's' that are computed **asynchronously**
    * asynchronous data stream - 비동기적인 데이터 스트림
    * 데이터 스트림이란? : **여러 값을 반환하는 것을 의미**

## Asynchronous Data Stream?(비동기적 데이터 스트림이란?)

### 예시 : 특정 정수에 대한 팩토리얼 값을 구하는 방법

* 방법1. 일반 함수 활용하기
  * 특징 : **오직 최종 결과 하나의 값**만을 반환한다.
  * 한계 : 팩토리얼이 계산되는 과정의 중간 값들을 반환할 수 없다
```
private fun calculateFactorialOf(number: Int): BigInteger {
    var factorial = BigInteger.ONE
    for (i in 1..number) {
        Thread.sleep(10)
        factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
    }
    return factorial
}
```

* 방법2. 리스트 형태로 반환하도록 하기
  * buildList api 활용(리스트 빌더 함수)
  * 실행시키고 실행 시간을 살펴보면 **데이터의 처리가 끝난 후에야** 같은 시간에 **한번에 모든 결과**들이 처리되어 출력됨을 알 수 있음
  * 한계 : 이것을 data stream이라고 볼 수 없음
    * 데이터 스트림의 특징은 **데이터가 발행될 때마다** 계속적으로 하나하나 받아오는 것(bit by bit, continuously)
  
```
fun main() {
    val startTime = System.currentTimeMillis()
    calculateFactorialOf(5).forEach {
        println("$it, time : $startTime")
    }
}

private fun calculateFactorialOf(number: Int): List<BigInteger> = buildList {
    var factorial = BigInteger.ONE
    for (i in 1..number) {
        Thread.sleep(10)
        factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
        // add to list
        add(factorial)
    }
}
```

* 방법4. Sequence 활용하기 - `Synchronous Data Stream`
  * Sequence란? Lazily하게 값들을 처리 가능하도록 한다.
  * sequence 빌더 함수 : sequence()
  * 결과를 확인해보면, 앞의 예시와는 달리 처리가 끝난 같은 시간이 아니라 하나하나 게산될 때마다 지속적으로 데이터의 흐름이 이어짐을 알 수 있음
  ```
  1 elapsed time : 54
  2 elapsed time : 67
  6 elapsed time : 83
  24 elapsed time : 98
  120 elapsed time : 113
  ready for the next work
  ```
  * 중간값들이 같은 시간(데이터의 산출이 모두 끝난 후)에 동시에 받아지는 것이 아님
  * 한계 : 이것은 훨씬 효율적이지만, 동기적으로 수행됨
    * main thread에서 수행되며, 값이 산출되고 발행되는 동안에 block
    * 위의 실행 결과에서 알 수 있듯, 모든 값들이 발행되고 나서야 다음 작업을 수행 가능
    * 특히 안드로이드에서 무거운 작업들을 이러한 방식으로 처리하면 ui를 블록하기 때문에, 이것을 사용하는 것은 지양해야함
```
fun main() {
    val startTime = System.currentTimeMillis()
    calculateFactorialOf(5).forEach {
        println("$it elapsed time : ${System.currentTimeMillis() - startTime}")
    }
    println("ready for the next work")
}

private fun calculateFactorialOf(number: Int): Sequence<BigInteger> = sequence {
    var factorial = BigInteger.ONE
    for (i in 1..number) {
        Thread.sleep(10) // CANNOT USE SUSPEND FUNCTIONS! ONLY ON MAIN THREAD
        factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
        // yield : add value to sequence
        yield(factorial)
    }
}
```

* 방법4. Flow를 활용한 비동기적 데이터 스트림으로 구현함으로써, 최종결과만 반환하는 것이 아니라 모든 중간값들을 반환하도록 함
  * main thread를 블록하지 않는 비동기 데이터 스트림
  * 결과 : sequence를 사용할 때와는 달리 "ready for the next work"가 먼저 호출됨(비동기적으로 처리)

```
fun main() = runBlocking {
    val startTime = System.currentTimeMillis()
    launch {
        calculateFactorialOf(5).collect {
            println("$it elapsed time : ${System.currentTimeMillis() - startTime}")
        }
    }
    println("ready for the next work")
}

private fun calculateFactorialOf(number: Int): Flow<BigInteger> = flow {
    var factorial = BigInteger.ONE
    for (i in 1..number) {
        delay(10)
        factorial = factorial.multiply(BigInteger.valueOf(i.toLong()))
        // yield : add value to sequence
        emit(factorial)
    }
}.flowOn(Dispatchers.Default)
```

### Suspend Function vs Flow
* Suspend function
  * asynchronous operations -> return a single value
* Flow
* emit()를 통해 여러 값들을 발행 가능