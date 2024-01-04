## buffer()

### 공식 문서 정리(IDE 상에서 buffer()를 타고 들어갔을 때 나오는 설명)
* Buffers flow emissions via channel of a specified capacity and runs collector in a separate coroutine.
  * 보통 flow는 순차적으로 진행된다.(발행 완료 -> 소비 시작) 이는 생산자와 소비자가 같은 코루틴에서 동작하기 때문이다.
  ```
  flowOf("A", "B", "C")
    .onEach  { println("1$it") }
    .collect { println("2$it") }
  ```
  * flowOf + onEach(생산자 + 중간 연산자)와 collect(소비자) 연산자는 "Q"라는 이름의 같은 코루틴에서 실행된다.
  * Q 코루틴 : -->-- [1A] -- [2A] -- [1B] -- [2B] -- [1C] -- [2C] -->--
  * 따라서 여러 개의 연산자로 이뤄진 파이프라인의 경우, 해당 파이프라인의 전체 실행 시간은 전체 연산자의 실행시간의 합이라고 봐야 한다.
  * 반면, 두 연산자 사이에 buffer() 연산자를 활용하면, 두 연산자 사이에 버퍼가 형성된다.
    * 두 연산자는 buffer를 경계로 서로 다른 코루틴에서 동작한다. 
    * 버퍼는 Channel을 활용하여 형성된다.
    * 이를 통하여, 두 연산자는 concurrently하게 동작할 수 있게 된다.
    * 결론적으로, flow 전체 파이프라인의 전체 실행 시간을 절약할 수 있다.
  * 매개변수
    * capacity: Int
    * onBufferOverflow: BufferOverflow

### 키워드 : 데이터 하나를 소비하는 시간이 발행하는 시간보다 오래 결릴 경우 활용

> 팬케이크를 굽는 상황을 가정. 핵심은 여러 사람에게 나누어주어야 한다는 것이다.
> 
> 요약 : pancake를 구울 수 있는 프라이팬은 emitter, 다 만들어진 팬케이크를 놓을 수 있는 접시는 buffer에 비유 가능
> 팬케이크가 다 구워지고 접시로 넘어가는 과정에서 Channel이라는 것이 활용되며, send() operator가 활용된다.(Channel의 연산자)

```
fun <T> Flow<T>.buffer(capacity: Int = DEFAULT): Flow<T> = flow {
    coroutineScope { // limit the scope of concurrent producer coroutine
        val channel = produce(capacity = capacity) {
            collect { send(it) } // send all to channel
        }
        // emit all received values
        channel.consumeEach { emit(it) }
    }
}
```

* Channel에 대하여 : https://onlyfor-me-blog.tistory.com/756

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
    * 두 개의 다른 코루틴 스코프에서 동작하는 생산자와 소비자 간 "통신"을 가능하게 해주는 것이 바로 Channel이다.
    * 여기에는 Channel의 send()라는 것을 활용하여 생산자 -> 소비자로의 데이터 이동이 발생
    * buffer()를 활용하는 경우, 생산자 측에서 emit하는 경우 send() 메소드가 내부적으로 활용된다.

### Backpressure : 하지만, buffer의 크기에는 제약이 있기 마련이다. -> 이로 인한 overflow 문제
> 만약 buffer가 다 차게 된다면, 어떤 일이 일어나는가?
> Backpressure : buffer가 다 차게 되어 생산자의 데이터 발행에 차질이 생기도록 하는 현상 

* buffer()의 매개변수
  * capacity : size of the buffer -> 직접 설정 가능
    * 기본적으로, "BUFFERED"라는 상수로 설정된다.
    * 이는 64를 의미
    * 즉, 위의 예시대로 생각해보면 64개의 팬케이크를 접시(buffer)에 보존할 수 있는 것
  * onBufferOverflow

* 어떠한 상황에서 buffer가 다 차게 되는가?
  * 유일한 경우 : emitter가 데이터를 발행하는 속도가 collector보다 데이터를 소비하는 속도보다 훨씬 빠른 경우
    * 이러현 경우를 Backpressure라고 부른다.

