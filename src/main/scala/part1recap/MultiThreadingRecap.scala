package part1recap

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object MultiThreadingRecap extends App {
  // creating threads on JVM
  val aThread = new Thread(new Runnable {
    override def run(): Unit = println("I'm running in parallel")
  })

  val aBetterImpThread = new Thread(() => println("I'm running in parallel"))

  aBetterImpThread.start()
  aBetterImpThread.join()

  private val threadHello = new Thread(() => (1 to 1000).foreach(_ => println("Hello")))
  private val threadBye = new Thread(() => (1 to 1000).foreach(_ => println("Bye")))

  // ! Threads are unpredictable and different runs produce different results
  threadHello.start()
  threadBye.start()

  // So if there is no thread safe:
  class BankAccount(private var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int): Unit = this.amount -= money

    def safeWithdraw(money: Int): Unit = this.synchronized {
      this.amount -= money
    }
  }

  /*
    BankAccount(10000)
    Thread1 -> withdraw 1000
    Thread2 -> withdraw 2000

    Thread 1 -> this.amount = thins.amount - ... // Preempted by the operating system
    Thread 2 -> this.amount = thins.amount - 2000 = 8000
    T1 -> 10000 - 1000 = 9000

    this.amount = this.amount - 1000 is NOT ATOMIC
   */

  // Inter-thread communication on the JMW
  // wait - notify mechanism

  // Scala Futures
  private val future = Future {
    // long computation - on a different thread
    42
  }

  // callback
  future.onComplete {
    case Success(42) => println("I found it")
    case Failure(_) => println("Something happened!")
  }

  val aProcessedFuture = future.map(_ + 1) // Future with 43
  val aFlatFuture = future.flatMap { value =>
    Future(value + 2)
  }

  private val filteredFuture = future.filter(_ % 2 == 0)

  // for comprehensions
  val aNonsenseFuture = for {
    meaningOfLife <- future
    filteredMeaning <- filteredFuture
  } yield meaningOfLife + filteredMeaning


}
