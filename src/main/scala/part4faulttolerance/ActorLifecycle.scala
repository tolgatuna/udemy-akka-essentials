package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}

object ActorLifecycle extends App {

  object StartChild
  private class LifecycleActor extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("I am starting")
    override def postStop(): Unit = log.info("I have stopped")

    override def receive: Receive = {
      case StartChild => context.actorOf(Props[LifecycleActor], "child")
    }
  }

  private val system: ActorSystem = ActorSystem("ActorLifecycleTest")
  private val parent: ActorRef = system.actorOf(Props[LifecycleActor])
  parent ! StartChild
  parent ! PoisonPill

  /**
   * Restart
   */
  private object Fail
  private object FailChild
  private object Check
  private object CheckChild

  class Parent extends Actor with ActorLogging {
    private val child = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild =>
        child ! Fail
      case CheckChild =>
        child ! Check
    }
  }

  private class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("child started")
    override def postStop(): Unit = log.info("child stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"supervised actor restarting because of : ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit =
      log.info(s"supervised actor restarted")

    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail now")
        throw new RuntimeException("I failed sorry :/")
      case Check =>
        log.warning("alive and ticking...")
    }
  }

  private val supervisor: ActorRef = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild

  // supervision strategy -> Parent actor 

}
