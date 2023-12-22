# 14. StateFlow, SharedFlow

## flow에서 발행된 데이터를 ui에 렌더링하는 방법

### 방법 1. ViewModel에서 라이브데이터로 변환하여 액티비티가 라이브데이터를 관찰
```
Activity ---(observes)---> ViewModel(LiveData<UiState>)
```
* 액티비티가 '관찰' -> 렌더링
* 주의사항 : Antipattern - to use LiveData in other Layers than the UI Layer
  * ViewModel(UI레이어) 이외의 레이어에서 LiveData를 사용하는 것은 안티패턴
    * ex) 라이브데이터를 노출하는 Room Database
    * client에게 data change를 알리기 위한 목적으로 사용하지만, 안티패턴임
  * 왜 안티패턴인가?
    * 라이브데이터 객체를 관찰하고 그에 대한 변환 작업 등을 수행하는 것은 항상 **메인 스레드**에서 수행 되기 때문이다.
    * 따라서, 백그라운드 스레드에서 수행되어야 하는 작업들도 LiveData를 활용하면 메인 스레드에서 동작하기 때문에 안티패턴이다.
    * 따라서 다른 레이어에서 라이브데이터를 활용하면 안되고 flow 혹은 suspend function을 활용해야 한다.

### 방법 2. LiveData 대신 Flow를 노출시키는 방법
```
Activity ---(collects)---> ViewModel(Flow<UiState>)
```
* Activity가 flow를 collect하여 수집된 값으로 ui를 업데이트
* 장점
  * A Single type of observable data holder throughout the architecture
    * 굳이 livedata라는 추가적인 데이터 홀더를 사용할 필요가 없게 된다.
    * flow 객체를 livedata로 변환할 필요도 없음 -> 심리적 오버헤드를 낮춤
  * flow를 활용하면, 라이브데이터와는 달리 다양한 연산자들을 활용하여 데이터를 가공할 수 있다.
  * LiveData 사용과는 달리, viewmodel에 대하여 안드로이드 플랫폼으로부터의 종속성을 제거할 수 있다.
    * 이로 인하여, 테스팅이 더욱 용이해진다.(안드로이드 플랫폼이 필요하지 않음)

* 단점
  * View단에서 더 많은 보일러플레이트 코드가 필요

## View 단에서 flow를 활용하는 방법

### 1. Naive Approach
* expose a flow instead of LiveData in the VM
* before
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

* after

1. LiveData 형태를 Flow 형태로 변경한다.(asLiveData 터미널 연산자 제거)
```
class FlowUseCaseViewModel(
    stockPriceDataSource: StockPriceDataSource
) : BaseViewModel<UiState>() {

  val currentStockPriceAsFlow: Flow<UiState> = 
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
}
```

2. 액티비티에서, 더 이상 observe 메소드를 호출할 수 없다.(Flow이므로)
* observe 대신 collect를 사용
```
override fun onCreate() {
    // ...
    lifecycleScope.launch {
        viewModel.currentStockPriceAsFlow.collect { uiState ->
            // if (uiState != null) => livedata와는 달리, null check이 필요없다
            // UiState가 non-null 타입이기 때문 
            render(uiState)
        }
    }
}
```

* 이 방법의 문제점은 무엇인가?
  * 앱이 백그라운드에 있을 때 계속 flow 생산자가 돌아간다는 것
    * 계속해서 데이터를 발행함
  * 앱이 백그라운드에 있을 때에도 **액티비티**가 발행된 것을 받아 ui를 렌더링함
    * 앱이 백그라운드에 있음에도 불구하고, flow가 계속 데이터를 발행하기 때문
  * 다수의 컬렉터들이 다수의 flow를 만들어낼 수 있다는 특성으로 인하여 문제 발생의 소지가 있음 - 메모리 이슈
    * 같은 액티비티에서 다수의 컬렉터가 같은 flow를 구독하게 되면, 각각에 대해 요청이 이뤄지며 이 데이터는 공유되지 않는다.
    * 여러 collector들이 한 번 발행되는 데이터를 공유하는 것이 이상적이지, 각 collector를 위해 각각 발행이 이뤄지는 것은 비효율적이다.
  * configuration change 발생 시 flow가 재시작된다.
    * lifecycle scope을 사용하였기 때문.
      * onDestroy가 호출되어 중지되었다가 다시 onCreate가 호출되기 때문.

## repeatOnLifecycle()
* 위 코드의 문제점
  * 액티비티에서 launch하는 lifecycleScope이 앱이 백그라운드에 있을 때도 계속 돌아간다는 것이 문제의 원인이다.
    * 이것 때문에, 백그라운드에서도 flow가 계속 발행을 진행
  * lifecycleScope가 실행을 멈추는 유일한 때가 destroy되었을 때이기 때문에 문제가 되는 것

