package actortyped

import actortyped.BankSystemTypedOOP.BankAccountTyped
import actortyped.BankSystemTypedOOP.BankAccountTyped.{BankStatementRequest, DepositMoney, TransactionFailure, TransactionSuccess, WithdrawMoney}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class BankSystemTypedOOPSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A bank account" should {
    "deposit money correctly" in {
      val requester = createTestProbe[BankAccountTyped.Command]()
      val accountActor = spawn(BankAccountTyped())

      accountActor ! DepositMoney(requester.ref, 1, 100)
      requester.expectMessageType[TransactionSuccess]
    }

    "withdraw money correctly" in {
      val requester = createTestProbe[BankAccountTyped.Command]()
      val accountActor = spawn(BankAccountTyped())

      accountActor ! DepositMoney(requester.ref, 1, 100)
      accountActor ! WithdrawMoney(requester.ref, 1, 10)

      requester.expectMessageType[TransactionSuccess]
    }

    "not withdraw money if not enough funds" in {
      val requester = createTestProbe[BankAccountTyped.Command]()
      val accountActor = spawn(BankAccountTyped())

      accountActor ! WithdrawMoney(requester.ref, 1, 10)

      requester.expectMessageType[TransactionFailure]
    }

    "reply with the correct funds when requested" in {
      val requester = createTestProbe[BankAccountTyped.Command]()
      val replyProbe = createTestProbe[BankAccountTyped.BankStatementReply]()
      val accountActor = spawn(BankAccountTyped())

      accountActor ! DepositMoney(requester.ref, 1, 100)
      accountActor ! BankStatementRequest(replyProbe.ref)

      val result = replyProbe.receiveMessage()
      result.funds should ===(100)
    }
  }
}
