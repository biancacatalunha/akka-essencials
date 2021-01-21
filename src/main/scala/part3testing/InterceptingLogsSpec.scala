package part3testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

/*
  Test by intercepting a log message and testing around it
  Event Filter needs to be specified in the config file
 */
class InterceptingLogsSpec
  extends TestKit(ActorSystem("InterceptingLogsSpec", ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import InterceptingLogsSpec._

  val item = "Rock the JVM Akka course"
  val creditCard = "1234-1234-1234-1234"
  val invalidCreditCard = "0000-0000-0000-0000"

  "a check out flow" should {
    "correctly log the dispatch of an order" in {
      //scans for info log messages
      EventFilter.info(pattern = s"Order [0-9]+ for item $item has been dispatched", occurrences = 1) intercept {
        val checkoutRef = system.actorOf(Props[CheckOutActor])
        checkoutRef ! Checkout(item, creditCard)
      }
    }

    //expect a runtime exception
    "freak out if the payment is denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        val checkoutRef = system.actorOf(Props[CheckOutActor])
        checkoutRef ! Checkout(item, invalidCreditCard)
      }
    }
  }
}

// Complex interaction between master actor and child actors, hard to test
object InterceptingLogsSpec {
  case class Checkout(item: String, creditCard: String)
  case class AuthorizeCard(creditCard: String)
  case class DispatchOrder(item: String)
  case object PaymentAccepted
  case object PaymentDenied
  case object OrderConfirmed

  class CheckOutActor extends Actor {
    private val paymentManagement = context.actorOf(Props[PaymentManagement], "paymentManager")
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager], "fulfillmentManager")

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, creditCard) =>
        paymentManagement ! AuthorizeCard(creditCard)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
      case PaymentDenied => throw new RuntimeException("I can't handle anymore")
    }

    def pendingFulfillment(item: String): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  class PaymentManagement extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(card) =>
        if(card.startsWith("0")) sender() ! PaymentDenied
        else sender() ! PaymentAccepted
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {
    var orderId = 43

    override def receive: Receive = {
      case DispatchOrder(item) =>
        orderId += 1
        log.info(s"Order $orderId for item $item has been dispatched")
        sender() ! OrderConfirmed
    }
  }
}
