package practice.fundamentals

fun main() {
    println("main starts")
    routine(1, 500)
    routine(2, 300)
    println("main ends")
}

// routine 생성 = kotlin에서의 일반적인 함수
fun routine(number: Int, delay: Long) {
    println("Routine $number started to work")
    Thread.sleep(delay)
    println("Routine $number finished")
}