package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
 * Dispatchers control how messages and sent and handled
 *
 * Types of dispatchers:
 * Dispatcher - binds an actor to a thread pool. Most used
 * PinnedDispatcher - binds an actor to a thread pool of exactly one thread, and those threads my single around
 * CallingThreadDispatcher - Ensures all invitations and communications of an actor happen on the calling thread
 */
object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0
    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

  val system = ActorSystem("DispatcherDemo")

  /**
   * Method 1. Programmatically
   */
  val actors = for(i <- 1 to 19) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_${i}")

  val r = new Random()
//  for (i <- 1 to 1000) {
//    actors(r.nextInt(10)) ! i
//  }

  /**
   * Method 2. From config
   */
  val rtjvmActor = system.actorOf(Props[Counter], "rtjvm")

  //Dispatchers implement the ExecutionContext trait
  class DBActor extends Actor with ActorLogging {

    //Allocate a dedicated dispatcher on blocking code, like a db computation
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")

    override def receive: Receive = {
      case message => Future {
        //wait on a resource
        Thread.sleep(5000)
        log.info(s"Success: $message")
      }
    }
  }

  val dbActor = system.actorOf(Props[DBActor])
//  DBActor ! "The meaning of life is 42"

  val nonBlockingActor = system.actorOf(Props[Counter])
  for(i <- 1 to 1000) {
    val message = s"Important message $i"
    dbActor ! message
    nonBlockingActor ! message
  }
}
