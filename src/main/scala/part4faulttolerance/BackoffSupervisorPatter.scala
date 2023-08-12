package part4faulttolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.BackoffSupervisor
import akka.pattern.BackoffOpts

import scala.concurrent.duration._
import java.io.File
import scala.io.Source
import scala.language.postfixOps

object BackoffSupervisorPatter extends App {
  case object ReadFile

  class FileBasedPersistenceActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit =
      log.info("Persistence Actor starting")

    override def postStop(): Unit =
      log.warning("Persistence Actor has stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.warning("Persistence Actor restarting")

    override def receive: Receive = {
      //      case ReadFile => if(dataSource == null) dataSource = Source.fromFile(new File("src/main/resources/testfiles/important.txt"))
      case ReadFile => if (dataSource == null) dataSource = Source.fromFile(new File("src/main/resources/testfiles/wrong_important.txt"))
        log.info(s"I've just read some Important data: ${dataSource.getLines().toList}")
    }
  }

  private val system: ActorSystem = ActorSystem("BackOffSupervisorDemo")
  //  private val actorRef: ActorRef = system.actorOf(Props[FileBasedPersistenceActor])
  //  actorRef ! ReadFile

  private val simpleSupervisorProps: Props = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      Props[FileBasedPersistenceActor],
      "fileActor",
      3 seconds, // 3 seconds, 6 seconds, 12 seconds, 24 seconds until to be 30 seconds (parameter down below)
      30 seconds,
      0.2
    )
  )

  val simpleBackOffSupervisor = system.actorOf(simpleSupervisorProps, "simpleBackOffSupervisor")
  /*
    we created a simpleBackOffSupervisor
      - which will create a child actor for us with a name fileActor (child type is Props[FileBasedPersistenceActor]),
      - it will redirect our messages to child actor directly
      - supervision strategy is default one (restart on everything), but we have added some extra timeouts:
        - will try to restart child after 3 seconds (with random 0.2 extra timeout, it can be 3.06 or 3.11 or 3.2 or bla bla... Totally random to max 0.2)
        - next attempt will be x2 of first one
   */

  simpleBackOffSupervisor ! ReadFile


  // If we want to control stop period of our actor if we get any error:
  private val stopSupervisorProps: Props = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[FileBasedPersistenceActor],
      "fileActor",
      3 seconds, // 3 seconds, 6 seconds, 12 seconds, 24 seconds until to be 30 seconds (parameter down below)
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
  /*
      we created a stopSupervisorProps
        - which will create a child actor for us with a name fileActor (child type is Props[FileBasedPersistenceActor]),
        - it will redirect our messages to child actor directly
        - supervision strategy is default stop, but we have added some extra timeouts:
          - will try to run child after 3 seconds (with random 0.2 extra timeout, it can be 3.06 or 3.11 or 3.2 or bla bla... Totally random to max 0.2)
          - next attempt will be x2 of first one
     */

  stopSupervisor ! ReadFile

  // Eager File Based Persistence Actor which tries to read file while initializing
  class EagerFBPActor extends FileBasedPersistenceActor {
    override def preStart(): Unit = {
      log.info("Eager Persistence Actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/wrong_important_file.txt"))
    }
  }

//  private val eagerActor: ActorRef = system.actorOf(Props[EagerFBPActor])
  // Will throw ActorInitializationException => STOP

  private val repeatSupervisionProps: Props = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[EagerFBPActor],
      "eagerFBPActor",
      1 second,
      30 seconds,
      0.1
    )
  )

  system.actorOf(repeatSupervisionProps, "repeatSupervision")
  /*
    repeatSupervision
      - child eagerFBPActor
        - will die on start with exception ActorInitializationException
        - trigger the supervision strategy in eagerSupervisor => STOP eagerActor
     - backoff will kick in after 1 sec, 2, 4, 8, 16 and that will be it!

   */
}