* 결국, flow 자체로는 안드로이드의 생명주기를 알 수 없기 때문에 이러한 문제점이 발생하는 것이다.
    * 따라서, flow가 생명주기를 인식할 수 있도록 추가적인 작업이 필요한 것이다.

### 이를 위한 수동적인 방법 : 라이프사이클 콜백마다 동작 시작 및 취소를 설정
  * onStart()에서 실행(백그라운드로 갔다가 다시 돌아오는 경우를 고려. onCreate는 백그라운드에서 돌아왔을 때 실행되지 않음. 백그라운드로부터 돌아왔을 때 onRestart -> onStart가 수행됨)
  * onStop()에서 취소(cancel. 백그라운드로 가는 경우를 고려. 백그라운드로 갔을 때 onDestroy는 실행되지 않는다.)
  * 하지만, 이러한 방법은 boilerplate code라는 점에서 문제
```
var job: Job? = null

// ...

override fun onStart() {
    super.onStart()
    
    job = lifecycleScope.launch {
        viewModel.currentStockPriceAsFlow.collect { uiState ->
            render(uiState)
        }
    }
}

override fun onStop() {
    super.onStop()
    
    job?.cancel()
}
```

### repeatOnLifecycle() : 더 나은 대안
* add dependency
`androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle-version`

* 매개변수
  * state: Lifecycle.State - 어떠한 생명주기 상태에서 시작할지
  * block : suspend CoroutineScope() -> Unit - pass the code that collects from flow

* **주의사항**
  * fragment에서는 다음과 같이 활용해야 한다.
  ```
  viewLifecycleOwner.lifecycleScope.launch {
      // fragment는 view의 생명주기가 따로 존재함...
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
  
      }
  }
  ```

* 코드
```
override fun onCreate() {
    // ...
    lifecycleScope.launch {
        // collection starts when started, ends when Stop state
        repeatOnLifecycle(state = Lifecycle.State.STARTED) 
        viewModel.currentStockPriceAsFlow.collect { uiState ->
            // if (uiState != null) => livedata와는 달리, null check이 필요없다
            // UiState가 non-null 타입이기 때문 
            render(uiState)
        }
    }
}
```

### flowWithLifecycle : repeatOnLifecycle의 또 다른 대안
* flow의 연산자 중 일종 - coroutine scope에서 동작 가능한 suspend function
* 매개변수
  * lifecycle: Lifecycle,
  * minActiveState: Lifecycle.State = Lifecycle.State.STARTED

```
override fun onCreate() {
    lifecycleScope.launch {
        viewModel.currentStockPriceAsFlow
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .collect { uiState -> render(uiState) }
    }
}
```

### 이 방법으로 해결할 수 있는 문제들
* 백그라운드에 갔을 때 flow의 수집을 멈춤
* 백그라운드로 이동 시 액티비티가 발행된 값을 받아 업데이트 하는 것을 멈춤

### 해결할 수 없는 문제들
* 여러 컬렉터들이 여러개의 flow를 만들어 수집하는 현상
  * 일반 Flow는 cold stream이기 때문이다.
* configuration change 발생 시 flow가 재시작

---

## SharedFlows
### Cold Flows vs Hot Flows
* Cold Flows
  * 수집 시작 시 활성화되고, 수집하는 코루틴의 취소가 발생할 때 비활성화 된다
    * 어떤 소비자가 수집하기 시작해야만 발행이 시작된다.
  * 각 컬렉터에 대해 독립적인 발행이 이뤄진다.

* Hot Flows
  * collector의 존재 여부와 상관 없이 활성화된다.
  * 또한, 모든 collector들은 같은 시점에 같은 값을 제공받는다.
  * 아래 코드를 보면, 소비자가 없음에도 println문이 실행되는 것을 보아하니 발행이 되는 것을 확인 가능
  * 이러한 특성 + 기본값이 필요 없다는 특징으로(StateFlow와는 달리 항상 어떤 값이 존재하지 않아도 됨), 이벤트로 구현하기 위해 사용 가능
  ```
  fun main() {
    
    val sharedFlow = MutableSharedFlow<Int>()
    
    val scope = CoroutineScope(Dispatchers.Default)
    scope.launch {
        // let sharedflow emit some value(s) : emit()
        repeat(5) {
            println("sharedflow emits $it")
            sharedFlow.emit(it)
            delay(200)
        }
    }
    
    Thread.sleep(1500)
  }
  ```
  ```
  sharedflow emits 0
  sharedflow emits 1
  sharedflow emits 2
  sharedflow emits 3
  sharedflow emits 4
  ```
  
