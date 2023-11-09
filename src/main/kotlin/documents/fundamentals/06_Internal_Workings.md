## 6. Internal Workings

> 어떻게 코루틴을 활용하여 특정 루틴을 정지하고 특정 시점에 이어서 할 수 있는 것인가?

### What's Happening Under the Hood?

* 안드로이드 애플리케이션의 빌드 프로세스
  * .kt 확장자로 되어있는 안드로이드 코드 파일을 작성
  * 코틀린은 컴파일러 언어로, 코틀린으로 작성된 코드를 동작시키기 위해서는 컴파일 과정이 사전에 필요
    * 코틀린 컴파일러는 모든 .kt 확장자로 되어있는 파일들을 컴파일하여, .class 확장자 파일(=jvm bytecodes)로 변환
  * 안드로이드 빌드 프로세스를 거침(매우 복잡)
    * 안드로이드 빌드 프로세스는 코틀린 컴파일러가 생성한 .class 파일을 받아 다양한 자원들과 함께 패키지화하여 apk 파일로 만듦
  * 이 파일이 최종적으로 안드로이드 디바이스에서 동작할 수 있게 됨

* Coroutine의 동작 원리는, 코틀린 컴파일러가 코틀린 코드를 바이트코드(.class)로 컴파일 할 때와 관련되어 있음
  * 코틀린 컴파일러가 코틀린 코드를 받아와 바이트 코드로 변환

    * 컴파일 전
    ```
    fun main() {
        println("hello world")
    }
    ```
    
    * 자바 바이트 코드로 컴파일 이후
    ```
    public final static main()V
      L0
        LINENUMBER 4 L0
        LD "hello world"
        ASTORE 0
      L1
        ICONST_0
        ISTORE 1
      L2
        GETSTATIC ...
        ALOAD 0
      ...
    ```
  
  * 이러한 원리에 따라, 코틀린 컴파일러는 suspend function을 컴파일 할 때 섬세한 코드 변환을 진행한다.
    * 컴파일 전 suspend function
    ```
    suspend fun coroutine(number: Int, delay: Long) {
        println("Coroutine $number starts working")
        delay(delay) // Thread의 sleep 대신 delay 활용
        println("Coroutine $number ends")
    }   
    ```
    
    * 컴파일러가 변환한 후의 바이트 코드를 다시 코틀린으로 변환하면, 이러한 모습을 확인할 수 있음
    ```
    fun coroutine(number: Int?, delay: Long?, continuation: Continuation<Any?>) {
        when (continuation.label) {
            0 -> {
                println("Coroutine $number starts working")
                delay(delay)
            }
            1 -> {
                println("Coroutine $number ends")
                continuation.resume(Unit)
            }
        }
    }   
    ```
    
    * 무슨 변화가 일어났으며 이것은 무슨 의미인가?
    ```
    i) suspend keyword가 사라짐. 바이트 코드로 변환된 시점에, 일반 함수로 변환됨을 알 수 있음
    ii) 컴파일러가 새로운 매개변수를 추가 : continuation(Continuation 타입)
    iii) 함수의 본문은 when 문을 포함
    레이블의 속성에 따라, 실행되는 코드가 다름
    마지막 레이블의 실행이 끝나면, resume 메소드를 실행시킴으로써 코루틴을 호출한 다른 함수에게 코루틴의 실행이 끝났음을 통신하며 코루틴의 동작이 종료
    ```
    
    * 결국, suspend 함수는 실제로는 반드시 부분적으로 실행되고 있음을 알 수 있다.
    * continuation 매개변수는 두 가지의 역할
      * 콜백으로서의 역할 : resume()의 호출로, 해당 코루틴을 호출한 곳에 코루틴의 실행이 끝났음을 알림
      * state machine으로서의 역할 : 실행의 상태가 continuation 내부에 보존되어 있는 label에 의해 결정되기 때문 -> 분기처리
      
    > 즉, continuation의 상태에 따라 하나의 suspend function의 코드들이 부분적으로 실행되고, suspend 함수의 실행이 끝나면 resume을 통해 해당 코루틴을 호출한 함수에게 종료를 알린다