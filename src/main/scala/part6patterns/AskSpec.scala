package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.postfixOps
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import akka.pattern.pipe

/**
 * Use ask (?) when you expect a single response
 * Process the future askFuture.onComplete {case ...}
 * Pipe it askFuture.mapTo[String].pipeTo(actor)
 * NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE
 * Avoid closing over the actor instance or mutable state
 *
 * Ask pattern:
 * Step 1: Import akka.pattern.ask
 * Step 2: Implicit timeout and execution context for the ask pattern
 * Step 3: Ask the actor
 * Step 4: Handle the future
 *
 * Pipe pattern: PREFERABLE
 * Step 1: Import akka.pattern.ask and import akka.pattern.pipe
 * Step 2: Implicit timeout and execution context for the ask pattern
 * Step 3: Ask the actor
 * Step 4: Process the future until you get the responses you will send back
 * Step 5: Pipe the resulting future to the actor you want to send the result to
 */
class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._
  import AuthManager._

  "An authentication" should {
    authenticatorTestSuit(Props[AuthManager])
  }

  "A piped authentication" should {
    authenticatorTestSuit(Props[PipedAuthManager])
  }

  def authenticatorTestSuit(props: Props): Unit = {
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("bianca", "rockTheJVM")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("bianca", "rockTheJVM")
      authManager ! Authenticate("bianca", "ILoveAkka")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("bianca", "rockTheJVM")
      authManager ! Authenticate("bianca", "rockTheJVM")
      expectMsg(AuthSuccess)
    }
  }
}

object AskSpec {
  //assume this code is somewhere else in the application

  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())
    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key $key")
        sender() ! kv.get(key)
      case Write(key, value) =>
        log.info(s"Writing the value $value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  //user authenticator actor
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "a system error"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._

    implicit val timeout: Timeout =  Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDb: ActorRef = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) => authDb ! Write(username, password)
      case Authenticate(username, password) => handleAuthentication(username, password)
    }

    def handleAuthentication(username: String, password: String): Unit = {
      val originalSender = sender()
      //to use an ask we need to have implicit timeout and execution context
      val future = authDb ? Read(username)
      future.onComplete {
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword ==  password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) =>
          originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    import AuthManager._

    override def handleAuthentication(username: String, password: String): Unit = {
      val future: Future[Any] = authDb ? Read(username)
      val passwordFuture: Future[Option[String]] = future.mapTo[Option[String]]
      val responseFuture: Future[Product] = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if(dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      }

      //when the future completes, send the response to the actor ref in the arg list
      responseFuture.pipeTo(sender())
    }
  }
}
