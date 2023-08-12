package part1recap

import scala.concurrent.Future

object ThreadModelLimitations extends App {
  /* Daniel's Rants */

  /**
   * DR #1: OOP encapsulation is only valid in SINGLE THREADED MODEL
   */
  class BankAccount(private var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int): Unit = this.amount -= money
    def deposit(money: Int): Unit = this.amount += money
    def getAmount: Int = amount
  }

  private val bankAccount = new BankAccount(2000)

  for (_ <- 1 to 1000) {
    new Thread(() => bankAccount.withdraw(1)).start()
  }

  for (_ <- 1 to 1000) {
    new Thread(() => bankAccount.deposit(1)).start()
  }
  println(s"Last Amount: " + bankAccount.getAmount) // There is a chance to get another number other that 2000

  // OOP encapsulation is broken in a multithreaded env
  // synchronization! Locks to the rescue
  // deadlocks, live-locks

  /**
   * DR #2: Delegating something to a thread is a PAIN.
   */
  // lets say you have a running thread and you want to pass a runnable to that thread.

  private var task: Option[Runnable] = None

  private val runningThread: Thread = new Thread(() => {
    while (true) {
      while (task.isEmpty) {
        runningThread.synchronized {
          println("[background] Waiting for a task")
          runningThread.wait()
        }
      }

      task.synchronized {
        println("[background] Running the Task")
        task.get.run()
        task = None
      }
    }
  })

  private def delegateBackgroundThread(r: Runnable): Unit = {
    if (task.isEmpty) task = Some(r)

    runningThread.synchronized {
      runningThread.notify()
    }
  }

  runningThread.start()
  Thread.sleep(1000)
  delegateBackgroundThread(() => println("Task 1"))
  Thread.sleep(1000)
  delegateBackgroundThread(() => println("Task 2"))

  /**
   * DR #3: Tracing and dealing with errors in a multithreaded env is a PAIN in the ASS!
   */
  // Lets say we want calculate sum of 1million numbers in 10 threads

  import scala.concurrent.ExecutionContext.Implicits.global

  val futures = (0 to 9)
    .map(i => 100000 * i until 100000 * (i + 1))
    .map(range => Future {
      // Lets say we have an error for one of the threads
      if(range.contains(546735)) throw new RuntimeException("invalid number")
      range.sum
    })

  private val sumFuture = Future.reduceLeft(futures)(_ + _) // Future with the sum of all the numbers
  sumFuture.onComplete(println) //Failure(java.lang.RuntimeException: invalid number) // And we have no idea which one throw that error? 
}
