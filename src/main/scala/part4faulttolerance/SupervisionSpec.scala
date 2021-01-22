package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

/*
Parent's Duty:
It is fine if actors crash. Parents must decide upon their children's failure.
When an actor fails, it
- Suspends its children
- Sends a (special) message to its parent

The parent can decide to:
- Resume the actor
- Restart the actor (default)
- Stop the actor
- Escalate and fail itself
 */
class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._

  //Resume directive. Actor state is preserved and problematic message is deleted
  "A supervisor" should {
    "resume its child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "Akka is awesome because I am learning to think in a whole new way"
      child ! Report
      expectMsg(3)// the state of an actor is preserved on a resume
    }

    //Restart directive. Actor loses its internal state
    "restart its child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! ""
      child ! Report
      expectMsg(0) //on a restart it clears the internal state
    }

    //Stop directive. Actor is terminated
    "terminate the child in case of a major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)

      child ! "akka is nice"
      val terminatedMessage = expectMsgType[Terminated] //returns a termination message when the actor is stopped
      assert(terminatedMessage.actor == child)
    }

    //Escalate directive. Stops its children
    "escalate an error when it doesn't know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)

      child ! 1
      val terminatedMessage = expectMsgType[Terminated] //stops all its children and returns a termination message when the actor is escalated
      assert(terminatedMessage.actor == child)
    }
  }

  //Escalate directive with override to not kill the children
  "a kinder supervisor" should {
    "not kill children in case it's restarted or escalates failures" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 45
      child ! Report
      expectMsg(0)//because the preRestart was overridden to not kill the child, the child is still alive
    }
  }

  //restart directive
  "An all for one supervisor" should {
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      supervisor ! Props[FussyWordCounter]
      val secondChild = expectMsgType[ActorRef]

      secondChild ! "Testing supervision"
      secondChild ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        child ! ""
      }

      Thread.sleep(500)//needs time to restart the children

      secondChild ! Report
      expectMsg(0)//restarts all the children
    }
  }

}

object SupervisionSpec {
  object Report

  class Supervisor extends Actor {

    //applies this strategy to the actor that caused the failure
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart //directive
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props:Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      //empty. Won't kill the children
    }
  }

  //Applies this strategy to ALL the actor
  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart //directive
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

  }

  class FussyWordCounter extends Actor with ActorLogging {
    var words = 0

    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("Sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("Sentence is too big")
        else if(!Character.isUpperCase(sentence(0))) {
          throw new IllegalArgumentException("Sentence must start with uppercase")
        } else words += sentence.split(" ").length
      case _ => throw new Exception("Can only receive strings")
    }
  }

}
