package actortyped

import actortyped.BankSystemTypedFunctional.Supervisor
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object BankSystemTypedFunctional {

  object BankAccount {
    sealed trait Command
    final case class DepositMoney(replyTo: ActorRef[Customer.Command], requestId: Long, amount: Int) extends Command with Customer.Command
    final case class WithdrawMoney(replyTo: ActorRef[Customer.Command], requestId: Long, amount: Int) extends Command with Customer.Command
    final case class BankStatementRequest(replyTo: ActorRef[BankStatementReply]) extends Command with Customer.Command
    final case class BankStatementReply(funds: Double) extends Command with Customer.Command
    final case class TransactionSuccess(requestId: Long, message: String) extends Command with Customer.Command
    final case class TransactionFailure(requestId: Long, message: String) extends Command with Customer.Command

    def apply(): Behavior[Command] = processing(0)

    def processing(funds: Double): Behavior[Command] = Behaviors.receive {(_, message) =>
      message match {
        case DepositMoney(replyTo, requestId, amount) =>
          if(amount < 0) {
            replyTo ! TransactionFailure(requestId, "[Bank Account] Invalid deposit amount")
            Behaviors.same
          } else {
            replyTo ! TransactionSuccess(requestId, s"[Bank Account] Successfully deposited $amount")
            processing(funds + amount)
          }

        case WithdrawMoney(replyTo, requestId, amount) =>
          if (funds < amount) {
            replyTo ! TransactionFailure(requestId, "[Bank Account] Not enough funds")
            Behaviors.same
          } else {
            replyTo ! TransactionSuccess(requestId, s"[Bank Account] Successfully withdrawn $amount")
            processing(funds - amount)
          }

        case BankStatementRequest(replyTo) =>
          replyTo ! BankStatementReply(funds)
          Behaviors.same
      }
    }
  }

  object BankManager {
    sealed trait Command
    final case class OpenBankAccount(name: String, replyTo: ActorRef[BankAccountOpened]) extends Command with Supervisor.Command with Customer.Command
    final case class BankAccountOpened(accountRef: ActorRef[BankAccount.Command]) extends Command with Supervisor.Command with Customer.Command
    final case class AccountTerminated(account: String) extends Command with Supervisor.Command
    final case class ListAccounts(replyTo: ActorRef[AccountsListed]) extends Command with Supervisor.Command
    final case class AccountsListed(accounts: Map[String, ActorRef[BankAccount.Command]]) extends Command with Supervisor.Command

    def apply(): Behavior[Command] = processing(Map.empty)

    def processing(accounts: Map[String, ActorRef[BankAccount.Command]]): Behavior[Command] = Behaviors.receive {(context, message) =>
      message match {
        case OpenBankAccount(name, replyTo) =>
          val accountActor = context.spawn(BankAccount(), s"account-$name")
          context.watchWith(accountActor, AccountTerminated(s"account-$name"))
          context.log.info(s"[Bank Manager] Open bank account request received. Client $name")
          replyTo ! BankAccountOpened(accountActor.ref)
          processing(accounts + (name -> accountActor.ref))

        case ListAccounts(replyTo) =>
          context.log.info(s"[Bank Manager] Returning accounts $accounts")
          replyTo ! AccountsListed(accounts)
          Behaviors.same
      }
    }
  }

  object Customer {
    sealed trait Command extends BankAccount.Command

    def apply(): Behavior[Command] = start()

    def start(): Behavior[Command] = Behaviors.receive {(context, message) =>
      import BankAccount._
      import BankManager._

      message match {
        case BankAccountOpened(accountRef) =>
          context.log.info("[Customer] My bank account was opened")
          accountRef ! DepositMoney(context.self, 1, 100)
          accountRef ! BankStatementRequest(context.self)
          accountRef ! WithdrawMoney(context.self, 2, 20)
          accountRef ! BankStatementRequest(context.self)
          accountRef ! WithdrawMoney(context.self, 3, 100)
          accountRef ! BankStatementRequest(context.self)

          operating(accountRef)
      }
    }

    def operating(account: ActorRef[BankAccount.Command]): Behavior[Command] = Behaviors.receive {(context, message) =>
      import BankAccount._

      message match {
        case TransactionSuccess(_, _) =>
          context.log.info("[Customer] My transaction was successful")
          Behaviors.same
        case TransactionFailure(_, _) =>
          context.log.info("[Customer] My transaction was not successful")
          Behaviors.same
        case BankStatementReply(funds) =>
          context.log.info(s"[Customer] I have $funds in my account")
          Behaviors.same
      }
    }
  }

  object Supervisor {
    sealed  trait Command extends BankManager.Command

    def apply(): Behavior[Supervisor.Command] =
      Behaviors.setup[Supervisor.Command](context => new Supervisor(context))
  }

  class Supervisor(context: ActorContext[Supervisor.Command]) extends AbstractBehavior[Supervisor.Command](context) {
    import BankManager._
    override def onMessage(msg: Supervisor.Command): Behavior[Supervisor.Command] = Behaviors.unhandled

    context.log.info("[Supervisor] started")

    val bankManagerActor: ActorRef[Command] = context.spawn(BankManager(), "bankManager")
    val customerActor: ActorRef[Customer.Command] = context.spawn(Customer(), "customer")

    bankManagerActor ! OpenBankAccount("Bianca",  customerActor.ref)
  }
}

object BankSystemApp {
  def main(args: Array[String]): Unit = {
    ActorSystem[Supervisor.Command](Supervisor(), "actor-system")
  }
}
