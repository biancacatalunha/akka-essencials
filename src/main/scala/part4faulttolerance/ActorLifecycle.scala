package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifecycle extends App {

  object StartChild

  class LifecycleActor extends Actor with ActorLogging {
    override def preStart(): Unit = {
      log.info("I am starting")
    }

    override def postStop(): Unit = {
      log.info("I have stopped")
    }

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }
  }

  val system = ActorSystem("LifecycleDemo")
  val parent = system.actorOf(Props[LifecycleActor], "parent")
//  parent ! StartChild
//  parent ! PoisonPill

  //restart

  object Fail
  object FailChild
  object CheckChild
  object Check

  class Parent extends Actor {
    private val child = context.actorOf(Props[Child], "supervisedChild")
    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }

  class Child extends Actor with ActorLogging {

    override def preStart(): Unit = {
      log.info("Supervised child started")
    }

    override def postStop(): Unit = {
      log.info("Supervised child stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"Supervised actor restarting because of ${reason.getMessage}")
    }

    //this is called by the new actor
    override def postRestart(reason: Throwable): Unit = {
      log.info("Supervised actor restarted")
    }

    override def receive: Receive = {
      case Fail =>
        log.warning("Child will fail now")
        throw new RuntimeException("I failed")
      case Check =>
        log.info("Alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild

  /*
  Supervision strategy
  Default: if an actor threw an exception while processing a message,
  this message that cause the exception to be thrown, is removed from the queue and not put back
  in the mailbox again. The actor is restarted, meaning the mailbox is untouched
   */
}