* flow에서 Backpressure가 발생하면?
  * onBufferOverflow에서 설정된 대로 동작한다.
  * 기본값은 BufferOverflow.SUSPEND
    * 버퍼에 공간이 생길 때까지 발행을 중단한다는 의미

### Overflow strategies
1. SUSPEND
```
suspend fun main() = coroutineScope {

    val flow = flow {
        repeat(5) {
            val idx = it + 1
            println("Emitter : Start Cooking pancake $idx")
            // 하나의 데이터를 발행하기까지 100ms 소요
            delay(100L)
            println("Emitter : Pancake $idx is ready")
            emit(idx)
        }
    }.buffer(
        capacity = 1, // 극단적인 상황 연출을 위하여 buffer size를 1로 설정
        onBufferOverflow = BufferOverflow.SUSPEND // 버퍼가 찬 상태에서 새로운 데이터 발행 시도 시, 버퍼에 공간이 생길 때까지 대기
    )

    flow.collect {
        println("Collector : Start eating pancake $it")
        // 하나의 데이터를 소비하기까지 300ms 소요
        delay(300)
        println("Collector : Finished eating pancake $it")
    }
}
```

```
Emitter : Start Cooking pancake 1
Emitter : Pancake 1 is ready
Collector : Start eating pancake 1
Emitter : Start Cooking pancake 2
Emitter : Pancake 2 is ready
Emitter : Start Cooking pancake 3
Emitter : Pancake 3 is ready
Collector : Finished eating pancake 1
Collector : Start eating pancake 2
Emitter : Start Cooking pancake 4
Emitter : Pancake 4 is ready
Collector : Finished eating pancake 2
Emitter : Start Cooking pancake 5
Collector : Start eating pancake 3
Emitter : Pancake 5 is ready
Collector : Finished eating pancake 3
Collector : Start eating pancake 4
Collector : Finished eating pancake 4
Collector : Start eating pancake 5
Collector : Finished eating pancake 5
```

* diagram 
<img src="https://mail.google.com/mail/u/0?ui=2&ik=eebbf4c2e1&attid=0.1&permmsgid=msg-a:r-7472852611764892203&th=18cd55535fadac3c&view=fimg&fur=ip&sz=s0-l75-ft&attbid=ANGjdJ_tNcwDCN44ajspGVVbhE1grbxFiGaOhak-7ds6jcF6r-rc_WR65kxwtdIgzeOH9Tapp56eT6j4dIqm6W37XuvK5j6r22tL8PJRxtPI3v3_tTXHU4n6LePLCH4&disp=emb&realattid=18cd554f6c2f07903751">

2. DROP_OLDEST
* emitter는 suspend되지 않는다.
* 버퍼가 모두 차있는 상태에서 새로운 데이터의 발행이 시도된다면, 
* 소비가 완료될 때까지 기다리지 않고 버퍼에 가장 오래 있었던 데이터부터 drop한다.

3. DROP_LATEST
* emitter는 suspend되지 않는다.
* DROP_OLDEST와 반대
* 새로운 데이터가 버퍼로 들어오려고 할 때, 최근에 들어온 것부터 Drop한다.

### Backpressure를 다루는 다른 방식 : 버퍼의 크기를 무제한으로 두는 것
* capacity 매개변수를 UNLIMITED로 설정
* No suspension, No drop
* 메모리 부족 이슈가 발생할 우려가 있으므로 함부로 사용하는 것은 지양해야 한다.
  * 특히 생산은 이뤄지지만 소비가 이뤄지지 않는 시나리오에서는 이 방법이 치명적일 수 있음
```
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
```
```
suspend fun main() = coroutineScope {

    val flow = flow {
        repeat(5) {
            val idx = it + 1
            println("Emitter : Start Cooking pancake $idx")
            // 하나의 데이터를 발행하기까지 100ms 소요
            delay(100L)
            println("Emitter : Pancake $idx is ready")
            emit(idx)
        }
    }.buffer(
        capacity = UNLIMITED
    )

    flow.collect {
        println("Collector : Start eating pancake $it")
        // 하나의 데이터를 소비하기까지 300ms 소요
        delay(300)
        println("Collector : Finished eating pancake $it")
    }
}

```