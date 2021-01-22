package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, PoisonPill, Props, Timers}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/*
Things to bear in mind:
- Don't use unstable references inside scheduled actions
- All scheduled tasks execute when the system is terminated
- Schedulers are not the best a precision and long-term planning
 */
object TimerSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimerDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  system.log.info("Scheduling reminder for simpleActor")

  //option2
//  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  //option 3
  import system.dispatcher

  //schedule an action at a defined point in the future
  system.scheduler.scheduleOnce(1 second){
    simpleActor ! "reminder"
  }
//  (system.dispatcher) //option 1

  //repeated action
  val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds) {
    simpleActor ! "heartbeat"
  }

  system.scheduler.scheduleOnce(5 seconds) {
    routine.cancel()
  }

  /*
  Exercise:
  Implement a self-closing actor
  - If the actor receives a message (anything), you have 1 second to send it another message
  - If the time window expires, the actor will stop itself
  - If you send another message, the time window is reset
   */

  class SelfClosingActor extends Actor with ActorLogging {
    var schedule: Cancellable = createTimeOutWindow()

    def createTimeOutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }
    }
    override def receive: Receive = {
      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Received $message")
        schedule.cancel() //cancel a schedule
        schedule = createTimeOutWindow()
    }
  }

//  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
//  selfClosingActor ! "Hi"
//  system.scheduler.scheduleOnce(10000 millis) {
//    selfClosingActor ! "Hello again"
//  }

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop

  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {
    //schedule messages to self, from within
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startTimerWithFixedDelay(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.info("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerBasedHeartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor], "timerActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerBasedHeartbeatActor ! Stop
  }

}
