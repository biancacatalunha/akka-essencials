package actortyped

import actortyped.VoteSystem.Citizen.{Vote, VoteStatusReply, VoteStatusRequest}
import actortyped.VoteSystem.Supervisor
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object VoteSystem {

  object Citizen {

    sealed trait Command

    final case class Vote(candidate: String) extends Command

    final case class VoteStatusRequest(replyTo: ActorRef[VoteAggregator.Command]) extends Command with VoteAggregator.Command

    final case class VoteStatusReply(candidate: Option[String], sender: ActorRef[Citizen.Command]) extends Command with VoteAggregator.Command

    def apply(): Behavior[Command] = voteReceived(None)

    def voteReceived(can: Option[String]): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case Vote(candidate) =>
          context.log.info(s"[vote received] Citizen voted for $candidate")
          voteReceived(Some(candidate))
        case VoteStatusRequest(replyTo) =>
          context.log.info(s"[vote received] Sending $can as vote response")
          replyTo ! VoteStatusReply(can, context.self)
          Behaviors.same
      }
    }
  }

  object VoteAggregator {

    sealed trait Command

    final case class AggregateVotes(citizen: Set[ActorRef[Citizen.Command]], replyTo: ActorRef[AggregateVotesReply]) extends Command with Supervisor.Command
    final case class AggregateVotesReply(votes: Map[String, Int]) extends Command with Supervisor.Command

    final case object PrintResults extends Command

    def apply(): Behavior[Command] = aggregateCommand()

    def aggregateCommand(): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case AggregateVotes(citizens, replyTo) =>
          citizens.foreach(citizenRef => citizenRef.ref ! VoteStatusRequest(context.self))
          aggregateStatus(Map(), citizens, replyTo)
      }
    }

    def aggregateStatus(votes: Map[String, Int], remainingCitizens: Set[ActorRef[Citizen.Command]], replyTo: ActorRef[AggregateVotesReply])
    : Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case VoteStatusReply(Some(candidate), sender) =>
          val updatedRemainingCitizens = remainingCitizens - sender
          val updatedNumberOfVotes = votes.getOrElse(candidate, 0) + 1
          val updatedVotes: Map[String, Int] = votes + (candidate -> updatedNumberOfVotes)
          if(updatedRemainingCitizens.isEmpty) {
            context.log.info(s"[aggregateStatus] Poll status: $updatedVotes")
            replyTo ! AggregateVotesReply(updatedVotes)
            Behaviors.stopped
          } else {
            aggregateStatus(updatedVotes, updatedRemainingCitizens, replyTo)
          }
        case VoteStatusReply(None, sender) =>
          sender ! VoteStatusRequest(context.self)
          Behaviors.same
      }
    }
  }

  object Supervisor {
    sealed trait Command

    def apply(): Behavior[Supervisor.Command] =
      Behaviors.setup[Supervisor.Command](context => new Supervisor(context))
  }

  class Supervisor(context: ActorContext[Supervisor.Command]) extends AbstractBehavior[Supervisor.Command](context) {

    import VoteAggregator._

    override def onMessage(msg: Supervisor.Command): Behavior[Supervisor.Command] = {
      msg match {
        case AggregateVotesReply(votes) =>
          context.log.info(s"[Supervisor] Vote result is $votes")
          Behaviors.same
      }
    }

    context.log.info("[Supervisor] started")
    val alice: ActorRef[Citizen.Command] = context.spawn(Citizen(), "alice")
    val bob: ActorRef[Citizen.Command] = context.spawn(Citizen(), "bob")
    val charlie: ActorRef[Citizen.Command] = context.spawn(Citizen(), "charlie")
    val daniel: ActorRef[Citizen.Command] = context.spawn(Citizen(), "daniel")

    alice ! Vote("Martin")
    bob ! Vote("Jonas")
    charlie ! Vote("Roland")
    daniel ! Vote("Roland")

    val voteAggregator: ActorRef[VoteAggregator.Command] = context.spawn(VoteAggregator(), "voteAggregator")
    voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel), context.self)

    Behaviors.same
  }
}

object App {
  def main(args: Array[String]): Unit = {
    ActorSystem[Supervisor.Command](Supervisor(), "iot-system")
  }
}
