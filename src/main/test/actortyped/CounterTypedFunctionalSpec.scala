package actortyped

import actortyped.CounterTypedFunctional.{Decrement, GetCount, Increment, ReplyCount}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class CounterTypedFunctionalSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "a Counter Actor" must {
    "reply with the correct count" in {
      val requester = createTestProbe[CounterTypedFunctional.Command]()
      val actor = spawn(CounterTypedFunctional())

      (1 to 5).foreach(_ => actor ! Increment)
      (1 to 3).foreach(_ => actor ! Decrement)
      actor ! GetCount(requester.ref)

      requester.expectMessage(ReplyCount(2))
    }
  }
}
