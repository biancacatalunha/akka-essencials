package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChildActors.CreditCard.AttachToAccount
import part2actors.ChildActors.Parent.{CreateChild, TellChild}

/*
  actor hierarchy
  parent -> child -> grandchild
         -> child ->

   Guardian actors(top-level)
   - /system = system guardian (manages the logging system)
   - /user = user-level guardian (manages the actors we create)
   - / = root guardian (manages the system and user guardian)
 */
object ChildActors extends App {

  //Actors can create other actors
  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }
  class Parent extends Actor {
    import Parent._

    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")
        //create a new actor
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(ref: ActorRef): Receive = {
      case TellChild(message) => ref forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")

  parent ! CreateChild("child")
  parent ! TellChild("Hey, kid")

  /*
  Actor selection - find an actor by a path
  If the path is not valid, the message will be sent to the dead letter
   */
  val childSelection = system.actorSelection("/user/parent/child")
  childSelection ! "I found you"

  /*
    Danger!
    NEVER PASS MUTABLE ACTOR STATE, OR THE 'THIS' REFERENCE TO CHILD ACTORS
    It breaks encapsulation

    Example:
   */

  object NaiveBankAccount {
    case class Deposit(amount:Int)
    case class Withdraw(amount:Int)
    case object InitializeAccount
  }
  class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._

    var amount = 0
    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) // never pass the actor itself but the actor ref
      case Deposit(funds) =>  deposit(funds)
      case Withdraw(funds) => withdraw(funds)
    }

    def deposit(funds: Int) = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} withdrawing $funds on top of $amount")
      amount -= funds
    }
  }

  object CreditCard {
    case class AttachToAccount(bankAccount: NaiveBankAccount) // should pass an actorRef, never the actor itself!!!
    case object CheckStatus
  }
  class CreditCard extends Actor {
    import CreditCard._

    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachTo(account))
    }

    def attachTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed")
        //benign
        account.withdraw(1) //that's the problem. Cannot call a method of an actor
    }
  }

  import NaiveBankAccount._
  import CreditCard._

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "naiveBankAccount")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(500)

  val ccSelection = system.actorSelection("/user/account/card")
  ccSelection ! CheckStatus

}
