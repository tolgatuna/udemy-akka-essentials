package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Stash}

object StashDemo extends App {
  /*
  ResourceActor
    - open => it can receive read/write requests to the resource
    - otherwise it will postpone all read/write requests until the state is open

    Resource actor is close
        - Open => switch to open state
        - Read, write messages are POSTPONED !!

    Resource actor is open
        - Read, Write are handled
        - Close => switch to the closed state

    Ex 1 - [Open, Read, Read, Write]
        - It will switch to open state
        - read the data
        - read the data again
        - write the data
          Everything is fine when it is open

    Ex 2 - [Read , Open, Write]
        - stash Read
          Stash: [Read]
        - Open => switch to the open state
          Mailbox: [Read, Read]
        - Read twice
        - Write the data
   */

  case object Open
  case object Close
  case object Read
  private case class Write(data: String)

  // step 1 - mix-in the Stash Trait
  private class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed
    private def closed: Receive = {
      case Open =>
        log.info("Opening Resource")
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed state")
        // step 2 - stash away what you can't handle
        stash()
    }

    private def open: Receive = {
      case Read =>
        log.info(s"I have read: $innerData")
      case Write(data) =>
        log.info(s"I am writing: $data")
        innerData = data
      case Close =>
        log.warning("Closing Resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the open state")
        stash()
    }
  }

  private val system: ActorSystem = ActorSystem("StashDemo")
  private val resourceActor: ActorRef = system.actorOf(Props[ResourceActor])
  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("I love stash")
  resourceActor ! Close
  resourceActor ! Read

}
