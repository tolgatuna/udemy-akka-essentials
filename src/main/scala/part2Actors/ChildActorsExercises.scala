package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercises extends App{
  // Distributed Word Counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }

  private class WordCounterMaster extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialize(nChildren) =>
        val refs = (0 until  nChildren)
          .map(elem => context.actorOf(Props[WordCounterWorker], s"child-$elem"))
          .toList
        context.become(withChildren(refs, 0, 0, Map()))
    }

    private def withChildren(refs: List[ActorRef], workerNo: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"${self.path} - Text started to process with worker: $workerNo")
        refs(workerNo) ! WordCountTask(currentTaskId, text)
        val updatedRequestMap = requestMap + (currentTaskId -> sender())
        context.become(withChildren(refs, (workerNo + 1) % refs.length, currentTaskId + 1, updatedRequestMap))
      case WordCountReply(taskId, count) =>
        println(s"${self.path} - Word count is: $count for task - $taskId")
        val originalSender = requestMap(taskId)
        originalSender ! count
        context.become(withChildren(refs, workerNo, currentTaskId, requestMap - taskId))
    }

  }

  private class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I got the message")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  /*
  Create WordCounterMaster
  send initialize(10) to wordCounterMaster
  send "Akka is awesome" to wordCounterMaster
    wordCounterMaster will send a WordCountTask("...") to one of its children
      child replies with a WordCountReply(3) in our case to master
    master replies with 3 to the sender

    requester -> wordCountMaster -> wordCounterWorker
    After work done:
    requester <- wordCountMaster <- wordCounterWorker

    Implement Round Robin Logic:
    If you have 5 worker and 7 tasks: Tasks will be shared like that:
      Worker 1, Worker 2, Worker 3, Worker 4, Worker 5, Worker 1, Worker 2
       Task 1 ,  Task 2 ,  Task 3 ,  Task 4 ,  Task 5 ,  Task 6 ,  Task 7
   */
  import WordCounterMaster._
  private val system: ActorSystem = ActorSystem("WordCountActorSystem")
  private val master: ActorRef = system.actorOf(Props[WordCounterMaster], "master")
  master ! Initialize(10)
  master ! "Akka is awesome"
  master ! "I love you, I love you"
  master ! "Super"
}
