package actortyped

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object CounterTypedFunctional {

  sealed trait Command
  final case object Increment extends Command
  final case object Decrement extends Command
  final case class GetCount(replyTo: ActorRef[Command]) extends Command
  final case class ReplyCount(count: Int) extends Command

  def apply(): Behavior[Command] = countReceive(0)

  def countReceive(currentCount: Int): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case Increment =>
        countReceive(currentCount + 1)
      case Decrement =>
        countReceive(currentCount - 1)
      case GetCount(replyTo) => replyTo ! ReplyCount(currentCount)
        Behaviors.same
    }
  }
}