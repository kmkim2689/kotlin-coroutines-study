## buffer()

### 키워드 : 데이터 하나를 소비하는 시간이 발행하는 시간보다 오래 결릴 경우 활용

> 팬케이크를 굽는 상황을 가정. 핵심은 여러 사람에게 나누어주어야 한다는 것이다.
> 
> 요약 : pancake를 구울 수 있는 프라이팬은 emitter, 다 만들어진 팬케이크를 놓을 수 있는 접시는 buffer에 비유 가능
> 팬케이크가 다 구워지고 접시로 넘어가는 과정에서 send() operator가 활용된다.(Channel의 연산자)

### 일반적으로 구현할 수 있는 사항
```
suspend fun main() = coroutineScope {

    val flow = flow {
        repeat(5) {
            println("Emitter : Start Cooking pancake $it")
            // 하나의 데이터를 발행하기까지 100ms 소요
            delay(100L)
            println("Emitter : Pancake $it is ready")
            emit(it)
        }
    }

    flow.collect {
        println("Collector : Start eating pancake $it")
        // 하나의 데이터를 소비하기까지 300ms 소요
        delay(300)
        println("Collector : Finished eating pancake $it")
    }
}
```

* 결과 : 하나를 소비하기 시작하려면(얻기 위해), 생산자가 앞에서 미리 만들어 놓을 수 있음에도 불구하고 소비자가 전의 데이터를 얻은 이후에야 새로운 데이터를 발행할 수 있다.  
* 즉, 팬케이크 하나에 대한 모든 생산과 소비가 **순차적**으로 일어난다.
```
Emitter : Start Cooking pancake 0
Emitter : Pancake 0 is ready
Collector : Start eating pancake 0
Collector : Finished eating pancake 0
Emitter : Start Cooking pancake 1
Emitter : Pancake 1 is ready
Collector : Start eating pancake 1
Collector : Finished eating pancake 1
Emitter : Start Cooking pancake 2
Emitter : Pancake 2 is ready
Collector : Start eating pancake 2
Collector : Finished eating pancake 2
Emitter : Start Cooking pancake 3
Emitter : Pancake 3 is ready
Collector : Start eating pancake 3
Collector : Finished eating pancake 3
Emitter : Start Cooking pancake 4
Emitter : Pancake 4 is ready
Collector : Start eating pancake 4
Collector : Finished eating pancake 4
```
```
만약 소비자가 팬케이크가 만들어지는 속도만큼(하나 당 100ms) 빠르게 가져갈 수 있다면, 이는 별 문제가 되지 않는다.
하지만, 소비자가 팬케이크가 만들어지는 속도보다 현저히 느린 속도로 가져간다면, 생산자의 시간 낭비가 발생하게 된다.
(ms)  100  200  300  400  500  600  700  800  900  1000
      0생산           0소비          1소비            2소비
                     1생산          2생산            3생산
-> 생산자는 더 빠르게 생산할 수 있음에도 불구하고 (100ms), 그보다 훨씬 느린 300ms 간격으로 생산하게 된다.
-> 생산자는 그만큼의 시간 낭비를 하고 있는 것.
```

### 목표 : 소비자의 소비 속도와 관계 없이 생산자는 생산자의 페이스대로 데이터를 발행할 수 있도록 하는 것
* 즉, 생산과 소비 간의 decoupling을 목표로 한다.
* buffer() operator
  * flow 변수에 활용

```
suspend fun main() = coroutineScope {

    val flow = flow {
        repeat(5) {
            println("Emitter : Start Cooking pancake $it")
            // 하나의 데이터를 발행하기까지 100ms 소요
            delay(100L)
            println("Emitter : Pancake $it is ready")
            emit(it)
        }
    }.buffer()

    flow.collect {
        println("Collector : Start eating pancake $it")
        // 하나의 데이터를 소비하기까지 300ms 소요
        delay(300)
        println("Collector : Finished eating pancake $it")
    }
}
```

* 결과
```
Emitter : Start Cooking pancake 0
Emitter : Pancake 0 is ready  -> 완료, 즉시 다음 것 만들기 시작
Collector : Start eating pancake 0(위와 동시에 출력되는 것이므로 무시)
Emitter : Start Cooking pancake 1 -> Finished eating pancake 0 이전에 바로 시작
Emitter : Pancake 1 is ready -> 완료, 즉시 다음 것 만들기 시작
Emitter : Start Cooking pancake 2 -> Finished eating pancake 1 이전에 바로 시작
Emitter : Pancake 2 is ready
Emitter : Start Cooking pancake 3
Collector : Finished eating pancake 0 -> 300ms 이후에야 섭취 완료. 4번째(pancake 3) 팬케이크가 만들어지는 와중에 섭취 완료
Collector : Start eating pancake 1 -> 다음 데이터에 대기
Emitter : Pancake 3 is ready
Emitter : Start Cooking pancake 4
Emitter : Pancake 4 is ready -> 생산자는 모두 생산 완료
Collector : Finished eating pancake 1 -> 이후 소비자의 소비만 이뤄짐. 
Collector : Start eating pancake 2
Collector : Finished eating pancake 2
Collector : Start eating pancake 3
Collector : Finished eating pancake 3
Collector : Start eating pancake 4
Collector : Finished eating pancake 4
```
```
emitter는 더 이상 소비자가 각각의 pancake을 소비하는 것을 기다리지 않는다.
```
```
만약 소비자가 팬케이크가 만들어지는 속도만큼(하나 당 100ms) 빠르게 가져갈 수 있다면, 이는 별 문제가 되지 않는다.
하지만, 소비자가 팬케이크가 만들어지는 속도보다 현저히 느린 속도로 가져간다면, 생산자의 시간 낭비가 발생하게 된다.
(ms)  100  200  300  400  500  600  700  800  900  1000
     0생산 1생산 2생산 3생산 4생산
          0소비            1소비           2소비 ... 
-> 앞의 예시와는 다르게, buffer를 활용하면 생산자가 4까지 생산하고, 소비자가 2까지 소비하는 데 시간이 확연히 단축됨을 확인 가능
-> 소비자는 생산 속도가 소비속도보다 빠른 것의 장점을 온전히 취할 수 있게 된다. 다음 것이 만들어지기까지 기다리지 않고 바로 소비할 수 있기 때문이다.
```

### 결국, flow의 buffer() operator는 생산자와 소비자가 concurrent하게 동작할 수 있도록 하는 연산자라고 볼 수 있다.
* **buffer()를 활용하지 않는 경우, 생산자와 소비자는 같은 코루틴에서 동작하기 때문에 생산과 소비가 순차적으로 발생한 것이다.**
* 원리 : **생산자와 소비자가 각각 다른 coroutine 스코프에서 동작**하도록 하는 것이다.
    * 여기에는 Channel의 send()라는 것이 활용된다.
    * buffer()를 활용하는 경우, 생산자 측에서 emit하는 경우 send() 메소드가 내부적으로 활용된다.

