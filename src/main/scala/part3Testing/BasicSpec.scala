package part3Testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  // Setup
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An echo actor" should {
    "send back the same message" in {
      import BasicSpec._
      val echoActor = system.actorOf(Props[EchoActor])
      val message = "Hello Test"
      echoActor ! message

      expectMsg(message)
    }
  }

  "A black hole actor" should {
    "send back no message" in {
      import BasicSpec._
      val blackHoleActor = system.actorOf(Props[BlackHole])
      val message = "Hello Test"
      blackHoleActor ! message

      expectNoMessage(1 second)
    }
  }

  "A lab test actor" should {
    import BasicSpec._
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string into uppercase" in {
      labTestActor ! "I Love Akka"
      expectMsg("I LOVE AKKA")
    }

    "turn a string into uppercase with assertions" in {
      labTestActor ! "I love you"

      val reply = expectMsgType[String]
      assert(reply == "I LOVE YOU")
    }

    "reply to greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("Hi!", "Hello!")
    }

    "reply to favoriteTechs" in {
      labTestActor ! "favoriteTechs"
      expectMsgAllOf("Scala", "Java", "Akka")
    }

    "reply with cool techs with different way" in {
      labTestActor ! "favoriteTechs"
      val seq = receiveN(3)
      // free to do more complicated assertions
    }

    "reply cool techs in a fancy way" in {
      labTestActor ! "favoriteTechs"
      expectMsgPF() {
        case "Scala" => //free to do assertions
        case "Java" =>
      }
    }
  }
}

object BasicSpec {
  private class EchoActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  private class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  // message assertions
  private class LabTestActor extends Actor {
    private val random = new Random()

    override def receive: Receive = {
      case "greeting" => sender() ! (if (random.nextBoolean()) "Hi!" else "Hello!")
      case "favoriteTechs" =>
        sender() ! "Scala"
        sender() ! "Akka"
        sender() ! "Java"
      case message: String => sender() ! message.toUpperCase()
    }
  }
}
