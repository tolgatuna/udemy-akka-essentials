package part5infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random

object Dispatchers extends App {
  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

  // Method #1 - programmatic
  private val system: ActorSystem = ActorSystem("DispatcherDemo")
  private val actors = for (i <- 1 to 10) yield {
    system.actorOf(Props[Counter].withDispatcher("myDispatcher"), s"counter_${i}")
  }

  val r = new Random()
  //  for(i <- 1 to 1000) {
  //    actors(r.nextInt(10)) ! i
  //  }

  // Method #2 - from config
  private val systemConf: ActorSystem = ActorSystem("DispatcherDemoViaConf")
  private val ourActorRockTheJvm: ActorRef = systemConf.actorOf(Props[Counter], "ourActorRockTheJvm")

  /**
   * Dispatchers Implement the ExecutionContext trait
   * So we can run Future inside an Actor
   */
  class DatabaseActor extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContext = context.dispatcher
    // #1 - To non block other actors because of same dispatcher, Solution - 1 seperate dispatcher can be used
//    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("myDispatcher")

    // #2 - Or PinnedDispatcher can be used in conf file!

    override def receive: Receive = {
      case message => Future {
        // wait on resource lets say writing to database and it will take time
        Thread.sleep(5000)
        log.info(s"Success: ${message.toString}")
      }
    }
  }

  private val databaseActor: ActorRef = system.actorOf(Props[DatabaseActor], "DatabaseActor")
//  databaseActor ! "Test Message"

  private val nonBlockingActor: ActorRef = system.actorOf(Props[Counter], "CounterTestActor")
  for(i <- 1 to 1000) {
    val message = s"Important Message $i"
    databaseActor ! message
    nonBlockingActor ! message

  }

}
