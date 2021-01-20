package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}

/*
 Logging is asynchronous
 Akka logging is done with actors
 You can change the logger, e.g SLF4
 */

object ActorLoggingDemo extends App {

  // Method 1: Explicit Logging
  class SimpleActorWithExplicitLogging extends Actor {
    val logger: LoggingAdapter = Logging(context.system, this)
    override def receive: Receive = {
      case message => logger.info(message.toString)
    }
  }

  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogging], "actor")
  actor ! "Logging a simple message"

  // Method 2: ActorLogging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}", a, b) // Interpolator
      case message => log.info(message.toString)
    }
  }

  val simplerActor = system.actorOf(Props[ActorWithLogging], "actorWithLogging")
  simplerActor ! "Logging a simple message by extending a trait"
  simplerActor ! (42, 65)

}
