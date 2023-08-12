package part3Testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class InterceptingLogsSpec extends TestKit(ActorSystem("InterceptingLogsSpec", ConfigFactory.load().getConfig("interceptionLogMessages")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import InterceptingLogsSpec._
  private val item = "test item"
  private val validCreditCard = "1234-1234-1234-1234"
  private val invalidCreditCard = "0000-1234-1234-1234"

  "A checkout flow" should {
    "correctly log the dispatch of an oder" in {
      EventFilter.info(pattern = s"Order [0-9]+ for item $item has been dispatched.", occurrences = 1) intercept {
        // our test code
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, validCreditCard)
      }
    }

    "freak out if the payment is denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        // our test code
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, invalidCreditCard)
      }
    }
  }
}

object InterceptingLogsSpec {
  private case class Checkout(item: String, creditCard: String)
  private case class AuthorizedCard(creditCard: String)
  private case object PaymentAccepted
  private case object PaymentDenied
  private case class DispatchedOrder(item: String)
  private case object OrderConfirmed

  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    private def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        paymentManager ! AuthorizedCard(card)
        context.become(pendingPayment(item))
    }

    private def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchedOrder(item)
        context.become(pendingFulfilment(item))
      case PaymentDenied =>
        throw new RuntimeException("I can't handle Payment Denied.")
    }

    private def pendingFulfilment(item: String): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  private class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizedCard(creditCard) =>
        if (creditCard.startsWith("0")) sender() ! PaymentDenied
        else sender() ! PaymentAccepted
    }
  }

  private class FulfillmentManager extends Actor with ActorLogging {
    private val orderId: Int = 0
    override def receive: Receive = behaviour(orderId)

    private def behaviour(orderId: Int): Receive = {
      case DispatchedOrder(item) =>
        context.become(behaviour(orderId + 1))
        log.info(s"Order $orderId for item $item has been dispatched.")
        sender() ! OrderConfirmed
    }
  }
}