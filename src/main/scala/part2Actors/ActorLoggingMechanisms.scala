package part2Actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingMechanisms extends App {

  // #1 - Explicit Logging
  private class SimpleActorWithExplicitLogger extends Actor {
    /*
      Logging levels:
      1. DEBUG
      2. INFO
      3. WARNING/WARN
      4. ERROR
     */
    val logger = Logging(context.system, this)

    override def receive: Receive = {
      case message: String => logger.info(s"Text received: $message")
    }
  }

  private val system: ActorSystem = ActorSystem("TestLogging")
  private val simpleActorWithExplicitLogger: ActorRef = system.actorOf(Props[SimpleActorWithExplicitLogger], "simpleActorWithExplicitLogger")
  simpleActorWithExplicitLogger ! "Test Logging"

  // #2 - Actor Logging (MOST COMMON WAY)
  private class SimpleActorWithActorLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String => log.info(s"Text received: $message")
      case (a, b) => log.info(s"Two things {} - {}", a.toString, b.toString)
    }
  }

  private val simpleActorWithActorLogging: ActorRef = system.actorOf(Props[SimpleActorWithActorLogging], "SimpleActorWithActorLogging")
  simpleActorWithActorLogging ! "Test Logging"
  simpleActorWithActorLogging ! (3, 42)
}
