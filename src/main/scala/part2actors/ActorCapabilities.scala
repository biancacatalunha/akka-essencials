package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      //replying to a message. The compiler injects self as the sender
      case "Hi" => context.sender() ! "Hello there"
      case message: String => println(s"[$self] I have received $message")
      case number: Int => println(s"[simple actor] I have received a number: $number")
      case SpecialMessage(contents) => println(s"[simple actor] I have received something special $contents")
      //receives a message of this type, sends it back to itself which will fit in the String case
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi"
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // I keep the original sender of the WPM
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  // messages can be of any type
  // messages must be immutable
  // messages must be serializable (send from one JVM to another)
  // in practice use case classes and case objects
  simpleActor ! "hello, actor"
  simpleActor ! 42

  case class SpecialMessage(contents: String)

  simpleActor ! SpecialMessage("some special message")

  //actors have information about their context and about themselves
  //context - environment, actor system, actor reference - context.self = this in OOP

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I'm an actor and I'm proud of it")

  // actors can reply to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  //dead letter - reply to me but doesn't send a reference
  alice ! "Hi"

  //forwarding messages
  //forwarding = sending a message with its original sender
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob)

}
