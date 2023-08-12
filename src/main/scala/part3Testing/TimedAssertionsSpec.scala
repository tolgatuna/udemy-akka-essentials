package part3Testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3Testing.TimedAssertionsSpec.{WorkResult, WorkerActor}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class TimedAssertionsSpec extends TestKit(ActorSystem("TimedAssertionsSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])

    "reply to sender within 500 millisecond - 1 second" in {
      within(500 millis, 1 second) {
        workerActor ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work at a reasonable cadence" in {
      within(1 second) {
        workerActor ! "workSequence"
        val results: Seq[Int] = receiveWhile[Int](2 second, 500 millis, messages = 10) {
          case WorkResult(result) => result
        }
        assert(results.sum == 10)
      }
    }

    "reply to a test probe in a timely manner" in { // check the application.conf
      val probe = TestProbe("testProbe")
      probe.send(workerActor, "work")
      probe.expectMsg(WorkResult(42))
    }
  }

}

object TimedAssertionsSpec {
  private case class WorkResult(result: Int)

  private class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSequence" =>
        val r = new Random()
        for(_ <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }
  }
}
