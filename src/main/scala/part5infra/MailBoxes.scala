package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object MailBoxes extends App {
  private val system: ActorSystem = ActorSystem("MailBoxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Message: $message")
    }
  }

  /**
   * Interesting case #1 - custom priority mailbox
   * P0 (-> Most important)
   * P1
   * P2
   * P3
   */
  // Step 1 - Mailbox Definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(PriorityGenerator {
      case message: String if (message.startsWith("[P0]")) => 0
      case message: String if (message.startsWith("[P1]")) => 1
      case message: String if (message.startsWith("[P2]")) => 2
      case message: String if (message.startsWith("[P3]")) => 3
      case _ => 4
    }) {
    // step 2 - make it known in the config (Check the config file)
    // step 3 - attach the dispatcher to an actor
  }

  private val supportTicketActor = system.actorOf(Props[SimpleActor].withDispatcher("support-ticker-dispatcher"))
  supportTicketActor ! PoisonPill
  supportTicketActor ! "[P3] this thing would be nice to have"
  supportTicketActor ! "[P0] this needs to be solved"
  supportTicketActor ! "[P1] Do this when you have time"

  /**
   * Interesting case #2 - control-aware mailbox
   * we'll use UnboundedControlAwareMailbox
   */
  // Step 1 - mark important messages as control messages
  private case object ManagementTicket extends ControlMessage

  /*
    Step 2 - configure who gets the mailbox
    - make the actor attach to the mailbox
   */
  // method #1 - config
  private val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"), "ControlAwareActor")
  controlAwareActor ! "[P3] this thing would be nice to have"
  controlAwareActor ! "[P0] this needs to be solved"
  controlAwareActor ! ManagementTicket

  // method #2 - using deployment config
  private val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlAwareActor")
  altControlAwareActor ! "[P3] this thing would be nice to have"
  altControlAwareActor ! "[P0] this needs to be solved"
  altControlAwareActor ! ManagementTicket
}
