package actortyped

import actortyped.WordCountTypedOOP.{Command, CountWords, WordsCounted}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class WordCountTypedOOPSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A word count actor" must {
    "count words in a message" in {
      val probe = createTestProbe[Command]()
      val actor = spawn(WordCountTypedOOP())

      actor ! CountWords(1, "I believe I can fly", probe.ref)
      probe.expectMessage(WordsCounted(1, 5))
    }
  }
}
