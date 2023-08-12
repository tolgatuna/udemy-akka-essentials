package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActors extends App {
  val system = ActorSystem("StoppingActorsDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object StopAll
  }

  private class Parent extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = withChildren(Map())
    private def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"${self.path} - Starting child: $name")
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChildren(children + (name -> childRef)))
      case StopChild(name) =>
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
        log.warning(s"${self.path} - Stopping child: $name")
        context.become(withChildren(children - name))
      case StopAll =>
        log.warning(s"${self.path} - Stopping myself and my children...")
        context.stop(self) // Important!! -> This will also stop children actors by itself! First children will stop and than parent
      // And again is not immediate operation
    }
  }

  private class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"${self.path} - Message: ${message.toString}")
    }
  }

  /**
   * Method #1 - Stopping with 'context.stop'
   */

  import Parent._
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")
  private val child1 = system.actorSelection("/user/parent/child1")
  child1 ! "Hello Child"

  parent ! StopChild("child1") // context.stop is not an immediate action
  //  for(_ <- 1 to 50) {
  //    child1 ! "Are you still there?"
  //  }
  parent ! StartChild("child2")
  private val child2 = system.actorSelection("/user/parent/child2")
  child2 ! "Hi child-2"

  parent ! StopAll
  parent ! "Are you still there!" // will not delivered at some point
  child2 ! "Hi child-2" // will not delivered at some point

  /**
   * Method #2 - Stopping with using special messages
   */
  private val anActor = system.actorOf(Props[Child], "anActor")
  anActor ! "Hello actor!"
  anActor ! PoisonPill // Trigger terminate process
  anActor ! "Hey actor are you there?"

  private val anotherActor = system.actorOf(Props[Child], "anotherActor")
  anotherActor ! "Hello another actor!"
  anotherActor ! Kill // It makes that throw an error akka.actor.ActorKilledException!
  anotherActor ! "Hey actor are you there?"

  /**
   * Death Watch
   */
  private class Watcher extends Actor with ActorLogging {

    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watched $name")
        context.watch(child)
      case Terminated(actorRef) =>
        log.info(s"the reference that I'm watching $actorRef has been stopped")

    }
  }

  private val watcher: ActorRef = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("WatchedChild")
  private val watchedChild: ActorSelection = system.actorSelection("/user/watcher/WatchedChild")
  watchedChild ! "Someone watching mee"
  watchedChild ! PoisonPill
}
