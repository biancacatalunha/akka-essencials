package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.Exercise1.BankAccount.{BankStatement, DepositMoney, WithdrawMoney}
import part2actors.Exercise1.Counter.{Decrement, Increment, Print}
import part2actors.Exercise1.Person.LiveTheLife

object Exercise1 extends App {


  /**
   * 1. Create a counter actor to holder an internal counter, which will respond to:
   *  - Increment
   *  - Decrement
   *  - Print
   */

  //my solution
  case class IncrementOne()
  case class DecrementOne()
  case class PrintCount()

  class CounterActor extends Actor {
    var counter = 0

    override def receive: Receive = {
      case IncrementOne() => counter += 1
      case DecrementOne() => counter -= 1
      case PrintCount => println(s"[counter actor] Counter is $counter")
    }
  }

  val actorSystem = ActorSystem("actorSystem")
  val counterActor = actorSystem.actorOf(Props[CounterActor], "counterActor")

  counterActor ! IncrementOne()
  counterActor ! PrintCount
  counterActor ! DecrementOne()
  counterActor ! PrintCount

  //Teacher's solution
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._

    var count = 0
    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"[counter] My current count is $count")
    }
  }

  val counter = actorSystem.actorOf(Props[Counter], "myCounter")
  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print


  /**
   *  2. Bank account as an actor, hold an internal balance, which will react to:
   *  - Deposit an amount
   *  - Withdraw an amount
   *  - Statement
   *  replies with
   *  - Success
   *  - Failure
   *
   *  interact with some other kind of actor
   */

  case class Deposit(amount: Double, ref: ActorRef)
  case class Withdraw(amount: Double, ref: ActorRef)
  case class Statement()
  case class Success()
  case class Failure()

  class UserActor() extends Actor {
    override def receive: Receive = {
      case Success => println("[user actor] Success, I'm going home")
      case Failure => println("[user actor] Failure, I'm calling the bank")
    }
  }

  class BankAccountActor extends Actor {
    var balance = 100.0
    override def receive: Receive = {
      case Withdraw(amount, ref) =>
        if(balance >= amount) {
          balance -= amount
          ref ! Success
        }
        ref ! Failure
      case Deposit(amount, ref) =>
        balance += amount
        ref ! Success
      case Statement => println(s"[bank account actor] Your balance is $balance")
    }
  }

  val bankAccountActor = actorSystem.actorOf(Props[BankAccountActor], "bankAccountActor")
  val userActor = actorSystem.actorOf(Props[UserActor], "userActor")

  bankAccountActor ! Withdraw(200.0, userActor)
  bankAccountActor ! Statement
  bankAccountActor ! Deposit(10.0, userActor)
  bankAccountActor ! Statement

  //Teacher's solution
  object BankAccount {
    case class DepositMoney(amount: Int)
    case class WithdrawMoney(amount: Int)
    case object BankStatement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
  }

  class BankAccount extends Actor {
    import BankAccount._
    var funds = 0
    override def receive: Receive = {
      case DepositMoney(amount) =>
        if(amount < 0) sender() ! TransactionFailure("Invalid deposit amount")
        else {
          funds += amount
          sender() ! TransactionSuccess(s"Successfully deposited $amount")
        }
      case WithdrawMoney(amount) =>
        if(amount < 0) sender() ! TransactionFailure("Invalid withdraw amount")
        else if (amount > funds) sender() ! TransactionFailure("Insufficient funds")
        else {
          funds -= amount
          sender() ! TransactionSuccess(s"Successfully withdrew $amount")
        }
      case BankStatement => sender() ! s"Your balance is $funds"
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor {
    import Person._
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! DepositMoney(10000)
        account ! WithdrawMoney(90000)
        account ! WithdrawMoney(500)
        account ! BankStatement
      case message => println(message.toString)
    }
  }

  val account = actorSystem.actorOf(Props[BankAccount], "bankAccount")
  val person = actorSystem.actorOf(Props[Person], "person")

  person ! LiveTheLife(account)
}
