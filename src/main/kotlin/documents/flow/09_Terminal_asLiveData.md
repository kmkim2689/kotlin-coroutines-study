# 09. .asLiveData() terminal operator

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

* 위의 코드 상에는 매우 치명적인 문제가 있다
* 뷰모델이 의존하는 data source 클래스에서 데이터를 가져올 때 로그를 표출하는 코드를 추가해본다.

```
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

* 이후, 애플리케이션을 실행해보고 홈 화면으로 이동(Stop 생명주기)했을 때,
  * 정상적인 상황이라면 백그라운드로 이동했을 때는 더 이상 네트워크로부터 데이터를 불러오지 않는 것이 정상이다. 
    * 하지만, **여전히 데이터가 불러와지는** 것을 확인할 수 있다.
  * 이는 매우 비효율적이다. 자원을 낭비하기 때문이다.

## 해결책
* 앱이 백그라운드로 이동했을 때, 해당 flow를 **취소**하는 것

### <권장 x> 1. Naive Approach : Job 프로퍼티를 만들어, 기존의 동작을 할당
* Job 타입의 변수로 만들어놓기
```
class FlowUseCaseViewModel(
    stockPriceDataSource: StockPriceDataSource
) : BaseViewModel<UiState>() {

  val currentStockPriceAsLiveData: LiveData<UiState> = MutableLiveData()
  
  // job property
  var job: Job? = null
  
  init {
        // launchIn()으로 끝나는 파이프라인(terminal 연산자로 끝나는 flow 파이프라인은 job이 된다)을 assign
      job = stockpriceDataSource.latestStockList
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

* flow의 동작을 취소하는 메소드를 추가로 만들어놓기
```
class FlowUseCaseViewModel(
    private val stockPriceDataSource: StockPriceDataSource
) : BaseViewModel<UiState>() {

  val currentStockPriceAsLiveData: LiveData<UiState> = MutableLiveData()
  
  // job property
  var job: Job? = null
  
  // start the flow
  fun startFlowCollection() {
      // launchIn()으로 끝나는 파이프라인(terminal 연산자로 끝나는 flow 파이프라인은 job이 된다)을 assign
      job = stockpriceDataSource.latestStockList
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
  
  // to cancel the flow
  fun stopFlowCollection() {
    // cancel()
    job?.cancel()
  }
 
}
```

* Activity 파일에서, 생명주기에 따라서 시작 혹은 중단
    * onStart()에서 시작 호출 => `viewModel.startFlowCollection()`
    * onStop()에서 취소 호출 => `viewModel.stopFlowCollection()`

### 2. asLiveData Terminal Operator -> 생명주기를 인식하는 LiveData의 특성을 활용하여, Flow를 LiveData로 변환하여 사용
* 1.의 방법은 의도대로 동작할 수 있도록 하지만, 몇가지 결함이 존재한다.
  * boilerplate codes
    * 뷰모델에서 job에 대한 변수를 만들고 파이프라인을 할당해야 하며, flow 동작 취소에 대한 코드도 작성해야 한다.
    * prone to errors!
    * 또한 액티비티 등의 코드에서도 생명주기에 따른 처리를 해주어야 한다.

  * **Configuration Change 발생할 때마다, flow가 cancel 된다.**
    * https://yeseul94.tistory.com/entry/%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C-%ED%94%84%EB%A1%9C%EA%B7%B8%EB%9E%98%EB%B0%8D-Next-Step-51-%EC%95%A1%ED%8B%B0%EB%B9%84%ED%8B%B0-%EC%83%9D%EB%AA%85%EC%A3%BC%EA%B8%B0
    * configuration change : onPause -> onSaveInstanceState -> **onStop** -> onDestroy -> onCreate -> **onStart** -> onRestoreInstanceState
    * onStop이 호출되는 시점에 cancel되도록 설정했기 때문에 취소되는 것...

* 이것을 해결할 수 있는 방법이 바로 asLiveData()
  * 코드가 간결해졌다.
```
class FlowUseCaseViewModel(
    stockPriceDataSource: StockPriceDataSource
) : BaseViewModel<UiState>() {

  val currentStockPriceAsLiveData: LiveData<UiState> = 
      stockpriceDataSource.latestStockList
        .map { stockList ->
            UiState.Success(stockList) as UiState // Cast 필수임!!!!!
        } // List<Stock> -> UiState. 이후 연산자인 downstream에서 UiState를 발행/사용할 수 있게 됨
        .onStart { uiState -> // List<Stock>이 아닌 UiState 형태를 발행 가능
            UiState.Loading
        } // remove onEach block.(asLiveData가 그 역할을 해줌)
        .onCompletion {
            // 취소가 되었는지 확인할 목적 => 백그라운드로 이동 시 flow가 취소되며, 이것이 호출됨
            println("flow cancelled")
        }
        .asLiveData() // directly converts flow into LiveData object.
}
```

## asLiveData() terminal operator의 특징
* 다른 terminal operator와는 달리, coroutine scope를 별도로 명시해주지 않거나 코루틴 내부에서 실행하지 않는다.
  * launchIn(), collect...

* asLiveData의 내부 동작
```
Creates a LiveData that has values collected from the origin Flow.
The upstream flow collection starts when the returned LiveData becomes active (LiveData.onActive). If the LiveData becomes inactive (LiveData.onInactive) while the flow has not completed, the flow collection will be cancelled after timeoutInMs milliseconds unless the LiveData becomes active again before that timeout (to gracefully handle cases like Activity rotation).
After a cancellation, if the LiveData becomes active again, the upstream flow collection will be re-executed.
If the upstream flow completes successfully or is cancelled due to reasons other than LiveData becoming inactive, it will not be re-collected even after LiveData goes through active inactive cycle.
If flow completes with an exception, then exception will be delivered to the CoroutineExceptionHandler of provided context. By default EmptyCoroutineContext is used to so an exception will be delivered to main's thread UncaughtExceptionHandler. If your flow upstream is expected to throw, you can use catch operator on upstream flow to emit a helpful error object.
The timeoutInMs can be changed to fit different use cases better, for example increasing it will give more time to flow to complete before being canceled and is good for finite flows that are costly to restart. Otherwise if a flow is cheap to restart decreasing the timeoutInMs value will allow to produce less values that aren't consumed by anything.
Params:
context - The CoroutineContext to collect the upstream flow in. Defaults to EmptyCoroutineContext combined with Dispatchers.Main.immediate
timeoutInMs - The timeout in ms before cancelling the block if there are no active observers (LiveData.hasActiveObservers. Defaults to DEFAULT_TIMEOUT.
```

```
public fun <T> Flow<T>.asLiveData(
    context: CoroutineContext = EmptyCoroutineContext,
    timeoutInMs: Long = DEFAULT_TIMEOUT
): LiveData<T> = liveData(context, timeoutInMs) {
    collect {
        emit(it)
    }
}
```

* liveData 빌더 함수를 활용하여 변환됨을 알 수 있다.
  * liveData 빌더는 코루틴 스코프이다 : LiveDataScope
  * 따라서, 내부에 suspend function을 호출 가능하며, 그 내부에 collect라는 terminal operator를 호출할 수 있게 된다.
  * 이러한 방식으로 변환되는 것
* asLiveData 및 liveData 빌더의 매개변수들
  * context : CoroutineContext => 기본적으로 정의되어 있음(EmptyCoroutineContext). 다만 이 매개변수를 통해 오버라이드 가능
  * timeoutInMs : livedata가 flow가 collect되기까지 최대로 기다릴 수 있는 시간을 가리킨다. 아무런 값도 넣지 않으면, **백그라운드로 이동 시 flow의 collect가 즉시 취소되지 않고 5초 있다가 취소된다. 기본값이 5초이기 때문**
    * configuration change 대응에 유리하다. configuration change에 소요되는 짧은 시간을 감당할 수 있게 되기 때문
* 결국, 내부적으로 코루틴이 구현되어 있기 때문에 asLiveData를 활용하면 코루틴 스코프를 명시할 필요가 없는 것이다.
