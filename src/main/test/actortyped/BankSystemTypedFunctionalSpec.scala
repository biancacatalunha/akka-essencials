package actortyped

import actortyped.BankSystemTypedFunctional.BankAccount.{BankStatementRequest, DepositMoney, TransactionFailure, TransactionSuccess, WithdrawMoney}
import actortyped.BankSystemTypedFunctional._
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class BankSystemTypedFunctionalSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A bank account actor" should {
    "deposit money" in {
      val requester = createTestProbe[BankAccount.Command]()
      val replyProbe = createTestProbe[BankAccount.BankStatementReply]()
      val accountActor = spawn(BankAccount())

      accountActor ! DepositMoney(requester.ref, 1, 100)
      requester.expectMessageType[TransactionSuccess]

      accountActor ! BankStatementRequest(replyProbe.ref)
      val result = replyProbe.receiveMessage()
      result.funds should ===(100)
    }

    "withdraw money when there's enough funds" in {
      val requester = createTestProbe[BankAccount.Command]()
      val replyProbe = createTestProbe[BankAccount.BankStatementReply]()
      val accountActor = spawn(BankAccount())

      accountActor ! DepositMoney(requester.ref, 1, 100)
      requester.expectMessageType[TransactionSuccess]

      accountActor ! WithdrawMoney(requester.ref, 1, 10)
      requester.expectMessageType[TransactionSuccess]

      accountActor ! BankStatementRequest(replyProbe.ref)
      val result = replyProbe.receiveMessage()
      result.funds should ===(90)
    }

    "refuse withdraw when there isn't enough funds" in {
      val requester = createTestProbe[BankAccount.Command]()
      val replyProbe = createTestProbe[BankAccount.BankStatementReply]()
      val accountActor = spawn(BankAccount())

      accountActor ! WithdrawMoney(requester.ref, 1, 10)
      requester.expectMessageType[TransactionFailure]

      accountActor ! BankStatementRequest(replyProbe.ref)
      val result = replyProbe.receiveMessage()
      result.funds should ===(0)
    }
  }

}
