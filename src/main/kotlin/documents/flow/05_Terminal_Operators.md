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
