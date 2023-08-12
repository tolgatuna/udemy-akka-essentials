package part3Testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll {
  private implicit val system: ActorSystem = ActorSystem("SynchronousTestingSpec")
  import SynchronousTestingSpec._

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "A counter" should {
    "synchronously increase it is counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! Inc
      assert(counter.underlyingActor.count == 1)
    }

    "synchronously increase it is counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Inc)
      assert(counter.underlyingActor.count == 1)
    }

    "work on the calling thread dispatcher" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe("testProbe")

      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 0) // Because they are on the same thread and probe has already received the message on that line
    }
  }
}

object SynchronousTestingSpec {
  private case object Inc
  private case object Read

  class Counter extends Actor {
    var count: Int = 0
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}
