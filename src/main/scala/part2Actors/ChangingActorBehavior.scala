package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2Actors.ActorsCapabilities.CounterActor

import scala.collection.immutable.Stream.Empty

object ChangingActorBehavior extends App {
  private object FussyKid {
    case object KidAccept

    case object KidReject

    val HAPPY = "Happy"
    val SAD = "Sad"
  }

  private class FussyKid extends Actor {

    import FussyKid._
    import Mom._

    // internal state of the kid
    var state = HAPPY

    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  private class StatelessFussyKid extends Actor {

    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    private def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }

    private def sadReceive: Receive = {
      case Food(VEGETABLE) =>
      case Food(CHOCOLATE) => context.become(happyReceive)
      case Ask(_) => sender() ! KidReject
    }
  }

  private object Mom {
    case class MomStart(kidRef: ActorRef)

    case class Food(food: String)

    case class Ask(question: String)

    val VEGETABLE = "VEGGIES"
    val CHOCOLATE = "CHOCOLATE"
  }

  private class Mom extends Actor {

    import FussyKid._
    import Mom._

    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("Do you want to play?")
      case KidAccept => println("My kid is happy :)")
      case KidReject => println("My kid is sad!!!")
    }
  }

  private val system: ActorSystem = ActorSystem("DemoActorSystem")
  //  private val fussyKid: ActorRef = system.actorOf(Props[FussyKid], "fussyKid")
  private val statelessFussyKid: ActorRef = system.actorOf(Props[StatelessFussyKid], "statelessFussyKid")
  private val mom: ActorRef = system.actorOf(Props[Mom], "mother")

  import Mom._
  //  mom ! MomStart(fussyKid)
  mom ! MomStart(statelessFussyKid)

  /**
   * Exercises
   *
   * 1. Recreate the Counter Actor as stateless
   *
   * 2. Simplified voting system
   */

  // Ex 1. Recreate the Counter Actor as stateless
  object CounterActor {
    // Domain of the CounterActor
    case class IncrementMessage(num: Int)

    case class DecrementMessage(num: Int)

    case class PrintMessage()
  }

  private class CounterActor extends Actor {

    import CounterActor._

    override def receive: Receive = onMessage(0)

    private def onMessage(acc: Int): Receive = {
      case IncrementMessage(num) => context.become(onMessage(acc + num))
      case DecrementMessage(num) => context.become(onMessage(acc - num))
      case PrintMessage() => println(s"[$self] That is the value: $acc")
    }
  }

  // Ex 2. Simplified voting system
  private case class Vote(candidate: String)

  private case object VoteStatusRequest

  private case class VoteStatusReply(candidate: Option[String])

  private class Citizen extends Actor {
    override def receive: Receive = handleVote(None)

    private def handleVote(votedCandidate: Option[String]): Receive = {
      case Vote(candidate) => if (votedCandidate.isEmpty) context.become(handleVote(Some(candidate)))
      case VoteStatusRequest => sender() ! VoteStatusReply(votedCandidate)
    }
  }

  private case class AggregateVotes(citizens: Set[ActorRef])

  private class VoteAggregator extends Actor {
    override def receive: Receive = handleVoteAggregation(0, Map())

    private def handleVoteAggregation(totalCitizen: Int, candidates: Map[String, Int]): Receive = {
      case AggregateVotes(refs) =>
        context.become(handleVoteAggregation(refs.size, Map()))
        refs.foreach(_ ! VoteStatusRequest)
      case VoteStatusReply(None) =>
        println("Waiting for some candidates to complete voting")
      case VoteStatusReply(Some(votedCandidate)) =>
        val left = totalCitizen - 1
        val count = candidates.getOrElse(votedCandidate, 0)
        val newResult = candidates + (votedCandidate -> (count + 1))
        context.become(handleVoteAggregation(left, newResult))
        if (left == 0) {
          println(s"Votes: $newResult")
        }

    }
  }

  private val alice: ActorRef = system.actorOf(Props[Citizen])
  private val bob: ActorRef = system.actorOf(Props[Citizen])
  private val charlie: ActorRef = system.actorOf(Props[Citizen])
  private val daniel: ActorRef = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Bob")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  private val voteAggregator: ActorRef = system.actorOf(Props[VoteAggregator])

  /*
    Should print:
    Martin -> 1
    Jonas -> 1
    Roland -> 2
   */
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))


}
