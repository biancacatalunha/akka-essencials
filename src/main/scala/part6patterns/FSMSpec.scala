package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{DurationDouble, DurationInt}

/**
 * FSM is used when context.become gets too complicated
 * startWith(InitialState, InitialData)
 * when(Something) - when a message is received, an event is trigged
 * goto(Something) using someOtherData / stay()
 * initialize()
 */
class FSMSpec extends TestKit(ActorSystem("FSMSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll with OneInstancePerTest {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import FSMSpec._

  def runTestSuite(props: Props): Unit = {
    "error when not initialized" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError(MACHINE_NOT_INITIALIZED))
    }

    "report a product not available" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("sandwich")
      expectMsg(VendingError(PRODUCT_NOT_AVAILABLE))
    }

    "throw a timeout if I don't insert money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 1 euros"))
      within(1.5 seconds) {
        expectMsg(VendingError(REQUEST_TIMED_OUT))
      }
    }

    "handle the reception of partial money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 euros"))
      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction("Please insert 2 euros"))

      within(1.5 seconds) {
        expectMsg(VendingError(REQUEST_TIMED_OUT))
        expectMsg(GiveBackChange(1))
      }
    }

    "deliver the product if I insert all the money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 euros"))
      vendingMachine ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
    }

    "give back change and be able to request money for a new product" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 euros"))
      vendingMachine ! ReceiveMoney(4)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(1))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 euros"))
    }
  }

  "A vending machine" should {
    runTestSuite(Props[VendingMachine])
  }

  "A vending FSM machine" should {
    runTestSuite(Props[VendingMachineFSM])
  }
}

object FSMSpec {
  /**
   * Vending Machine
   * VM - Initialize
   * User - RequestProduct
   * VM - Instruction
   * User - ReceiveMoney
   * VM - Deliver and GiveBackMoney or VendingError and ReceiveMoneyTimeout
   */

  val MACHINE_NOT_INITIALIZED = "MachineNotInitialized"
  val PRODUCT_NOT_AVAILABLE = "ProductNotAvailable"
  val REQUEST_TIMED_OUT = "RequestTimedOut"
  val COMMAND_NOT_FOUND = "CommandNotFound"


  case class Initialize(inventory: Map[String, Int], prices: Map[String, Int])
  case class RequestProduct(product: String)
  case class Instruction(instruction: String)
  case class ReceiveMoney(amount: Int)
  case class Deliver(product: String)
  case class GiveBackChange(amount: Int)
  case class VendingError(reason: String)
  case object ReceiveMoneyTimeout

  class VendingMachine extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContextExecutor = context.dispatcher

    override def receive: Receive = idle

    def idle: Receive = {
      case Initialize(inventory, prices) => context.become(operational(inventory, prices))
      case _ => sender() ! VendingError(MACHINE_NOT_INITIALIZED)
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) =>
        inventory.get(product) match {
          case None | Some(0) => sender() ! VendingError(PRODUCT_NOT_AVAILABLE)
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert $price euros")
            context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
        }
    }

    def waitForMoney(
                      inventory: Map[String, Int],
                      prices: Map[String, Int],
                      product: String,
                      money: Int,
                      moneyTimeoutScheduler: Cancellable,
                      requester: ActorRef): Receive = {
      case ReceiveMoneyTimeout =>
        requester ! VendingError(REQUEST_TIMED_OUT)
        if(money > 0) requester ! GiveBackChange(money)
        context.become(operational(inventory, prices))
      case ReceiveMoney(amount) =>
        moneyTimeoutScheduler.cancel()
        val price = prices(product)
        if(money + amount >= price) {
          //user buys the product
          requester ! Deliver(product)

          //deliver change
          if(money + amount - price > 0) {
            requester ! GiveBackChange(money + amount - price)

            //updates inventory
            val newStock = inventory(product) - 1
            val newInventory = inventory + (product -> newStock)
            context.become(operational(newInventory, prices))
          }
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney euros")
          context.become(waitForMoney(inventory, prices, product, //don't change
            money + amount, //user has inserted some money
            startReceiveMoneyTimeoutSchedule, //I need to set the timeout again
            requester))
        }

    }

    def startReceiveMoneyTimeoutSchedule: Cancellable = context.system.scheduler.scheduleOnce(1 second) {
      self ! ReceiveMoneyTimeout
    }
  }

  //step 1. define states and the data of the actor
  trait VendingState
  case object Idle extends VendingState
  case object Operational extends VendingState
  case object WaitingForMoney extends VendingState

  trait VendingData
  case object Uninitialized extends VendingData
  case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData
  case class WaitingForMoneyData(inventory: Map[String, Int], prices: Map[String, Int], product: String, money: Int, requester: ActorRef) extends VendingData

  /**
   * FSM is an actor with some methods. Doesn't handle messages directly. It triggers an event
   * The event has a state and data
   * event => state and data can be changed
   *
   * state => Idle
   * data => Uninitialized
   *
   * event(Initialize(Map("coke", 10), Map("coke", 1)))
   *  =>
   *  state = Operational
   *  data = Initialized(Map("coke", 10), Map("coke", 1)))
   *
   * event(RequestProduct("coke")
   *  =>
   *  state = WaitForMoney
   *  data = WaitForMoneyData(Map("coke", 10), Map("coke", 1), coke, 0, R)
   *
   * event(ReceiveMoney(2)
   *  =>
   *  state = Operational
   *  data = Initialized(Map("coke", 9), Map("coke", 1))
   *
   */

  class VendingMachineFSM extends FSM[VendingState, VendingData] {
    startWith(Idle, Uninitialized)

    when(Idle) {
      case Event(Initialize(inventory, prices), Uninitialized) =>
        //equivalent to context.become(operational(inventory, prices))
        goto(Operational) using Initialized(inventory, prices)
      case _ =>
        sender() ! VendingError(MACHINE_NOT_INITIALIZED)
        stay()
    }

    when(Operational) {
      case Event(RequestProduct(product), Initialized(inventory, prices)) =>
        inventory.get(product) match {
          case None | Some(0) =>
            sender() ! VendingError(PRODUCT_NOT_AVAILABLE)
            stay()
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert $price euros")
            goto(WaitingForMoney) using WaitingForMoneyData(inventory, prices, product, 0, sender())
        }
    }

    when(WaitingForMoney, stateTimeout = 1 second) {
      case Event(StateTimeout, WaitingForMoneyData(inventory, prices, product, money, requester)) =>
        requester ! VendingError(REQUEST_TIMED_OUT)
        if(money > 0) requester ! GiveBackChange(money)
        goto(Operational) using Initialized(inventory, prices)

      case Event(ReceiveMoney(amount), WaitingForMoneyData(inventory, prices, product, money, requester)) =>
        val price = prices(product)
        if (money + amount >= price) {
          //user buys the product
          requester ! Deliver(product)

          //deliver change
          if (money + amount - price > 0) requester ! GiveBackChange(money + amount - price)

          //updates inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)

          goto(Operational) using Initialized(newInventory, prices)
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney euros")
          stay() using WaitingForMoneyData(inventory, prices, product, money + amount, requester)
        }
    }

    whenUnhandled {
      case Event(_, _) =>
        sender() ! VendingError(COMMAND_NOT_FOUND)
        stay()
    }

    onTransition {
      case stateA -> stateB => log.info(s"Transitioning from $stateA to $stateB")
    }

    initialize()
  }

}
