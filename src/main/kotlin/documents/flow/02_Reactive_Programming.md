# 02. Reactive Programming

> Flow는 **반응형 스트림**을 구현한다.

* Flow를 활용함으로써, **Reactive Programming**이라는 프로그래밍 패러다임을 따를 수 있게 된다.

### 반응형 프로그래밍 vs 명령형 프로그래밍
* 반응형 프로그래밍 : Observer 패턴에 기인한다
  * 한 소프트웨어 컴포넌트가 다른 컴포넌트를 관찰 -> 다른 컴포넌트의 상태 변화를 받아들이고 -> 이 변화에 따른 반응을 수행
  * ReactiveX, Flow : 옵저버 패턴을 연장하는 라이브러리들... 데이터 프로세싱을 위한 유용한 연산자들을 추가

### 반응형 프로그래밍 - Flow와 LiveData와의 차이
* LiveData : 다른 컴포넌트에 의하여 관찰될 수 있는 데이터 홀더로서, 상태의 변화가 발생한다면 그것에 반응 가능
  * 한계 : UI 레이어에서만 Reactive Programming을 구현 가능
    * ViewModel에서 구현된 LiveData(UI State)를 Activity에서 관찰 가능
    * 변화가 발생함에 따라, 변화된 UI State를 자동으로 액티비티에 새롭게 렌더링 진행
    * Activity ---(Observe LiveData Property and React to Changes)---> ViewModel

* Flow
  * LiveData와는 달리, 앱의 다른 계층도 반응형으로 만들 수 있음
  * 예시 : Room
    * Room의 Reactive Programming - Room에서는 Flow의 활용을 지원한다. 테이블의 데이터를 flow를 활용해 비동기적 데이터 스트림을 통해 노출시킬 수 있음
      * 장점 : 다른 컴포넌트들이 데이터베이스 테이블의 값들을 구독하여 변화 발생 시 이를 통지받아 이에 대한 처리(UI 반영)를 할 수 있음
    * Room의 Imperative Programming - 단순 suspend function 사용
      * 다른 컴포넌트들이 주기적으로 데이터의 변화를 체크해야 한다
      * Bad Responsiveness
        * 데이터베이스의 데이터 변화 -> UI 반영까지 딜레이 발생

### Reactive Programming으로서 Flow의 단점
* 컨셉과 연산자들이 이해하기 어려움 -> 코드의 복잡성과 가독성 저해