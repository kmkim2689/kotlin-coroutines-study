# 16. Channels

> Hot Flow와 유사한데, 모든 수집자에게 발행된 값이 수집되지 않고 한 수집자에게만 각각의 발행이 수집될 수 있는 형태
> 
> Low-level inter-coroutine communication primitive
> 
> flow & some flow operators are build on top of channels
* Each value is only consumed by a single subscriber => "fair"
  * 여러 개의 수집자가 있다면, 각 수집자가 수집하는 데이터의 양은 균일하다!
  * n개의 consumer가 있다고 하면, 첫 번째 값은 1번 소비자에게, 두 번째 값은 2번 소비자에게, n번째 값은 n번 소비자에게, n+1번째 값은 1번 소비자에게...
  * 유즈케이스 : 안드로이드 앱에서 단 한 번만 발생해야 하는 이벤트 구현 시, 오직 '단 하나'의 구독자가 존재하는 경우라면 채널을 활용하여 이벤트를 구현할 수 있다.
* 그러나, no typical use case in android applications...

```
suspend fun main() = coroutineScope {
    // channel을 생성하기 위해, produce 메소드 활용
    val channel = produce<Int> { 
        println("sending 10")
        // 발행을 위해 send 활용
        send(10)
        
        println("sending 20")
        // 발행을 위해 send 활용
        send(20)
    }
    
    launch { 
        channel.consumeEach { receivedValue ->
            println("consumer1 : $receivedValue")
        }
    }
    launch { 
        channel.consumeEach { receivedValue ->
            println("consumer2 : $receivedValue")
        }
    }
}
```