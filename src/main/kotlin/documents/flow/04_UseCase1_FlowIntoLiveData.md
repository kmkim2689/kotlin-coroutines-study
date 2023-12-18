# 04. flow into livedata

## Example
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
                // into LiveData
                currentStockPriceLiveData.value = UiState.Success(stockList)
             
          }
      }
      
  }
}
```