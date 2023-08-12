package part1recap

import scala.concurrent.Future
import scala.language.implicitConversions

object AdvancedRecap extends App {
  // partial functions
  private val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  partialFunction(1)
  // partialFunction(11) // => will throw error -> scala.MatchError

  // Same with partialFunction
  val pf = (x: Int) => x match {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val function: (Int => Int) = partialFunction

  val modifiedList = List(1, 2, 3).map {
    case 1 => 42
    case _ => 0
  }

  // lifting
  val lifted = partialFunction.lift
  lifted(2) // will return Some(65)
  lifted(99) // will return None (other that throwing exception)

  // orElse
  val pfChain = partialFunction.orElse[Int, Int] {
    case 60 => 9000
  }
  pfChain(5) // 999
  pfChain(60) // 9000
  //  pfChain(475) // throw a Match Error

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]

  def receive: ReceiveFunction = {
    case 1 => println("Hello")
    case _ => println("confused...")
  }

  // implicits
  implicit val timeout: Int = 3000

  private def setTimeout(f: () => Unit)(implicit timeout: Int): Unit = f()

  setTimeout(() => println("timeout ended")) // extra parameter list omitted with implicit

  // implicit conversions
  // 1) implicit defs
  case class Person(name: String) {
    def greet = s"Hi my name is $name"
  }

  implicit def fromStringToPerson(string: String): Person = Person(string)

  "Peter".greet

  // 2) implicit classes
  implicit class Dog(name: String) {
    def bark(): Unit = println("bark")
  }

  "Lassie".bark()

  // organize
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1, 2, 5, 4, 3).sorted // List (5, 4, 3, 2, 1)

  // imported scope

  import scala.concurrent.ExecutionContext.Implicits.global

  val future = Future {
    println("hello, future")
  }

  // companion objects of the types included in the call
  object Person {
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }

  private val people: List[Person] = List(Person("Bob"), Person("William"), Person("Alice")).sorted
  println(people) // List(Person(Alice), Person(Bob), Person(William))

}
