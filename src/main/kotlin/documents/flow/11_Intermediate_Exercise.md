## Requirements
```
class FlowUseCase2ViewModel(
    stockPriceDataSource: StockPriceDataSource,
    defaultDispatcher: CoroutineDispatcher
) : BaseViewModel<UiState>() {

    /*

    Flow exercise 1 Goals
        1) only update stock list when Alphabet(Google) (stock.name ="Alphabet (Google)") stock price is > 2300$
        2) only show stocks of "United States" (stock.country == "United States")
        3) show the correct rank (stock.rank) within "United States", not the world wide rank
        4) filter out Apple  (stock.name ="Apple") and Microsoft (stock.name ="Microsoft"), so that Google is number one
        5) only show company if it is one of the biggest 10 companies of the "United States" (stock.rank <= 10)
        6) stop flow collection after 10 emissions from the dataSource
        7) log out the number of the current emission so that we can check if flow collection stops after exactly 10 emissions
        8) Perform all flow processing on a background thread

     */

    val currentStockPriceAsLiveData: LiveData<UiState> = TODO()
}
```

* solution
```
class FlowUseCase2ViewModel(
    stockPriceDataSource: StockPriceDataSource,
    defaultDispatcher: CoroutineDispatcher
) : BaseViewModel<UiState>() {

    /*

    Flow exercise 1 Goals
        1) only update stock list when Alphabet(Google) (stock.name ="Alphabet (Google)") stock price is > 2300$
        2) only show stocks of "United States" (stock.country == "United States")
        3) show the correct rank (stock.rank) within "United States", not the world wide rank
        4) filter out Apple  (stock.name ="Apple") and Microsoft (stock.name ="Microsoft"), so that Google is number one
        5) only show company if it is one of the biggest 10 companies of the "United States" (stock.rank <= 10)
        6) stop flow collection after 10 emissions from the dataSource
        7) log out the number of the current emission so that we can check if flow collection stops after exactly 10 emissions
        8) Perform all flow processing on a background thread

     */

    val currentStockPriceAsLiveData: LiveData<UiState> = stockPriceDataSource.latestStockList
        .filter {
        
        }
}
```

```
val currentStockPriceAsLiveData: LiveData<UiState> = stockPriceDataSource.latestStockList
    .withIndex()
    .onEach { indexedValue ->   
        println("${indexedValue.index + 1}")
    }
    .map { 
        it.value
    }
    .filter { stocks ->
        val googlePrice = stocks.find { stock -> stock.name == "Alphabet (Google)" }?.currentPrice ?: return@filter false
        googlePrice > 2300
    }
    .map { stocks ->
        stocks.filter {
            it.country == "United States" && it.name != "Apple" && it.name != "MicroSoft"
        }
    }
    .map { stocks ->
        stocks.mapIndexed { index, stock ->
            stock.copy(rank = index + 1)
        }
    }
    .map { stocks ->
        stocks.filter { it.rank <= 10 }
    }
    .take(count = 10)
    .map {
        UiState.Success(it) as UiState
    }
    .onStart {
        UiState.Loading
    }
    .asLiveData(defaultDispatcher)
```