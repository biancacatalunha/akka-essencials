package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

/*
Method 1: context.stop(self)
Asynchronous so it doesn't stop immediately
An actor can stop itself or its children.
If it stops itself and it has children, they will be stopped first and then the parent.

Method 2:
PoisonPill - stops the actor straight away
Kill - stops immediately and throws an ActorKilledException

Watcher:
A parent watching a child by
context.watch(actorRef)
When the child is stopped, the watcher actor(parent) receives a Terminated(actorRef) message
 */
object StartingStoppingActors extends App {
  val system = ActorSystem("StoppingActorsDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }
  class Parent extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"Stopping child with name $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
      case Stop =>
        log.info("Stopping myself")
        context.stop(self) // that's how an actor stops itself and its child actors. The child stops before the parent
      case message => log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  import Parent._

  //Method 1
//  val parent = system.actorOf(Props[Parent], "parent")
//  parent ! StartChild("child1")
//  val child = system.actorSelection("/user/parent/child1")
//  child ! "Hi kid"
//  parent ! StopChild("child1") // the child is not stopped immediately
//  //for (_ <- 1 to 50) child ! "Are you there?"
//
//  parent ! StartChild("child2")
//  val child2 = system.actorSelection("/user/parent/child2")
//  child2 ! "Hi second child"
//  parent ! Stop
//  for (i <- 1 to 10) parent ! s"$i parent, are you there?" // should not be received
//  for (i <- 1 to 100) child2 ! s"$i second kid, are you there?"

  // Method 2
//  val looseActor = system.actorOf(Props[Child])
//  looseActor ! "hello loose actor"
//  looseActor ! PoisonPill // invokes the stopping procedure and stops straight away
//  looseActor ! "are you still there?"
//
//  val abruptlyTerminatedActor = system.actorOf(Props[Child])
//  abruptlyTerminatedActor ! "You are about to be terminated"
//  abruptlyTerminatedActor ! Kill // stops immediately and throws an error
//  abruptlyTerminatedActor ! "You have been terminated"

  // Watcher
  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child) // starts watching a child
      case Terminated(ref) =>
        log.info(s"The reference that I'm watching $ref has been stopped")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)
  watchedChild ! PoisonPill
}