* hot flow 발행 시작 후 짧은 시점 이후 수집 시작하도록 하기
```
fun main() {

    val sharedFlow = MutableSharedFlow<Int>()

    val scope = CoroutineScope(Dispatchers.Default)
    scope.launch {
        // let sharedflow emit some value(s) : emit()
        repeat(5) {
            println("sharedflow emits $it")
            sharedFlow.emit(it)
            delay(200)
        }
    }

    Thread.sleep(500)

    scope.launch {
        sharedFlow.collect {
            println("collected $it")
        }
    }

    Thread.sleep(1500)
}
```

```
sharedflow emits 0
sharedflow emits 1
sharedflow emits 2
sharedflow emits 3
// 3부터 수집됨
collected 3
sharedflow emits 4
collected 4
```

* 모든 collector가 같은 값을 공유 : 중복으로 발행된다는 cold flow의 문제점을 해결할 수 있다.
```
fun main() {

    val sharedFlow = MutableSharedFlow<Int>()

    val scope = CoroutineScope(Dispatchers.Default)
    scope.launch {
        // let sharedflow emit some value(s) : emit()
        repeat(5) {
            println("sharedflow emits $it")
            sharedFlow.emit(it)
            delay(200)
        }
    }

    scope.launch {
        sharedFlow.collect {
            println("collected $it in 1")
        }
    }

    scope.launch {
        sharedFlow.collect {
            println("collected $it in 2")
        }
    }

    Thread.sleep(1500)
}
```

* cold flow와는 달리, 여러 개의 collector가 있음에도 불구하고 각 emit은 한 번만 일어나고 emit된 값을 공유한다.
  * 발행된 값을 공유한다는 뉘앙스에서 SharedFlow라는 이름을 가지게 되었음
```
sharedflow emits 0
sharedflow emits 1
collected 1 in 1
collected 1 in 2
sharedflow emits 2
collected 2 in 2
collected 2 in 1
sharedflow emits 3
collected 3 in 1
collected 3 in 2
sharedflow emits 4
collected 4 in 1
collected 4 in 2
```

### SharedFlow를 활용한 문제 해결
* Hot Flow
  * 모든 collector가 공유 가능하도록 한다.
  * HotFlow 사용 시, cold flow의 문제점을 해결한다
    * 발행과 수집이 각각 독립적으로 발생하기 때문에 collect하는 coroutine scope이 취소될 때도 계속 flow는 동작한다는 문제점 해결

* Hot Flow를 만드는 방법
  1. MutableSharedFlow/MutableStateFlow 변수를 초기화하고, 필요한 값을 emit하는 방식(앞의 코드)
  2. **이미 존재하는 cold flow를 hot flow로 변환하는 메소드 활용**

