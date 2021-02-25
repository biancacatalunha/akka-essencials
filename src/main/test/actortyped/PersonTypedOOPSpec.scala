package actortyped

import actortyped.PersonTypedOOP.{Greet, Greeting}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class PersonTypedOOPSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A person actor" must {
    "greet when greeted" in {
      val requester = createTestProbe[PersonTypedOOP.Command]()
      val personActor = spawn(PersonTypedOOP("bianca"))

      personActor ! Greet(requester.ref)

      requester.expectMessage(Greeting("Hi, my name is bianca"))
    }
  }
}
