package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
  // 1. Inline configuration
  val configurationString =
    """
      |akka {
      |   loglevel = "INFO"
      |}
      |""".stripMargin
  val config = ConfigFactory.parseString(configurationString)
  val system = ActorSystem("configurationDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])

  actor ! "A message to remember"

  // 2. Config file called application.conf under resources
  // This is the default

  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])

  defaultConfigActor ! "Remember me?"

  // 3. Separate configuration in the same file

  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("specialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])

  specialConfigActor ! "I am here"

  // 4. Separate config in another file

  val separateConfig = ConfigFactory.load("secretFolder/secretConfig.conf")
  println(s"Separate config ${separateConfig.getString("akka.loglevel")}")

  // 5. Different file formats e.g JSON, properties

  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"json config ${jsonConfig.getString("aJsonProperty")}")
  println(s"json config ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"properties config ${propsConfig.getString("my.simpleProperty")}")
  println(s"properties config ${propsConfig.getString("akka.loglevel")}")
}
