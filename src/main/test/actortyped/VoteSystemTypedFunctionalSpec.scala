package actortyped

import actortyped.VoteSystem.{Citizen, VoteAggregator}
import actortyped.VoteSystem.Citizen.{Vote, VoteStatusReply, VoteStatusRequest}
import actortyped.VoteSystem.VoteAggregator.AggregateVotes
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class VoteSystemTypedFunctionalSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A Citizen" should {
    "be able to vote" in {
      val aggregatorProbe = createTestProbe[VoteAggregator.Command]()
      val citizenActor = spawn(Citizen())

      citizenActor ! Vote("Bianca")

      citizenActor ! VoteStatusRequest(aggregatorProbe.ref)

      aggregatorProbe.expectMessage(VoteStatusReply(Some("Bianca"), citizenActor.ref))
    }
  }

  "A vote aggregator" should {
    "request and aggregate votes" in {
      val aggregatorProbe = createTestProbe[VoteAggregator.AggregateVotesReply]()
      val citizen1 = spawn(Citizen())
      val citizen2 = spawn(Citizen())
      val aggregator = spawn(VoteAggregator())

      citizen1 ! Vote("Bianca")
      citizen2 ! Vote("Sheila")
      aggregator ! AggregateVotes(Set(citizen1, citizen2), aggregatorProbe.ref)

      val result = aggregatorProbe.receiveMessage()
      result.votes should ===(Map(("Bianca", 1), ("Sheila", 1)))
    }
  }
}
