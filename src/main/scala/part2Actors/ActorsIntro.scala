package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsIntro extends App {
  // step 1 - create an actor system
  private val actorSystem = ActorSystem("firstActorSystem")

  // step 2 - create an actor (ex: Word Count Actor)
  class WordCountActor extends Actor {
    // internal data
    private var totalWords = 0

    override def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[WordCounter] I received message: $message")
        totalWords += message.split("").length
      case msg => println(s"[WordCounter] I cannot understand ${msg.toString}")
    }
  }

  // step 3 - instantiate our actor
  private val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  private val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  // step 4 - communicate!
  wordCounter ! "I am learning Akka and It's pretty damn cool!"
  anotherWordCounter ! "Hi to you"
  // That messages are totally asynchronous
  // ! => means "tell". wordCounter tell message...

  // Actor with constructor parameter example:
  object Person {
    def props(name: String): Props = Props(new Person(name))
  }
  class Person(name: String) extends Actor{
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ => // DO NOTHING
    }
  }

  private val person = actorSystem.actorOf(Person.props("Bob"))
  person ! "hi"



}
