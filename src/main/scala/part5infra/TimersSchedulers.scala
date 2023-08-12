package part5infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, PoisonPill, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

object TimersSchedulers extends App {
  private class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  private val system: ActorSystem = ActorSystem("TimersSchedulersDemo")
  private val simpleActor: ActorRef = system.actorOf(Props[SimpleActor])

//  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    // or
  import system.dispatcher
  system.log.info("Scheduling reminder for simpleActor")
  system.scheduler.scheduleOnce(1 second){
    simpleActor ! "Hello There"
  }

  private val routine = system.scheduler.scheduleAtFixedRate(1 second, 2 seconds, simpleActor, "heartbeat")
  system.scheduler.scheduleOnce(5 seconds) {
    routine.cancel()
  }

  /**
   * Exercise - Implement a self closing actor
   *
   * - if the actor receives a message (can be anything), you have one second to send another message
   * - if the time window expires, the actor will stop itself
   * - if you send another message, the time window will be reset
   */

  private class SelfClosingActor extends Actor with ActorLogging {
    override def receive: Receive = withMessage(createTimeoutWindow())

    private def withMessage(timeoutWindow: Cancellable): Receive = {
      case message =>
        log.info(s"Message Received: ${message.toString}, You have one second!")
        timeoutWindow.cancel()
        context.become(withMessage(createTimeoutWindow()))
    }

    private def createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! PoisonPill
      }
    }
  }

  private val selfClosingActor: ActorRef = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
  system.scheduler.scheduleOnce(500 millis) {
    selfClosingActor ! "Hello There"
  }
  system.scheduler.scheduleOnce(700 millis) {
    selfClosingActor ! "Hi again"
  }
  system.scheduler.scheduleOnce(1500 millis) {
    selfClosingActor ! "Me"
  }
  system.scheduler.scheduleOnce(3000 millis) {
    selfClosingActor ! "Died :/"
  }

  /**
   * Timer
   */
  private case object TimerKey
  private case object Start
  private case object HeartBeat
  private case object Stop
  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 1 second)
    override def receive: Receive = {
      case Start =>
        log.info("I have started")
        timers.startTimerAtFixedRate(TimerKey, HeartBeat, 1 second) // If you use same key, no need to worry about cancel and other thing...
      case HeartBeat =>
        log.info("Heartbeat!")
      case Stop =>
        log.warning("Stopping!")
        timers.cancel(TimerKey) // For cancel timer
    }
  }

  private val timerBasedHeartbeatActor: ActorRef = system.actorOf(Props[TimerBasedHeartBeatActor], "timerBasedHeartbeatActor")
  system.scheduler.scheduleOnce(10 seconds) {
    timerBasedHeartbeatActor ! Stop
  }
  system.scheduler.scheduleOnce(15 seconds) {
    timerBasedHeartbeatActor ! Start
  }
  system.scheduler.scheduleOnce(20 seconds) {
    timerBasedHeartbeatActor ! Stop
    timerBasedHeartbeatActor ! PoisonPill
  }
}
