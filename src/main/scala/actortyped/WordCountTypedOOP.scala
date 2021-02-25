package actortyped

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object WordCountTypedOOP {

  def apply(): Behavior[WordCountTypedOOP.Command] = {
    Behaviors.setup(context => new WordCountTypedOOP(context))
  }

  sealed trait Command
  final case class CountWords(requestId: Long, message: String, replyTo: ActorRef[Command]) extends Command
  final case class WordsCounted(requestId: Long, count: Long) extends Command
}

class WordCountTypedOOP(context: ActorContext[WordCountTypedOOP.Command]) extends AbstractBehavior[WordCountTypedOOP.Command](context) {
  import WordCountTypedOOP._

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case CountWords(requestId, message, replyTo) =>
        val count = message.split(" ").length
        replyTo ! WordsCounted(requestId, count)
        this
    }
  }
}
