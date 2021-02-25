package actortyped

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object PersonTypedOOP {

  def apply(name: String): Behavior[PersonTypedOOP.Command] = {
    Behaviors.setup(context => new PersonTypedOOP(context, name))
  }

  sealed trait Command
  final case class Greet(replyTo: ActorRef[Greeting]) extends Command
  final case class Greeting(message: String) extends Command
}

class PersonTypedOOP(context: ActorContext[PersonTypedOOP.Command], name: String) extends AbstractBehavior[PersonTypedOOP.Command](context) {
  import PersonTypedOOP._

  override def onMessage(msg: PersonTypedOOP.Command): Behavior[PersonTypedOOP.Command] =
    msg match {
      case Greet(replyTo) =>
        replyTo ! Greeting(s"Hi, my name is $name")
        this
    }
}
