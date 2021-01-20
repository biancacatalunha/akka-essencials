package part2actors.exercises.childActors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable


object MyFinalImplementation extends App {

  object WordCounterMaster {
    case class Initialize(nChildren: Int) //creates n children
    case class WordCountTask(id: Int, text: String) //receive a text, send to it's children
    case class WordCountReply(id: Int, count: Int) // reply
    case object AggregateChildren
  }

  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) =>
        println(s"${self.path} Initializing $nChildren children")
        val children: mutable.Queue[ActorRef] = mutable.Queue()

        (1 to nChildren).foreach(n => {
          val childRef = context.actorOf(Props[WordCounterWorker], s"child$n")
          println(s"${self.path} $childRef created")

          children.enqueue(childRef)
        })
        context.become(withChildren(children))
    }

    def withChildren(children: mutable.Queue[ActorRef]): Receive = {
      case WordCountTask(id, text) =>
        val worker: ActorRef = children.dequeue
        children.enqueue(worker)
        worker ! WordCountTask(id, text)
        println(s"${self.path} Sending message to worker $worker")

      case WordCountReply(id, count) =>
        println(s"${self.path} Task $id returned $count")
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I received a task")
        val count = text.split(" ").length
        sender() ! WordCountReply(id, count)
    }
  }

  import WordCounterMaster._

  val system = ActorSystem("system")
  val master = system.actorOf(Props[WordCounterMaster], "master")
  master ! Initialize(3)
  master ! WordCountTask(1, "Akka")
  master ! WordCountTask(2, "Akka is")
  master ! WordCountTask(3, "Akka is awesome")
  master ! WordCountTask(4, "Akka is awesome !!")
}
