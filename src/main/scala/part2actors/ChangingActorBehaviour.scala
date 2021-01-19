package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehaviour.Mom.MomStart

object ChangingActorBehaviour extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._

    var state: String = HAPPY

    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive) //forces to swap the handler
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLE) =>
      case Food(CHOCOLATE) => context.become(happyReceive)
      case Ask(_) => sender() ! KidReject
    }

    /*
    Changing actor behaviour
    context.become(anotherHandler, true)
    true is the default, it replaces the old handles with the new one
    context.become(anotherHandler, false)
    It it stack the new handler on top of the old one
    context.unbecome()
    Pops the current behaviour off the stack
    Rules:
    - Akka always uses the latest handler on top of the stack
    - if the stack is empty, ut calls receive
     */
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String)
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor {
    import Mom._
    import FussyKid._

    override def receive: Receive = {
      case MomStart(kidRef) =>
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("Do you want to play?")
      case KidAccept => println("Yay, my kid is happy")
      case KidReject => println("My kid is sad, but at least he is healthy")
    }
  }

  val system = ActorSystem("changingActorBehaviour")
  val fussyKid = system.actorOf(Props[FussyKid], "fussyKid")
  val mom = system.actorOf(Props[Mom], "mom")
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "statelessFussyKid")

  mom ! MomStart(statelessFussyKid)
  /*
  Mom receives MomStart
    kid receives Food(veg) -> kid will change the handler to sadReceive
    kid receives Ask(play) -> kid replies with the sadReceive handler
  Mom receives KidReject
   */

}
