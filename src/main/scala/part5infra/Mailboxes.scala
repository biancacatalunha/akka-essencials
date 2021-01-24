package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Mailbox configuration to reorder messages before it's processed, based on configured
 * priority or control messages
 */
object Mailboxes extends App {

  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  /**
   * Use case 1: Custom priority mailbox
   * P0 -> most important
   * P1
   * P2
   * P3
   */

  //step 1 - mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        case message: String if message.startsWith("[P0]") => 0
        case message: String if message.startsWith("[P1]") => 1
        case message: String if message.startsWith("[P2]") => 2
        case message: String if message.startsWith("[P3]") => 3
        case _ => 4
    })

  //step 2 - make it known in the config
  //attach the mailbox to a dispatcher (see application.conf)

  //step 3 - attach the dispatcher to an actor

  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
//  supportTicketLogger ! "[P3] nice to have"
//  supportTicketLogger ! "[P0] needs to be solved now"
//  supportTicketLogger ! "[P1] do when you have time"
  //after which time can I send another message and be prioritize accordingly?

  /**
   * Case 2: Control aware mailbox
   * We'll use unboundedControlAwareMailbox
   */

  //step 1: Mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  //step 2: Configure who gets the mailbox
  //- make the actor attach to the mailbox
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
//  controlAwareActor ! "[P0] needs to be solved now"
//  controlAwareActor ! "[P1] do when you have time"
//  controlAwareActor ! ManagementTicket

  val alternativeControlAwareActor = system.actorOf(Props[SimpleActor], "alternativeControlAwareActor")
  alternativeControlAwareActor ! "[P0] needs to be solved now"
  alternativeControlAwareActor ! "[P1] do when you have time"
  alternativeControlAwareActor ! ManagementTicket
}
