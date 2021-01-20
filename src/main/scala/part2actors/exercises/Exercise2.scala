package part2actors.exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Exercise2 extends App {

  val actorSystem = ActorSystem("actorSystem")

  /*
  1. Recreate the counter actor with context become and no mutable state
   */

  object Counter {

    case object Increment

    case object Decrement

    case object Print

  }

  class Counter extends Actor {

    import Counter._

    override def receive: Receive = countReceive(0)

    def countReceive(currentCount: Int): Receive = {
      case Increment =>
        println(s"[$currentCount] incrementing")
        context.become(countReceive(currentCount + 1))
      case Decrement =>
        println(s"[$currentCount] decrementing")
        context.become(countReceive(currentCount - 1))
      case Print => println(s"[counter] My current count is $currentCount")

    }
  }

  import Counter._

  val counter = actorSystem.actorOf(Props[Counter], "myCounter")
  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  /*
  2. Simplified voting system
  Two actors
  mark the citizen to have voted -> state of having voted
  vote aggregator will send messages to the citizen asking who they have voted for
   */

  object Citizen {

    case class Vote(candidate: String)

    case object VoteStatusRequest

    case class VoteStatusReply(candidate: Option[String])

  }

  class Citizen extends Actor {

    import Citizen._

    override def receive: Receive = voteReceived(None)

    def voteReceived(can: Option[String]): Receive = {
      case Vote(candidate) =>
        println(s"[vote received] Citizen voted for $candidate")
        context.become(voteReceived(Some(candidate)))
      case VoteStatusRequest =>
        println(s"[vote received] Sending $can as vote response")
        sender() ! VoteStatusReply(can)
    }
  }

  object VoteAggregator {

    case class AggregateVotes(citizens: Set[ActorRef])

    case object PrintResults

  }

  class VoteAggregator extends Actor {

    import Citizen._
    import VoteAggregator._

    override def receive: Receive = aggregateCommand()

    def aggregateCommand(): Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
        context.become(aggregateStatus(Map(), citizens))
    }

    def aggregateStatus(votes: Map[String, Int], remainingCitizens: Set[ActorRef]): Receive = {
      case VoteStatusReply(Some(candidate)) =>
        val updatedRemainingCitizens = remainingCitizens - sender()
        val updatedNumOfVotes = votes.getOrElse(candidate, 0) + 1
        val updatedVotes: Map[String, Int] = votes + (candidate -> updatedNumOfVotes)
        if (updatedRemainingCitizens.isEmpty) {
          println(s"[aggregatorStatus] Poll status: $updatedVotes")
        } else {
          context.become(aggregateStatus(updatedVotes, updatedRemainingCitizens))
        }
      case VoteStatusReply(None) => sender() ! VoteStatusRequest
      case _ => println("Something else happened")
    }
  }

  import Citizen._
  import VoteAggregator._

  val alice = actorSystem.actorOf(Props[Citizen], "alice")
  val bob = actorSystem.actorOf(Props[Citizen], "bob")
  val charlie = actorSystem.actorOf(Props[Citizen], "charlie")
  val daniel = actorSystem.actorOf(Props[Citizen], "daniel")

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = actorSystem.actorOf(Props[VoteAggregator], "voteAggregator")
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))
  /*
  print the status of the votes
    Martin ->  1
    Jonas -> 1
    Roland -> 2
   */
}
