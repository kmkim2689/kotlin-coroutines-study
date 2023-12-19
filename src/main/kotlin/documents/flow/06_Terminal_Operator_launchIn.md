# 06. launchIn() terminal operator
```
public fun <T> Flow<T>.launchIn(scope: CoroutineScope): Job = scope.launch {
    collect() // tail-call
}
```
### 다른 터미널 연산자와는 다른 성격을 가진다.
* 무엇보다도, **suspend function이 아니라는** 것이 가장 큰 특징이다.
* 코루틴 스코프 내에서 값을 간단히 수집할 수 있도록 하는 연산자이다.
  * 코루틴스코프.launch { flow변수.collect() }와 같은 역할을 한다.
* 매개변수
  * coroutine scope : 스코프를 전달해줌으로써, 내부적으로 새로운 코루틴을 시작하도록 함. 해당 범위 안에서 flow의 값들을 수집
* 특징
  * onEach, onCompletion 등과 함께 주로 사용되곤 한다.
    * onEach : 발행되는 모든 값들을 다루기 위함
  * catch 연산자와도 함께 사용된다 : 업스트림 플로우 측에서 발생할 수 있는 예외에 대해 처리하기 위함
```
Terminal flow operator that launches the collection of the given flow in the scope. It is a shorthand for scope.launch { flow.collect() }.
This operator is usually used with onEach, onCompletion and catch operators to process all emitted values handle an exception that might occur in the upstream flow or during processing, for example:
flow
    .onEach { value -> updateUi(value) }
    .onCompletion { cause -> updateUi(if (cause == null) "Done" else "Failed") }
    .catch { cause -> LOG.error("Exception: $cause") }
    .launchIn(uiScope)

Note that the resulting value of launchIn is not used and the provided scope takes care of cancellation.
```

* 예시
```
fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val scope = CoroutineScope(EmptyCoroutineContext)

    flow1
        .onEach {
            println(it)
        }
        .launchIn(scope) // first value, 1, secondvalue, 2

    // 여기에 코루틴에서 수집을 마칠 때까지 기다려주는 코드를 작성해주지 않으면 결과 확인 불가
    Thread.sleep(1000)
}
```

### collect vs launchIn
* flow를 구독할 때, launchIn을 사용하는 것이 더우 가독성이 높다.
  * indentation이 더 적기 때문
* 하지만, launchIn은 syntactic sugar를 제공하지 않는다는 한계도 있음
* 그 외 차이점들
  * collect는 suspend function임과 달리, launchIn은 regular function
    * launchIn을 활용하는 경우, flow가 완료되기 전까지 코루틴이 기다려주지 않는다.
    * 반면, collect 활용 시, flow의 동작이 완료될 때까지 코루틴이 동작의 완료를 기다림
    * **따라서, 만약 2개의 launchIn 연산자가 사용된다면, 두 launchIn은 코루틴을 멈출 일이 없으므로 모든 작업이 병렬적으로 수행된다.**

* ex1) 같은 scope에 대해서 collect와 launchIn을 함께 사용하는 경우
```
fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val scope = CoroutineScope(EmptyCoroutineContext)

    flow1
        .onEach {
            println("launchIn()1 - $it")
        }
        .launchIn(scope)

    scope.launch { 
        flow1.collect {
            println("collect - $it")
        }
    }

    Thread.sleep(1000)
}
```
* 결과
```
first value
first value 
collect - 1
launchIn()1 - 1
second value
collect - 2 
second value
launchIn()1 - 2
```

* ex2) 같은 scope에 대해서 2개의 launchIn을 활용하는 경우
```
fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val scope = CoroutineScope(EmptyCoroutineContext)

    flow1
        .onEach {
            println("launchIn()1 - $it")
        }
        .launchIn(scope)

    flow1
        .onEach {
            println("launchIn()2 - $it")
        }
        .launchIn(scope)

    Thread.sleep(1000)
}
```
* 결과 : 동시다발적으로 두 flow의 수집이 발생
```
first value
first value
launchIn()2 - 1
launchIn()1 - 1
second value
launchIn()2 - 2
second value
launchIn()1 - 2
```

* ex3) collect가 2개 사용되는 경우
```
fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val scope = CoroutineScope(EmptyCoroutineContext)

    scope.launch {
        flow1.collect {
            println("collect 1 - $it")
        }

        flow1.collect {
            println("collect 2 - $it")
        }
    }



    Thread.sleep(1000)
}
```

* 결과 : 수집이 순차적으로 수행됨
```
first value
collect 1 - 1
second value
collect 1 - 2
---
first value
collect 2 - 1
second value
collect 2 - 2
```