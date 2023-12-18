# 03. 주식 데이터 애플리케이션을 구현하며 실제 세계의 유즈케이스 분석

## 전제 사항
* 주식 데이터는 **주기적으로** 모조 엔드포인트를 호출함으로써 얻어진다.
* 애플리케이션 단에서, 이 데이터를 가져오는 로직을 여러 개의 컴포넌트로 나누어서 구현
  * Data Source : expose a flow of stocks in the view module of this use case => Flow<Stock>
  * ViewModel : observe the flow in order to get updated whenever we have new data about the stock prices => LiveData<UiState>
    * 새로운 데이터를 가져올 때마다, 뷰모델에서는 라이브데이터 프로퍼티를 업데이트(LiveData<UiState>)
  * Activity : 뷰모델의 UiState 라이브데이터 프로퍼티를 관찰하여, 새로운 주식 데이터를 UI에 렌더링한다.

## Flow Builders
* 계속적으로 여러 데이터를 발행할 수 있는 stream을 만들기 위하여 필요

1. flowOf<T>(vararg elements: T) : T 타입의 데이터를 발행하는 스트림을 만들겠다는 의미
   * 발행하고자 하는 데이터는 모두 같은 타입이어야 한다. 예를 들어, 문자열과 정수를 함께 발행할 수 없다. 굳이 발행해야 한다면, 발행 타입을 Any로 정해주어야 한다.
     * `val anyTypeFlow = flowOf<Any>(1, "a")` => 가능은 하지만 Type Safety를 잃게 된다.
   * produces values from the specified vararg-arguments.

* 해당 flow 객체를 관찰(collect)하는 방법
  * **터미널 연산자**를 호출해야 관찰 시작(모든 flow 객체가 그렇다)
  * ex) collect(suspend function)

```
fun main() = runBlocking { 
    // flowOf()
    // 매개변수로 넣은 인자들이 발행된다.
    val firstFlow = flowOf<Int>(1, 2, 3, 4, 5).collect { emittedValue ->
        println("$emittedValue")
    }
}
```

* cf) runBlocking 대신, 코틀린 1.3에서부터 main 함수에 대해서도 suspend function을 제공한다.
* **일반 main 함수에 runBlocking을 같이 활용하는 것과 같은 효과**
```
suspend fun main() { 
    // flowOf()
    // 매개변수로 넣은 인자들이 발행된다.
    val firstFlow = flowOf<Int>(1, 2, 3, 4, 5).collect { emittedValue ->
        println("$emittedValue")
    }
}
```

2. asFlow 확장함수 : 변환에 초점
* 사용될 수 있는 것들
  * Iterable 자료형들
  * Sequence
  * Array
  * Range // ex) (1..3) => IntRange, LongRange...

```
listOf("A", "B").asFlow().collect { emittedValue ->
    println(emittedValue)
}
```

* 한계 : 단지 하나/그 이상의 개수의 값을 flow로 변환할 뿐

3. flow() : much more flexible
`public fun <T> flow(@BuilderInference block: suspend FlowCollector<T>.() -> Unit): Flow<T> = SafeFlow(block)`
* contains suspendable lambda block
  * we can call not only regular functions but suspend functions

```
flow { 
    delay(2000)
    emit("item emitted")
}.collect { emittedValue ->
    println(emittedValue)
}
```

* 이 flow() 빌더는 다른 빌더 함수들에 비해 **유연하다**
  * 데이터의 **발행 뿐만 아니라**, **추가적인 실행을 요하는 코드 역시 작성 가능하기 때문이다.**
  * 해당 빌더 내부에서 당연히 다른 flow를 collect할 수도 있다.(collect는 suspend function)
    * collect 받은 데이터를 다시 emit하는 용도 등으로 활용
```
val secondFlow = flowOf(1, 2, 3, 4, 6)

flow { 
    delay(2000)
    emit("item emitted")
    secondFlow.collect { emittedValue ->
        emit(emittedValue)
    }
}.collect { emittedValue ->
    println(emittedValue)
}
```

* cf) 기존의 flow 값들을 flow() 빌더 함수에서 emit하는 방법 : emitAll(flow변수명) 
  * 더욱 간결하다.
```
val secondFlow = flowOf(1, 2, 3, 4, 6)

flow { 
    delay(2000)
    emit("item emitted")
    emitAll(secondFlow)
}.collect { emittedValue ->
    println(emittedValue)
}
```

## 언제 무엇을 쓰는 것이 적절할까?
* 만약 **고정**된 어떤 값이나 컬렉션을 flow로 변환하고자 한다면, flowOf() 혹은 .asFlow()를 활용
* 그것이 아니고 다른 함수를 통해 불러와야 하는 값을 발행해야 한다면, flow() 빌더 함수를 활용하는 것이 좋을 것이다


## 예시
* 발행 측
  * 5초마다 네트워크 요청이 이뤄지고 최신 데이터를 가져오게 된다. -> emit을 통해 발행
```
interface StockPriceDataSource {
    val latestStockList: Flow<List<Stock>>
}

class NetworkStockPriceDataSource(mockApi: FlowMockApi) : StockPriceDataSource {

    override val latestStockList: Flow<List<Stock>> = flow {
        while (true) {
            Timber.tag("Flow").d("Fetching current stock prices")
            val currentStockList = mockApi.getCurrentStockPrices()
            emit(currentStockList)
            delay(5_000)
        }
    }
}
```

* 수집 측 : ViewModel
```
class FlowUseCaseViewModel(
    stockPriceDataSource: StockPriceDataSource
) : BaseViewModel<UiState>() {

  val currentStockPriceAsLiveData: LiveData<UiState> = MutableLiveData()
  
  init {
      // runs whenever the new instance of viewmodel is created
      // collect from the flow data
      viewModelScope.launch {
          // 수집
          stockPriceDataSource.latestStockList.collect { stockList ->
            Log.d("Received item", "${stockList.first()}") // only the firstly emitted data(5초마다 계속 받지 않고 처음 한번만 받기 위함)
          }
      }
      
  }
}
```