* shareIn의 매개변수들
  * scope : Coroutine Scope
    * 왜 코루틴 스코프가 필요한가? : shareIn은 기존의 cold flow로부터 발행된 값들을 수집하여야 하기 때문에 내부적으로 코루틴을 실행시켜야 하기 때문
    * sharedflow는 cold flow로부터 받은 값들을 hot flow 특성에 맞게 재발행하는 역할을 해야 하기 때문
    * 따라서 SharedFlow는 수집자이자 발행자 역할을 동시에 수행한다고 볼 수 있음
      * cold flow --(수집)--> hot flow --(재발행)--> collector
    * 결국, sharedflow를 통하여 여러 collector에 동일한 데이터를 전달, 즉 데이터의 multicasting을 구현 가능한 것

  * started : SharingStarted
    * collect하고 재발행하는 코루틴(위의 매개변수)이 언제 시작되어야 하는지를 설정하기 위함
    * SharingStarted : remit(재발행) 대신 share라는 용어로 사용한다고 생각하면 됨. cold flow로부터 수집받은 데이터를 언제 collector로 재발행을 시작할 것인지를 명시
    * 기본적으로 세 가지가 주어지지만, 직접 제작할 수도 있다.
      * SharingStarted.Eagerly(Not Recommended) : sharedflow가 초기화된 즉시 - 수집자가 아무도 없다고 하더라도, 업스트림으로부터의 수집이 즉시 발생한다. 즉 sharedflow를 수집하고자 하는 소비자가 없다고 하더라도 즉시 수집 및 재발행 시작
        * collect하는 activity가 아직 초기화되지 않았다고 하더라도 즉시 시작
        * collect가 본격적으로 시작되기 전에 발행이 시작된다면 데이터가 유실될 수 있다.
        * 이 옵션의 다른 문제점은 모든 수집자가 수집을 멈추었을 경우에도(백그라운드에 이동했을 때) 계속 발행이 이뤄진다는 것이다.
      * SharingStarted.Lazily(Not Recommended) : Eagerly와 비슷한 동작
        * Eagerly와 유일한 차이점은 첫 collector가 수집을 시작할 때 발행이 시작된다는 것이다.
        * 이것은 적어도 첫번째 수집자는 hot flow에서 발행되는 모든 데이터를 수집할 수 있다는 것을 보장한다.
        * 다만 Eagerly와 마찬가지로 모든 수집자가 수집을 멈추었을 경우에 여전히 발행이 지속된다.
      * SharingStarted.WhileSubscribed(stopTimeoutMillis: Long)
        * 수집과 재발행을 첫 수집자가 수집을 할 때 시작
        * 또한, 마지막 collector가 수집을 멈출 때가 되어서야 sharedflow는 수집과 재발행을 멈춤
        * 즉 이름에서 알 수 있다시피, 첫 구독이 시작될 때 수집과 재발행이 시작되고, 마지막 구독이 끝날 때 수집과 재발행이 중단된다.
        * WhileSubscribed의 생성자 중 하나인 stopTimeoutMills이 configuration change에 대응할 수 있도록 하는 키 포인트이다.
          * 생명주기 컴포넌트가 destroy되고 다시 시작되기까지 hot flow는 계속 활성화 되어 있어야 한다.
          * 따라서 마지막 collector가 수집을 중단되고 다음 collector가 동작이 시작될 때까지 hot flow가 계속 동작할 시간을 설정함으로써, configuration change 이후에도 이어서 수집 및 재발행 진행 가능
          * 기본값은 0이기 때문에, 따로 설정을 해주지 않으면 configuration change에 대응할 수 없다.
          * 설정한 시간이 지나면, upstream flow의 발행이 중단된다.
          * 안드로이드 문서에 따르면 5초 권장
        * configuration change뿐만 아니라, 백그라운드로 넘어갔을 때의 상황도 대응 가능
        
    * replay : 기본값은 0
      * 가장 최근 n개의 발행값을 configuration change 등으로 인한 재수집 및 재발행이 시작될 때 수집받을 수 있다
      * 기본값이 0이므로, 만약 configuration change가 발생했을 때 다음 값을 발행받기 전까지 아무것도 수집받지 못하는 상태가 된다.

* ViewModel - shareIn operator를 pipeline 끝에 설정한다.
```

```

### StateFlow를 활용한 문제 해결
* StateFlow의 차별점
  * 코루틴 스코프 없이도 .value를 통해 최신 값을 collect할 수 있다.
  * StateFlow의 값을 설정하기 위하여, .value를 통해 set할 수도 있지만, thread safe를 달성하기 위하여 .update() 메소드를 사용할 수 있다.
    * StateFlow가 갖고 있는 값에 다수의 코루틴이 동시 접근하였을 경우 발생하는 문제 해결
    * 그 외 getAndUpdate, updateAndGet 등 존재
    * https://proandroiddev.com/managing-viewmodel-state-with-stateflow-preventing-race-conditions-dedaca6a8c24
  ```
  
  ```
  

## SharedFlow와 StateFlow의 차이점
* 초기값의 존재 여부
  * sharedflow는 없고 stateflow는 있어야 한다.
  * replay cache의 사이즈는 sharedflow는 커스터마이즈가 가능하나, stateflow는 반드시 1이다(최신 값만을 담기 때문)
  * **연속적으로 같은 값을 발행하는지의 여부 : sharedflow는 발행할 수 있으나 stateflow는 그럴 수 없다.**
    * SharedFlow는 1을 연속적으로 발행 가능 -> SharedFlow의 소비자들은 1을 두 번 수집하게 된다. 
    * 그러나, StateFlow의 경우 직전에 같은 값이 발행되었다면 또 발행되지 않는다. not notified -> distinctUntilChanged intermediate operator가 SharedFlow에 적용된 형태

* 언제 무엇을 사용해야 하는가?
  * 기본적으로 HotFlow를 사용해야 하는 상황이라면, 일단 StateFlow를 생각하라
    * State를 활용할 때 StateFlow가 더 효율적이다
    * StateFlow가 값을 읽고 쓰는 데 더 편리하다... non-suspending 방식으로(.value) 접근 가능하기 때문
  * 다만 연속적으로 같은 값을 발행해야 하는 경우 SharedFlow를 활용해야 할 것이다
    * 따라서 이벤트 구현과 관련이 깊음
    * 이벤트의 경우 같은 값을 발행하는 이벤트가 필요할 수 있기 때문