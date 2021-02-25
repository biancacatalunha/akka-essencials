package actortyped

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

object VendingMachineTypedFunctional {


  sealed trait Command
  case class Initialize(inventory: Map[String, Int], prices: Map[String, Int]) extends Command
  case class RequestProduct(replyTo: ActorRef[Command], product: String) extends Command
  case class Instruction(instruction: String) extends Command
  case class ReceiveMoney(amount: Int) extends Command
  case class Deliver(product: String) extends Command
  case class GiveBackChange(amount: Int) extends Command
  case class VendingError(reason: String) extends Command
  case object ReceiveMoneyTimeout extends Command

  val MACHINE_NOT_INITIALIZED = "MachineNotInitialized"
  val PRODUCT_NOT_AVAILABLE = "ProductNotAvailable"
  val REQUEST_TIMED_OUT = "RequestTimedOut"
  val COMMAND_NOT_FOUND = "CommandNotFound"

  def apply(): Behavior[Command] = idle()

  def idle(): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case Initialize(inventory, prices) =>
      operational(inventory, prices)
    case _ =>
      Behaviors.unhandled
  }

  def operational(inventory: Map[String, Int], prices: Map[String, Int]): Behavior[Command] = Behaviors.receive[Command]{(context, message) =>
    message match {
      case RequestProduct(replyTo, product) =>
        inventory.get(product) match {
          case None | Some(0) =>
            replyTo ! VendingError(PRODUCT_NOT_AVAILABLE)
            Behaviors.same
          case Some(_) =>
            val price = prices(product)
            replyTo ! Instruction(s"Please insert $price euros")
            waitingForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule(context), replyTo)
        }
    }
  }

  def waitingForMoney(
                       inventory: Map[String, Int],
                       prices: Map[String, Int],
                       product: String, money: Int,
                       moneyTimeoutScheduler: Cancellable,
                       requester: ActorRef[Command]): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {

      case ReceiveMoneyTimeout =>
        requester ! VendingError(REQUEST_TIMED_OUT)
        if (money > 0) requester ! GiveBackChange(money)
        operational(inventory, prices)
      case ReceiveMoney(amount) =>
        moneyTimeoutScheduler.cancel()
        val price = prices(product)
        if (money + amount >= price) {
          //user buys the product
          requester ! Deliver(product)


          //deliver change
          if (money + amount - price > 0) {
            requester ! GiveBackChange(money + amount - price)
          }

          //updates inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          operational(newInventory, prices)
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney euros")
          waitingForMoney(inventory, prices, product, money + amount, startReceiveMoneyTimeoutSchedule(context), requester)
        }
    }
  }

  def startReceiveMoneyTimeoutSchedule(context: ActorContext[Command]): Cancellable = {
    context.scheduleOnce(1 second, context.self, ReceiveMoneyTimeout)
  }
}
