package actortyped

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object CounterTypedOOP {

  def apply(): Behavior[CounterTypedOOP.Command] = {
    Behaviors.setup(context => new CounterTypedOOP(context))
  }

  sealed trait Command
  final case object Increment extends Command
  final case object Decrement extends Command
  final case class GetCount(replyTo: ActorRef[Command]) extends Command
  final case class ReplyCount(count: Int) extends Command
}

class CounterTypedOOP(context: ActorContext[CounterTypedOOP.Command])
  extends AbstractBehavior[CounterTypedOOP.Command](context) {
  import CounterTypedOOP._

  var count = 0

  override def onMessage(msg: CounterTypedOOP.Command): Behavior[CounterTypedOOP.Command] = {
    msg match {
      case Increment =>
        count += 1
      case Decrement =>
        count -= 1
      case GetCount(replyTo) =>
        replyTo ! ReplyCount(count)
    }
    this
  }
}

