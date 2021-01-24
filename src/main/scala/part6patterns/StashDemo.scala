package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

/**
 * Put messages aside for later
 * extends Actor with Stash - mix-in Stash trait
 * stash() - stash the message away
 * unstashAll() - empty the stash
 *
 * Things to be careful:
 * - Potential memory bounds on Stash (~>1M)
 * - Potential mailbox overflow if ~>1M
 * - No not stash the same message twice or it will throw an error
 * - The Stash trait overrides preRestart so must be mixed-in last
 *
 * ResourceActor
 *  - open => it can receive read/write requests to the resource
 *  otherwise it will postpone all read/write requests until the state is open
 *
 * ResourceActor is closed
 *   - Open => switch to the open state
 *   - Read/write messages are postponed
 *
 *  ResourceActor is open
 *  - Read, write are handled
 *  - Close => switch to the closed state
 *
 * [Open, Read, Read, Write]
 * - switch to the open state
 * - read the data
 * - read the data again
 * - write the data
 *
 * [Read, Open, Write]
 * - stash Read
 *   Stash: [Read]
 * - open => switch to the open state
 *   Mailbox: [Read, Write]
 * - read and write are handled
 */
object StashDemo extends App {

  case object Open
  case object Closed
  case object Read
  case class Write(data: String)

  //step 1: extend stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        //step 3: unstash all when you switch the message handler
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed stage")
        //step 2: stash away what you can't handle
        stash()
    }

    def open: Receive = {
      case Read =>
        log.info(s"I have read $innerData")
      case Write(data) =>
        log.info(s"I am writing $data")
        innerData = data
      case Closed =>
        log.info("I am closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed stage")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])
  resourceActor ! Read //stash
  resourceActor ! Open //unstash process read
  resourceActor ! Open //stash
  resourceActor ! Write("I love stash")//process
  resourceActor ! Closed//switch to close; pop the Open that was stashed and switch to open
  resourceActor ! Read//process
}
