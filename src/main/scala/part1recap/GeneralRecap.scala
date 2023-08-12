package part1recap

import scala.annotation.tailrec
import scala.util.Try

object GeneralRecap extends App {
  private val aCondition: Boolean = false

  private var aVariable = 42
  aVariable += 1 // aVariable = 43

  // expression
  val aConditionVal = if (aCondition) 42 else 65

  // Code block
  val aCodeBlock = {
    if (aCondition) 64
    56
  }

  // types
  // Unit (Side Effect)
  private val theUnit = println("Hello Scala")

  // functions
  def aFunction(x: Int): Int = x + 1

  // recursion & TAIL recursion
  @tailrec
  private def factorial(n: Int, acc: Int): Int =
    if (n <= 0) acc
    else factorial(n - 1, acc * n)

  // OOP
  class Animal

  private class Dog extends Animal

  val aDog: Animal = new Dog

  trait Carnivore {
    def eat(a: Animal): Unit
  }

  class Crocodile extends Animal with Carnivore {
    override def eat(a: Animal): Unit = println("Crunch")
  }

  // method notations
  private val aCrocodile = new Crocodile
  aCrocodile.eat(aDog)
  aCrocodile eat aDog // Identical

  // Anonymous Class
  private val aCarnivore = new Carnivore {
    override def eat(a: Animal): Unit = println("Roar")
  }

  aCarnivore eat aDog

  // generics
  abstract class MyList[+A]

  // companion objects
  object MyList

  // case classes
  case class Person(name: String, age: Int)

  // Exceptions
  val aPotentialFailure = try {
    throw new RuntimeException("I am Innocent") // Nothing
  } catch {
    case e: Exception => "I caught an exception"
  } finally {
    // side effect
    println("some logs")
  }
  println(aPotentialFailure)

  // Functional Programming
  private val incrementer = new Function1[Int, Int] {
    override def apply(v1: Int): Int = v1 + 1
  }
  private val incremented: Int = incrementer(42) // == incrementer.apply(42)

  val anonymousIncrementer: (Int => Int) = (x: Int) => x + 1
  // Functional Programming is all about working with functions as first-class

  private val ints: List[Int] = List(1, 2, 3).map(anonymousIncrementer)

  // for comprehensions
  val pairs = for {
    num <- List(1, 2, 3, 4)
    char <- List('A', 'B', 'C', 'D')
  } yield num + " - " + char
  // == List(1, 2, 3, 4).flatMap(num => List('A', 'B', 'C', 'D').map(char => num + " - " + char))
  println(pairs)

  // Options and Try
  val anOption = Some(2)
  val aTry = Try {
    throw new RuntimeException("Exception")
  }

  // pattern matching
  private val unknown = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "two"
    case _ => "Other"
  }

  private val bob = Person("Bob", 22)
  private val greeting = bob match {
    case Person(n, _) => s"Hi my name is $n"
    case _ => "Sorry not want to meet with you"
  }
  println(greeting)

}
