package part2Actors

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}

object ChildActors extends App {
  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  import Parent._
  private class Parent extends Actor {
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} Creating a child with $name")
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    private def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => if(childRef != null) childRef forward message
    }
  }

  private class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} - I got the message: $message")
    }
  }

  private val system: ActorSystem = ActorSystem("ActorSystemTest")
  private val parent: ActorRef = system.actorOf(Props[Parent], "parent")

  parent ! CreateChild("child")
  parent ! TellChild("Hello kid")

  // actor hierarchies
  // Parent can have many children Actor or child can have also some children Actors...
  // Parent -> child -> grandChild....
  //        -> anotherChild

  /**
   * Guardian Actors (top-level actors)
   *    - /system => system guardian
   *    - /user   => user-level guardian
   *    - /       => the root guardian
   */

  /**
   * Actor Selection
   */
  private val potentialSelection: ActorSelection = system.actorSelection("/user/parent/child")
  potentialSelection ! "I found you the child"

  private val potentialSelectionInvalid: ActorSelection = system.actorSelection("/user/asd")
  potentialSelectionInvalid ! "I am trying to find asd"

  /**
   * DANGER!!
   *
   * NEVER PASS MUTABLE ACTOR STATE, OR THE ‘THIS‘ REFERENCE, TO CHILD ACTORS.
   */
  private object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeBankAccount
  }
  private class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._

    var amount = 0

    override def receive: Receive = {
      case InitializeBankAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "creditCard")
        creditCardRef ! AttachToAccount(this) // !!
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)
    }

    def deposit(funds: Int): Unit = {
      println(s"${self.path} Depositing $funds on top of $amount")
      amount += funds
    }
    def withdraw(funds: Int): Unit = {
      println(s"${self.path} Withdrawing $funds from $amount")
      amount -= funds
    }
  }

  private object CreditCard {
    case class AttachToAccount(bankAccount: NaiveBankAccount) // !! -> Parent in child!
    case object CheckStatus
  }


  private class CreditCard extends Actor {
    import CreditCard._
    override def receive: Receive = {
      case AttachToAccount(bankAccount) => context.become(attachedToAccount(bankAccount))
    }

    private def attachedToAccount(bankAccount: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} Your message has been processed")
        bankAccount.withdraw(1) //!! -> because I can!
    }
  }

  import NaiveBankAccount._
  import CreditCard._

  private val bankAccount: ActorRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccount ! InitializeBankAccount
  bankAccount ! Deposit(1000)

  Thread.sleep(500)
  private val creditCardSelection = system.actorSelection("/user/account/creditCard")
  creditCardSelection ! CheckStatus

}
