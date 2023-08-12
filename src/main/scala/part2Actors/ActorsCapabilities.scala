package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsCapabilities extends App {
  private class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => context.sender() ! "Hello my friend!" // replying to a message
      case message: String => println(s"[${self}] Message Received: $message") // context.self == self
      case number: Number => println(s"[$self] Number Received: $number")
      case SpecialMessage(content) => println(s"[$self] Special Message Received: $content")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi!"
      case ForwardMessage(content, secondPerson, lastPerson) => secondPerson ! TellsYouMessage(content, lastPerson)
      case TellsYouMessage(content, lastPerson) => lastPerson forward ResultMessage(content)
      case ResultMessage(content) => println(s"[$self] I GOT FORWARDED MESSAGE from ${sender()} and it tells to me '$content'")
    }
  }

  private val system: ActorSystem = ActorSystem("DemoActorSystemCapabilities")
  private val simpleActor: ActorRef = system.actorOf(Props[SimpleActor], "simpleActor")

  private case class SpecialMessage(content: String)

  /**
   * 1. Messages can be any type with these two conditions
   *      - Messages must be IMMUTABLE
   *      - Messages must be SERIALIZABLE (in practice use case classes and case objects)
   */
  simpleActor ! "Hello There"
  simpleActor ! 42
  simpleActor ! SpecialMessage("Super Special Message")

  /**
   * 2. Actors have information about their context and about themselves
   *      - context.self == self ===> `this` in OOP
   */
  private case class SendMessageToYourself(content: String)

  simpleActor ! SendMessageToYourself("I am sending a message and please send it to yourself")

  /**
   * 3. Actors can REPLY to messages
   *      - with help of "context.sender()"
   */
  private case class SayHiTo(ref: ActorRef)

  private val bob = system.actorOf(Props[SimpleActor], "bob")
  private val alice = system.actorOf(Props[SimpleActor], "alice")
  alice ! SayHiTo(bob)

  /**
   * 4. Dead Letters
   *      - What if happens if we try to get response from main?
   *        + Akka System is creating a akkaDeadLetter Actor. It is the garbage pool of messages.
   */
  alice ! "Hi!" // reply to "main" ?? -> [akkaDeadLetter][08/02/2023 14:26:52.935] [DemoActorSystemCapabilities-akka.actor.default-dispatcher-8] [akka://DemoActorSystemCapabilities/deadLetters] Message [java.lang.String] from Actor[akka://DemoActorSystemCapabilities/user/alice#1064420908] to Actor[akka://DemoActorSystemCapabilities/deadLetters] was not delivered. [1] dead letters encountered. If this is not an expected behavior then Actor[akka://DemoActorSystemCapabilities/deadLetters] may have terminated unexpectedly. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.

  /**
   * 5. Forwarding messages
   *      - sending a message with the original sender
   *      - Ex: Daniel -> Alice -> Bob
   *        Daniel tell a message to Alice to tell that message to Bob
   *        So Alice will tell that message like that. Bob, Daniel tell you a message
   */
  private case class ForwardMessage(content: String, secondPerson: ActorRef, lastPerson: ActorRef)

  private case class TellsYouMessage(content: String, lastPerson: ActorRef)

  private case class ResultMessage(content: String)

  private val daniel = system.actorOf(Props[SimpleActor], "daniel")
  alice ! ForwardMessage("Important Message", bob, daniel)

  /**
   * Exercises
   *
   * 1. a Counter actor
   *  - Increment
   *  - Decrement
   *  - Print
   *
   * 2. a Bank account as an actor
   *    - receives
   *      + Deposit an amount
   *      + Withdraw an amount
   *      + Statement
   *    - replies with
   *      + Success
   *      + Failure
   *    - Use an ATM actor to interact with the bank account actor.
   */

  // Ex. 1 -  Counter Actor
  object CounterActor {
    // Domain of the CounterActor
    case class IncrementMessage(num: Int)

    case class DecrementMessage(num: Int)

    case class PrintMessage()
  }

  private class CounterActor extends Actor {

    import CounterActor._

    private val counter: Int = 0

    override def receive: Receive = onMessage(counter)

    private def onMessage(counter: Int): Receive = {
      case IncrementMessage(num) => context.become(onMessage(counter + num))
      case DecrementMessage(num) => context.become(onMessage(counter - num))
      case PrintMessage() => println(s"[$self] That is the value: $counter")
    }
  }

  import CounterActor._

  private val counter: ActorRef = system.actorOf(Props[CounterActor], "counter")
  counter ! IncrementMessage(10)
  counter ! IncrementMessage(5)
  counter ! DecrementMessage(1)
  counter ! PrintMessage()

  // 2.  Bank account as an actor
  object BankAccount {
    def props(balance: Int): Props = Props(new BankAccount(balance))
  }

  private class BankAccount(private var balance: Int) extends Actor {
    override def receive: Receive = {
      case DepositRequest(amount) =>
        if (amount < 0) {
          Failure("Deposit Failed - Amount is not correct")
        } else {
          balance += amount
          sender() ! Success("Deposit Success")
        }
      case WithdrawRequest(amount) =>
        if (amount <= balance) {
          balance -= amount
          sender() ! Success("Withdraw Success")
        } else {
          sender() ! Failure("Withdraw Failed - Not enough balance")
        }
      case StatementRequest() =>
        sender() ! Statement(balance)
    }
  }

  private class ATM extends Actor {
    override def receive: Receive = {
      case DepositTo(amount, ref) => ref ! DepositRequest(amount)
      case WithdrawFrom(amount, ref) => ref ! WithdrawRequest(amount)
      case StatementOf(ref) => ref ! StatementRequest()
      case Success(msg) => println(s"[ATM] OPERATION SUCCESS - $msg")
      case Failure(msg) => println(s"[ATM] OPERATION FAILED - $msg")
      case Statement(totalAmount) => println(s"[ATM] STATEMENT - $totalAmount")
    }
  }

  private case class DepositRequest(amount: Int)
  private case class DepositTo(amount: Int, bankAccountRef: ActorRef)

  private case class WithdrawRequest(amount: Int)
  private case class WithdrawFrom(amount: Int, bankAccountRef: ActorRef)

  private case class StatementRequest()
  private case class StatementOf(bankAccountRef: ActorRef)

  private case class Success(msg: String)
  private case class Failure(msg: String)
  private case class Statement(totalAmount: Int)

  private val myAccount: ActorRef = system.actorOf(BankAccount.props(1000))
  private val atm: ActorRef = system.actorOf(Props[ATM], "atm")

  atm ! DepositTo(1000, myAccount)
  atm ! WithdrawFrom(100, myAccount)
  atm ! WithdrawFrom(10000, myAccount)
  atm ! StatementOf(myAccount)

}
