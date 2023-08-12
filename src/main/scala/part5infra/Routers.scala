package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router, Broadcast}
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /**
   * #1 - Manual Router
   */
  class Master extends Actor with ActorLogging {
    // Step 1 - create rooutes (we have created 5 actor routees based off Slave Actor)
    private val slaveRoutees = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave-${i}")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    // Step 2 - define router
    private var router: Router = Router(RoundRobinRoutingLogic(), slaveRoutees)

    override def receive: Receive = {
      // Step 3 - route the message
      case message =>
        router.route(message, sender())

      // Step 4 - handle the termination/lifecycle of routees
      case Terminated(ref) =>
        val removedFromRouter = router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        router = removedFromRouter.addRoutee(ActorRefRoutee(newSlave))
    }
  }

  private class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"${self.path} - Message: $message")
    }
  }

  private val system: ActorSystem = ActorSystem("RoutersDemo")
  val master = system.actorOf(Props[Master])
  //  for(i <- 1 to 10) {
  //    master ! s"[$i] Hello from world"
  //  }

  /**
   * #2 - POOL Router
   * a router actor with its own children.
   *
   * There are two ways to do that:
   */
  // #2.1 - Programmatically (in code)
  private val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  //  for (i <- 1 to 10) {
  //    poolMaster ! s"[$i] Hello from world"
  //  }

  // #2.2 - from configuration (check application.conf)
  private val systemWithConf: ActorSystem = ActorSystem("RoutersDemoWithConf", ConfigFactory.load().getConfig("routersDemo"))
  val poolMaster2 = systemWithConf.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
//  for (i <- 1 to 10) {
//    poolMaster2 ! s"[$i] Hello from world"
//  }

  /**
   * #3 - GROUP Router
   * a router with actors created elsewhere
   *
   * Again there are two ways to do that:
   */
  private val slaveList = (1 to 5).map (i => system.actorOf(Props[Slave], s"slave_${i}")).toList

  // need slaves path
  private val slavePaths: List[String] = slaveList.map(slaveRef => slaveRef.path.toString)
  println(slavePaths)
  // #3.1 - in the code
//  private val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
//  for (i <- 1 to 10) {
//    groupMaster ! s"[$i] Hello"
//  }

  // #3.2 - in the config file
  (1 to 5).map (i => systemWithConf.actorOf(Props[Slave], s"slave__${i}")).toList
  private val groupMaster2 = systemWithConf.actorOf(FromConfig.props(), "groupMaster2")
  for (i <- 1 to 10) {
    groupMaster2 ! s"[$i] Hello"
  }


  /**
   * SPECIAL MESSAGES
   */
  groupMaster2 ! Broadcast("Hello Everyone")

  // PoisonPill and Kill messages are not routed
  // AddRoutee, Remove, Get handled only by the routing actor


}
