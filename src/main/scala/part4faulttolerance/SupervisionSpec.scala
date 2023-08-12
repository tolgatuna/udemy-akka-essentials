package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._

  "A supervisor" should {
    "create a child successfully" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)
    }

    "resume its child in case of a minor fault (RuntimeException)" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "Akka is awesome because I'm learning to think in a different way" // It will throw runtime exception and we will just resume the child
      child ! Report
      expectMsg(3) // because it is resumed, state should be same
    }

    "restart its child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "" // It will null pointer exception and we will restart the child
      child ! Report
      expectMsg(0) // because it is restarted, state should start from beginning
    }

    "terminate its child in case of major error (IllegalArgumentException)" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! "an invalid message" // It will stop the child
      child ! Report
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }

    "escalate itself with an error when it doesn't know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      watch(supervisor)
      child ! 43 // It will escalate everything
      child ! Report
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "A kinder supervisor" should {
    "not kill children in case it's restarted or escalates failure" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! 42
      child ! Report
      expectMsg(0) //
    }
  }

  "An allForOneSupervisor" should {
    "apply the all-for-one-strategy" in {
      val allForOneSupervisor = system.actorOf(Props[AllForOneSupervisor])
      allForOneSupervisor ! Props[FussyWordCounter]
      val child1 = expectMsgType[ActorRef]

      allForOneSupervisor ! Props[FussyWordCounter]
      val child2 = expectMsgType[ActorRef]

      child2 ! "Second Child"
      child2 ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        child1 ! ""
      }

      Thread.sleep(500)
      child2 ! Report
      expectMsg(0)
    }
  }
}

object SupervisionSpec {
  class Supervisor extends Actor {
    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() { // OneForOneStrategy: just do operation for error thrower
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate // Kill let the upper child know error as well
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  private class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  private class AllForOneSupervisor extends Supervisor {
    override def supervisorStrategy = AllForOneStrategy() { // AllForOneSupervisor: do operation for all children if one fail!
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate // Kill let the upper child know error as well
    }
  }

  case object Report
  class FussyWordCounter extends Actor {
    var words = 0
    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if(sentence.length > 20) throw new RuntimeException("sentence is too big")
        else if(!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else words += sentence.split(" ").length
      case _ => throw new Exception("can only receive string")
    }
  }
}