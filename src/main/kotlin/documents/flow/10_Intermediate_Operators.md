# 10. Intermediate Operators

* 참고 가능한 사이트 : flowmarbles.com

* executed from every emission of the flow
  * return/transform into the new type of flow

### 1. map
* 발행되는 각각(모든)의 값을 **다른 값** 혹은 **다른 타입**으로 변경
```
suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
        .map { it * 10 }
        .collect { collectedValue ->
            println(collectedValue)
        }
}
// 10, 20, 30, 40, 50
```

```
suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
        .map { "emission $it" }
        .collect { collectedValue ->
            println(collectedValue)
        }
}
// emission 1, emission 2, emission 3, emission 4, emission 5(to String)
```

* 파생되는 operators
  * mapNotNull : emit되는 값들 중 **map 내부에서 변환된 결과가 null이 아닌 것**들만 다운스트림으로 전달한다.
    * null인 것들은 다운스트림으로 전달 x
```
suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
        .mapNotNull {
            if (it % 2 == 0) null else it
        }
        .collect { collectedValue ->
            println(collectedValue)
        }
        // 1, 3, 5만 수집 가능
}
```

### 2. filter : predicate
* 발행되는 것들 중에서 특정 조건을 만족하는 것들만 수집할 수 있도록 한다.
* 블록 내부가 true가 되는 것들만 수집
```
suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
        .filter { it % 2 == 0 }
        .collect { collectedValue ->
            println(collectedValue)
        }

} // 1, 3, 5
```

* 파생되는 operator
  * filterNot(predicate to false) : filter와 반대로 조건문 대입 시 false가 되는 것만 collect할 수 있도록 한다.
  * filterNotNull : original emitted value가 null이 아닌 것만 collect하도록 한다.
  * filterIsInstance<R>() : R타입인 것만 collect할 수 있도록 한다.
    * 만약 특정 flow가 여러 데이터 타입으로 이뤄진 것이라면 유용하게 사용할 수 있음(Type Safety 달성)

### 3. take : one of the size limiting operators
* related to flow size
* 처음 collect되는 값들 n개만 취한다.
  * count 매개변수

```
suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
        .take(count = 3)
        .collect { collectedValue ->
            println(collectedValue)
        }

} // 1, 2, 3(처음 3개만)
```

* 파생 operators
  * takeWhile : 조건에 부합할 때(predicate)까지만 collect한다.
  ```
  suspend fun main() {
      flowOf(1, 2, 3, 4, 5)
          .takeWhile { it < 3 } // 처음으로 3 이상이 나올 때 수집 종료
          .collect { collectedValue ->
              println(collectedValue)
          }
  }
  ```
  
* filter vs takeWhile
  * **upstream flow를 cancel**하느냐의 여부의 차이
  * filter는 조건에 부합하지 않더라고 하더라도 끝까지 모두 순회
  * takeWhile은 조건에 부합하지 않는 순간 취소

### 4. drop : one of the size limiting operators
* 매개변수 count : 처음 발행되는 n개만큼 수집을 생략한다.
```
suspend fun main() {
    flowOf(1, 2, 3, 4, 5)
        .drop(count = 3) // 처음 발행되는 3개에 대한 수집을 드롭한다.
        .collect { collectedValue ->
            println(collectedValue)
        }
} // 4, 5
```

* 파생되는 operator : dropWhile
  * takeWhile과는 달리, 조건에 부합하지 않아도 upstream을 취소하지 않음

