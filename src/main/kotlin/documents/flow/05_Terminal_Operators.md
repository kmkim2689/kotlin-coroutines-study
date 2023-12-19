# 05. Terminal Operators

## Operators
* Flow 객체를 만들어놓기만 한다면, flow 블록 내부의 코드는 아무것도 실행되지 않는다.
  * 선언만으로는 생산자의 코드가 아무것도 실행되지 않음
  * 프린트 함수 실행 x
```
suspend fun main() {
    
    // just a declaration
    val flow1 = flow {
        delay(100)
        
        println("first value")
        emit(1)
        delay(100)
        
        println("second value")
        emit(2)
    }
}
```

* 이러한 flow builder의 특징은, listBuilder(buildList...) 함수와 차이를 보인다.
  * **선언만으로도 내부 코드가 실행**됨(프린트 함수가 실행)
```
val list1 = buildList { 
    add(1)
    println("first value")
    add(2)
    println("second value")
}
```

* flow 객체를 실행시키려면? => terminal operator가 필요

1. collect
* collect 함수가 왜 suspend function인가?
  * flow 빌더 함수의 인자인 블록 내부에서 suspend function을 호출 가능하도록 하기 위함
    * 다른 flow 빌더에서 필요 시 발행할 수 있도록 하기 위함이다(03_usecase1_builders 참고)

* collect 함수만 호출하고 내부에 아무런 아이템 수집도 하지 않는다면, 해당 데이터들을 그냥 흘려보내게 된다.
  * 수도꼭지를 틀어놓고 물을 사용하지 않는 것과 같은 효과

```
suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    flow1.collect {  }
}

// 결과 : 2개의 프린트문 호출됨
```

* 실제 발행되는 데이터 확인
```
suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    flow1.collect { receivedValue ->
        println(receivedValue)
        
    }
}
```

---
2. first() / firstOrNull()
* only emits the first value -> then terminates the flow
* 첫 번째 emit()가 끝나면 그 flow는 끝
* 만약 해당 flow가 어느 값도 emit하지 않는다면, first()는 에러 발생시킴
  * 이것을 해결하기 위하여, firstOrNull()을 활용
```
suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val items = flow1.first()
    println(items)
}

// first value
// 1
// 끝... 더 이상 출력되지 않음
```

* first(predicate: suspend(Int) -> Boolean)
  * 조건을 통해 첫 번째로 발행될 아이템의 조건을 설정한다.
```
val item = flow1.first {
    // bigger than one
    it > 1
}
println(item) // 2
```

3. last() / lastOrNull()
* 마지막으로 발행되기로 한 것만 말행함
* first와 비슷한 원리

4. single()
* 1개의 값만이 발행되는 flow에 대해서만 사용 가능하다.
* 하나만 발행된다고 확신할 수 있을 때만 활용
  * 만약 flow가 2개 이상의 값을 발행한다면, IllegalArgumentException을 발생시킨다.

* Wrong UseCase
```
suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2) // IllegalArgument
    }

    val items = flow1.single()
    println(items)

}
```

* Right
```
suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

//        println("second value")
//        emit(2)
    }

    val items = flow1.single()
    println(items) // 1

}
```

5. toSet(), toList()
* 발행될 모든 데이터들을 순서대로 넣은 하나의 집합/리스트로 만들어준다.
* 특징 : flow의 동작이 완료될 때까지 대기 -> 컬렉션 형태로 모든 발행된 아이템들을 한번에 내온다.
  * 모든 것들이 발행될 때까지 대기해야함
```
suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)

        println("third value")
        emit(2)
    }

    val setItems = flow1.toSet()
    println(setItems) // [1, 2]

    val listItems = flow1.toList()
    println(listItems) // [1, 2, 2]

}
```

> first, toList, toSet 등은 테스트할 때 요긴하게 사용될 수 있다.

6. fold()
* 매개변수
  * 초기값 : 첫 발행된 값에 대해 사용할 accumulator 값
  * 작업을 요하는 람다 함수 : 다음 발행에 활용할 accumulator 값을 산출(계산)
    * 마지막 발행 시 계산된 결과가 최종 결과
```
suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val items = flow1.fold(initial = 5) { accumulator, emittedItem ->
        // accumulator
        // reducer와 유사 : 발행된 데이터를 사용할 때마다 accumulator 값을 같이 활용할 수 있다.
        // 중요 : 처음으로 발행된 값의 경우, accumulator의 값은 initial로 설정한 값이다.
        // round1 - accumulator : 5, emittedItem : 1 => 6
        // round2 - accumulator : 6, emittedItem : 2 => 8 => 최종 결과
        accumulator + emittedItem // new accumulator value
        // 굳이 더하기가 아니더라도 다양한 연산이 가능
    }
    println(items) // 8

}
```

7. reduce() : fold와 유사하나, 초기값을 설정할 수 없다는 점에서 차이
* 초기값이 없으므로, 두 번째 발행부터만 연산이 가능하다.(첫 번째로 발행된 값이 accumulator의 초기값이 되고, 이것을 두 번째 발행값에서부터 연산이 가능하다.)
```
suspend fun main() {

    val flow1 = flow {
        delay(100)

        println("first value")
        emit(1)
        delay(100)

        println("second value")
        emit(2)
    }

    val items = flow1.reduce { accumulator, emittedItem ->
        // accumulator
        // 중요 : 처음으로 발행된 값의 경우, 계산이 불가능. 처음에 발행된 값의 경우 두번째 발행에서 accumulator의 값으로 활용됨
        // round1 -> x
        // round2 - accumulator : 1, emittedItem : 2 => 3 => 최종 결과
        accumulator + emittedItem // new accumulator value
    }
    println(items) // 3

}
```