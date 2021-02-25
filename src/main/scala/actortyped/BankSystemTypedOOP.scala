package actortyped

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object BankSystemTypedOOP {
  object BankAccountTyped {

    def apply(): Behavior[BankAccountTyped.Command] = {
      Behaviors.setup(context => new BankAccountTyped(context))
    }

    sealed trait Command
    final case class DepositMoney(replyTo: ActorRef[BankAccountTyped.Command], requestId: Long, amount: Int) extends Command
    final case class WithdrawMoney(replyTo: ActorRef[BankAccountTyped.Command], requestId: Long, amount: Int) extends Command
    final case class BankStatementRequest(replyTo: ActorRef[BankAccountTyped.BankStatementReply]) extends Command
    final case class BankStatementReply(funds: Double) extends Command
    final case class TransactionSuccess(requestId: Long, message: String) extends Command
    final case class TransactionFailure(requestId: Long, message: String) extends Command
  }

  class BankAccountTyped(context: ActorContext[BankAccountTyped.Command])
    extends AbstractBehavior[BankAccountTyped.Command](context) {
    import BankAccountTyped._

    var funds = 0

    override def onMessage(msg: BankAccountTyped.Command): Behavior[BankAccountTyped.Command] =
      msg match {
        case DepositMoney(replyTo, requestId, amount) =>
          if(amount < 0)
            replyTo ! TransactionFailure(requestId, "Invalid deposit amount")
          else {
            funds += amount
            replyTo ! TransactionSuccess(requestId, s"Successfully deposited $amount")
          }
          this

        case WithdrawMoney(replyTo, requestId, amount) =>
          if(amount > funds)
            replyTo ! TransactionFailure(requestId, "Invalid funds")
          else {
            funds -= amount
            replyTo ! TransactionSuccess(requestId, s"Successfully withdrawn $amount")
          }
          this

        case BankStatementRequest(replyTo) =>
          replyTo ! BankStatementReply(funds)
          this

      }
  }

  object BankManager {

    def apply(): Behavior[BankManager.Command] = {
      Behaviors.setup(context => new BankManager(context))
    }

    sealed trait Command
    final case class OpenBankAccount(name: String, replyTo: ActorRef[BankAccountOpened]) extends Command
    final case class AccountTerminated(account: String) extends Command
    final case class BankAccountOpened(id: String) extends Command
    final case class ListAccounts(replyTo: ActorRef[AccountsListed]) extends Command
    final case class AccountsListed(accounts: Map[String, ActorRef[BankAccountTyped.Command]]) extends Command
  }
  class BankManager(context: ActorContext[BankManager.Command]) extends AbstractBehavior[BankManager.Command](context) {
    import BankManager._

    var accounts: Map[String, ActorRef[BankAccountTyped.Command]] = Map.empty

    override def onMessage(msg: BankManager.Command): Behavior[BankManager.Command] = {
      msg match {
        case OpenBankAccount(name, replyTo) =>
          val accountActor: ActorRef[BankAccountTyped.Command] = context.spawn(BankAccountTyped(), s"account-$name")
          context.watchWith(accountActor, AccountTerminated(s"account-$name"))
          context.log.info(s"Open bank account request received. Client $name")
          accounts += (name -> accountActor.ref)
          replyTo ! BankAccountOpened(s"account-$name")
          Behaviors.same

        case ListAccounts(replyTo) =>
          replyTo ! AccountsListed(accounts)
          Behaviors.same
      }
    }
  }
}
