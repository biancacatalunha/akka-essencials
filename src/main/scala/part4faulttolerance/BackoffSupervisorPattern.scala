package part4faulttolerance

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.language.postfixOps

/*
Use case: Imagine the database went down, all actor that connect to that database will restart
it takes time for databases to come back up, and when it does, it might crash again because all
the actor will try to access it at the same time

Solution:
BackoffSupervision for exponential delays between attempts
 */
object BackoffSupervisorPattern extends App {

  case object ReadFile

  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit = {
      log.info("Persistent actor starting")
    }

    override def postStop(): Unit = {
      log.warning("Persistent actor has stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.warning("Persistent actor restart")
    }

    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null) {
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/important.txt"))
          log.info("I've just read some important data: " + dataSource.getLines().toList)
        }
    }
  }

  val system = ActorSystem("BackoffSupervisorDemo")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
//  simpleActor ! ReadFile

  //creates a child
  val simpleSupervisorProps: Props = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      Props[FileBasedPersistentActor],
      "simpleABackoffActor",
      3 seconds, //this strategy will kick in after 3 s then 6s, 12s, 24s
      30 seconds, //cap
      0.2 //minBackoff will be multiplied by this number
    )
  )

  //creates the parent and a child
//  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
//  simpleBackoffSupervisor ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(OneForOneStrategy() {
      case _ => Stop
    })
  )

//  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
//  stopSupervisor ! ReadFile

  class EagerFileBasedPersistentActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/important.txt"))
    }
  }

  //ActorInitializationException => Stop

  //to test this, run the app and rename the txt file from important to important_data
  //you should see the actor failing to initialize
  //attempting again after one second, then 2, then 4, then 8, then 16
  //rename the file within 30 seconds
  //and you will see the actor initializing
  val repeatedSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[EagerFileBasedPersistentActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )

  //attempts a restart
  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")


}
