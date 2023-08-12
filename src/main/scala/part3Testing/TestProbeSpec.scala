package part3Testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._

  "A master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAct)
    }

    "send the work to the slave actor and get response correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAct)

      val workLoadString = "I love Akka"
      master ! Work(workLoadString)

      slave.expectMsg(SlaveWork(workLoadString, testActor))
      slave.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAct)

      val workLoadString = "I love Akka" // Twice send
      master ! Work(workLoadString)
      master ! Work(workLoadString)

      // Mocking slave
      slave.receiveWhile() {
        case SlaveWork(`workLoadString`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
      }

      // There should be two reports
      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }
}

object TestProbeSpec {
  // scenario
  /*
    Word counting actor hierarchy master-slave

    send some work to the master
      - master sends the slave the piece of work
      - slave process the work and replies to master
      - master aggregates the result
    master sends the total count to the original requester
   */
  private case class Register(slaveRef: ActorRef)
  private case class Work(text: String)
  private case class SlaveWork(text: String, originalRequester: ActorRef)
  private case class WorkCompleted(count: Int, originalRequester: ActorRef)
  private case object RegistrationAct
  private case class Report(totalCount: Int)

  private class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender() ! RegistrationAct
        context.become(online(slaveRef, 0))
      case _ => // ignore
    }

    private def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(slaveRef, newTotalWordCount))
    }
  }

  // private class Slave extends Actor .... (We don't care slave for now cause we will mock it with TestProbe)
}
