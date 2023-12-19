# 07. flow into livedata

* 코드 읽기 쉽게 개선하기 : launchIn, onEach를 활용하여

## Example
```
class FlowUseCaseViewModel(
    stockPriceDataSource: StockPriceDataSource
) : BaseViewModel<UiState>() {

  val currentStockPriceAsLiveData: LiveData<UiState> = MutableLiveData()
  
  init {
      stockpriceDataSource.latestStockList
        .onEach { stockList ->
            currentStockPriceLiveData.value = UiState.Success(stockList)
        }
        .launchIn(viewModelScope)
  }
}
```