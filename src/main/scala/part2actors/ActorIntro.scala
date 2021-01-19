package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorIntro extends App {

  // 1. create an actor system
  //actor system is a heavy weight data structure, that controls a number
  //of threads under the hood, which allocates to running actors
  //only one actor system per application instance
  //must only contain letters, numbers, underscores and dashes
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // 2. create an actor
  //an actor is uniquely identified
  //asynchronous
  //each actor has a unique behaviour
  //cannot invade another actor
  class WordCountActor extends Actor {
    var totalWords = 0

    override def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[word counter] I have received: $message")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] I cannot understand ${msg.toString}")
    }
  }

  // 3. Instantiate the actor
  // we communicate with an actor using it's reference
  // we cannot instantiate actors or invoke it's methods
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  // 4. Communicate
  // asynchronous
  // ! tell - only way to communicate with an actor
  wordCounter ! "I am leaning Akka and it's pretty damn cool"
  anotherWordCounter ! "A different message"

  //pass constructor arguments in an actor
  object Person {
    def props(name: String): Props = Props(new Person(name))
  }

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi my name is $name")
      case _ =>
    }
  }
  val person = actorSystem.actorOf(Person.props("bob"))
  person ! "hi"



}
