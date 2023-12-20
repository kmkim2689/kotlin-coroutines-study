# 08. Lifecycle Operators
> onStart, onCompletion

## onStart() operator
* flow 수집을 시작하기 '전'에 행할 액션을 정의 가능
  * 첫 아이템이 emit되기 전

## onCompletion()
* flow의 동작이 완료되고 나서 행할 액션을 정의
* 정상적인 종료뿐만 아니라 예외가 발생하여 종료될 때도 해당 액션이 수행된다.
* 매개변수 : cause(Throwable)를 매개변수로 가지는 function
  * Throwable 변수는 flow가 정상적으로 종료되었는지 혹은 오류가 발생했는지 알 수 있도록 한다.(if (throwable != null))
    * 만약 정상 종료 시, cause는 null
    * 그렇지 않으면, null이 아님
```
public fun <T> Flow<T>.onCompletion(
    action: suspend FlowCollector<T>.(cause: Throwable?) -> Unit
): Flow<T>
```

## 예시 코드
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

    flow1
        .onStart {
            println("started")
        }
        .onEach {
            println("$it")
        }
        .onCompletion { cause ->
            if (cause != null) {
                println("flow completed with exception : ${cause.message}")
                return@onCompletion
            }
            println("flow completed")
        }
        .launchIn(CoroutineScope(EmptyCoroutineContext))

}
```

## Pipelines - Flow Processing Pipeline과 관련된 lifecycle operator의 여러 기능들
* 반응형 프로그래밍을 활용하여 구현할 때,
  * 명령형 및 순차적 프로그래밍을 구현할 때와는 꽤나 다른 코드 스타일을 보인다.

* 위의 코드에서, 하나의 flow를 수집하는 과정에서 각각의 연산자들이 **순서대로** 실행되는 **파이프라인을 형성**함을 알 수 있다.
  * 이것을 **Flow Processing Pipeline**으로 부른다.

* 단, lifecycle operator(onStart, onCompletion)의 경우, 어느 순서로 나오든 상관 없이 각각 시작과 끝에서 수행된다.
  * 위의 코드와 같은 효과
  ```
  flow1
        .onStart {
            println("started")
        }
        .onCompletion { cause ->
            if (cause != null) {
                println("flow completed with exception : ${cause.message}")
                return@onCompletion
            }
            println("flow completed")
        }
        .onEach {
            println("$it")
        }
        .launchIn(CoroutineScope(EmptyCoroutineContext))
  ```
  
* 또한, lifecycle operator를 여러 개 구현할 수 있다.
```
flow1
        .onStart {
            println("started")
        }
        .onEach {
            println("$it")
        }
        .onStart {
            println("started 2")
        }
        .onCompletion { cause ->
            if (cause != null) {
                println("flow completed with exception : ${cause.message}")
                return@onCompletion
            }
            println("flow completed")
        }
        .launchIn(CoroutineScope(EmptyCoroutineContext))
```
* 결과
  * 두 번째 onStart가 먼저 호출되었다는 것이 흥미로운 점이다
```
started 2
started
```

* lifecycle operator 블록 내부에서, emit도 가능하다.
  * 기존 flow에서 발행될 데이터 이외에 시작 전/끝나고 난 이후에 추가적으로 발행이 필요하다면, Flow 처리 파이프라인에서 새로운 아이템을 받아올 수 있도록 할 수 있다.

## Upstream, DownStream : two important terms for flow processing pipelines
* Upstream : 파이프라인에서 특정 연산자보다 위에 위치하는 모든 연산자들
* Downstream : 특정 연산자보다 아래에 위치하는 모든 연산자들

* 알아두기 : 대부분의 연산자들은 downstream에 영향을 준다.
  * 예외 : flowOn(scope) 등등

* 상황 예시 : 만약, 본래 T라는 타입의 데이터들이 반환되는 스트림을 R이 반환되도록 변환시키고 싶다면?
  * 추가 조건 : flow를 수집하기 전, 로딩 ui state 데이터를 발행해야 한다. 이는 기존의 flow가 반환하던 list와 다름

* 주어진 flow 객체 : List<Stock> 형태로 발행됨

* 기존 수집 코드
```
class FlowUseCaseViewModel(
    stockPriceDataSource: StockPriceDataSource
) : BaseViewModel<UiState>() {

  val currentStockPriceAsLiveData: LiveData<UiState> = MutableLiveData()
  
  init {
      stockpriceDataSource.latestStockList
        .onEach { stockList -> // List<Stock>
            currentStockPriceLiveData.value = UiState.Success(stockList)
        }
        .launchIn(viewModelScope)
  }
}
```

* 수정 : map이라는 중간 연산자를 활용하여, onStart 이전에 위치시킴으로써, 다운스트림의 onStart 역시 변형된 데이터 형태를 쉽게 발행하여 이후 활용할 수 있도록 한다.
  * map은 컬렉션 연산자와 비슷하게 변환의 역할을 해줌.
  * **업스트림인 map이 다운스트림인 onStart에 영향을 미침**으로써, 본래는 onStart가 리스트 형태를 발행해야 하는 것을 UiState를 발행할 수 있도록 한다.
```
class FlowUseCaseViewModel(
    stockPriceDataSource: StockPriceDataSource
) : BaseViewModel<UiState>() {

  val currentStockPriceAsLiveData: LiveData<UiState> = MutableLiveData()
  
  init {
      stockpriceDataSource.latestStockList
        .map { stockList ->
            UiState.Success(stockList) as UiState // Cast 필수임!!!!!
        } // List<Stock> -> UiState. 이후 연산자인 downstream에서 UiState를 발행/사용할 수 있게 됨
        .onStart { uiState -> // List<Stock>이 아닌 UiState 형태를 발행 가능
            UiState.Loading
        }
        .onEach { uiState -> // 비로소 UiState 형태를 활용
            currentStockPriceLiveData.value = uiState
        }
        .launchIn(viewModelScope)
  }
}
```
