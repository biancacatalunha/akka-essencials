package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

/**
  Supported options for routing logic:
  - Round-robin: Cycles between routees
  - Random: Not very smart
  - Smallest mailbox: Always sends the next message to the actor with fewer messages in the queue
  - Broadcast: Sends the same message to all the routees
  - Scatter-gather-first: Broadcasts and waits for the first reply and all the other replies aer discarded
  - Tail-chopping: Forwards the message to each actor sequentially until the first reply is received and
  all the other replies are discarded
  - Consistent-hashing: All the messages with the same hash gets to the same actor

  PoisonPill and Kill are NOT routed
  AddRoutee, Remove, Get are handled only by the routing actor
 */
object Routers extends App {

  /**
   *   Method 1. Manual router
   */

  class Master extends Actor {
    //step 1 create routees
    private val slaves = for(i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    //step 2. define the router
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = onMessage(router)

    private def onMessage(router: Router): Receive = {
      //step 3. route the messages
      case message => router.route(message, sender())
      //step 4. handle the termination/lifecycle of the routees
      case Terminated(ref) =>
        context.become(onMessage(router.removeRoutee(ref)))
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        context.become(onMessage(router.addRoutee(newSlave)))
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master], "master")

//  for(i <- 1 to 10) {
//    master ! s"[$i] Hello from the world"
//  }

  /**
   * Method 2. Router actor with it's own children (Pool router)
   */

  //2.1 programmatically(in code)
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePooMaster")

//  for(i <- 1 to 10) {
//    poolMaster ! s"[$i] Hello from the world"
//  }

  //2.2 from configuration
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]()), "poolMaster2")
//  for(i <- 1 to 10) {
//    poolMaster ! s"[$i] Hello from the world"
//  }

  /**
   * Method 3. Router with actors created elsewhere (Group router)
   */

  //... in another part of my application
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList

  //need their paths as string for the group router
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  //it doesn't need any props because the actors have already been created
  //3.1 in the code
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())

//  for(i <- 1 to 10) {
//    groupMaster ! s"[$i] Hello from the world"
//  }

  //3.2 from configuration
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")
  for(i <- 1 to 10) {
    groupMaster2 ! s"[$i] Hello from the world"
  }

  //Handling special message
  groupMaster2 ! Broadcast("hello, everyone")
}
