package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

/*
Synchronous tests: All messages are handled in the calling thread
Option 1: TestActorRef - Needs an implicit actor system
Option 2: CallingThreadDispatcher - .withDispatcher(CallingThreadDispatcher.Id)
 */
class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll {

  implicit val system = ActorSystem("SynchronousTestingSpec")

  override def afterAll(): Unit = {
    system.terminate()
  }

  import SynchronousTestingSpec._

  "a counter" should {
    "synchronously increase its counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! Inc // counter has already received a message

      assert(counter.underlyingActor.count == 1) // can access the variables inside the actor
    }

    "Synchronously increase its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Inc) // same as sending a message

      assert(counter.underlyingActor.count == 1) // can access the variables inside the actor
    }

    "work on the calling thread dispatcher" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 0) //make sure that the probe has already received the message 0
    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read

  class Counter extends Actor {
    var count = 0

    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}
