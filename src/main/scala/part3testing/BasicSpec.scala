package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.BasicSpec.{BlackHole, LabTestActor, SimpleActor}

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.Random

/*
  TestKit(ActorSystem("BasicSpec")) - It instantiate an actor system
  ImplicitSender - Send reply scenarios in actors, passes the testActor as the sender for every message
  WordSpecLike - Allows the description of tests in a natural language style
  BeforeAndAfterAll - Hocks

  system and testActor are members of the testKit

  Recommended to use a companion object
 */
class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  // destroys
  override def afterAll(): Unit = {
    //system is a member of testkit
    TestKit.shutdownActorSystem(system)
  }

  //test suite
  "A simple actor" should {
    //test
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "Hello test"
      echoActor ! message

      expectMsg(message) // akka.test.single-expect-default set to change the timeout
    }
  }

  "a blackHole actor" should {
    "send back some message" in {
      val blackHole = system.actorOf(Props[BlackHole])
      val message = "Hello test"
      blackHole ! message

      expectNoMessage(1 second)
    }
  }

  "a lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string to upper case" in {
      labTestActor ! "I love Akka"
      val reply = expectMsgType[String]

      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("Hi", "Hello")
    }

    "reply with favourite tech" in {
      labTestActor ! "favouriteTech"
      expectMsgAllOf("Scala", "Akka")
    }

    "reply with cool tech in a different way" in {
      labTestActor ! "favouriteTech"
      val messages: Seq[AnyRef] = receiveN(2)// asserts I get 2 messages and stores them
      // free to do more complicated assertions
    }

    "reply with cool tech in a fancy way" in {
      labTestActor ! "favouriteTech"
      expectMsgPF() {
        case "Scala" => //we only care that the partial function is defined
        case "Akka" =>
      }
    }
  }
}

//store all the values going to be used in the test
object BasicSpec {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()

    override def receive: Receive = {
      case "greeting" =>
        if (random.nextBoolean()) sender() ! "Hi" else sender() ! "Hello"
      case "favouriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase()

    }
  }

}
