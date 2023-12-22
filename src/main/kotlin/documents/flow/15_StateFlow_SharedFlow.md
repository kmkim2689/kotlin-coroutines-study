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

