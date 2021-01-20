package part2actors.exercises.childActors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable

object MyFirstImplementation extends App {

  /*
    Distributed word counting

    create WordCounterMaster
    send Initialize(10) to wordCounterMaster
    send "Akka is awesome" to wordCounterMaster
      wcm will send WordCountTask(..) to one of its children
        child replies with WordCountReply(3) to the master
      master replies with 3 to the sender

    requester -> wcm (sends a task)-> wcw
    requester <- wcm <-

    round robin logic
   */

  object WordCounterMaster {

    case class Initialize(nChildren: Int) //creates n children
    case class WordCountTask(text: String) //receive a text, send to it's children
    case class WordCountReply(count: Int, worker: ActorRef) // reply
    case object AggregateChildren

  }

  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    var children: mutable.Queue[ActorRef] = mutable.Queue()

    override def receive: Receive = {
      case Initialize(nChildren) =>
        println(s"${self.path} Initializing $nChildren children")
        (1 to nChildren).foreach(n => {
          val childRef = context.actorOf(Props[WordCounterWorker], s"child$n")
          println(s"${self.path} $childRef created")

          children.enqueue(childRef)
        })

      case WordCountTask(text) =>
        val worker = children.dequeue()
        children.enqueue(worker)
        worker ! WordCountTask(text)
        println(s"${self.path} Sending message to worker ${worker}")

      case WordCountReply(count, childRef) =>
        println(s"${self.path} Word count is $count worker was $childRef")
    }

  }


  class WordCounterWorker extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(text) =>
        println(s"${self.path} I received a task")
        val count = text.split(" ").length
        sender() ! WordCountReply(count, context.self)
    }
  }

  import WordCounterMaster._

  val system = ActorSystem("system")
  val master = system.actorOf(Props[WordCounterMaster], "master")
  master ! Initialize(3)
  master ! WordCountTask("Akka")
  master ! WordCountTask("Akka is")
  master ! WordCountTask("Akka is awesome")
  master ! WordCountTask("Akka is awesome !!")

}
