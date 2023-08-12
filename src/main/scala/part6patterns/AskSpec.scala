package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import part6patterns.AskSpec.AuthManager.{AUTH_FAILURE_NOT_FOUND, AUTH_FAILURE_SYSTEM, AUTH_FAILURE_WRONG_PASSWORD}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
// step 1 - import ask pattern
import akka.pattern.ask
import akka.pattern.pipe // for pipeTo method
import scala.concurrent.duration._

import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import scala.util.{Success, Failure}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "an Authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "an Piped Authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }

  private def authenticatorTestSuite(props: Props): Unit = {
    import AuthManager._

    "fail to pipe authenticate a non-registered user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("Test", "TestPassword")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to pipe authenticate if password is not valid" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("Test", "TestPassword")
      authManager ! Authenticate("Test", "WrongPassword")
      expectMsg(AuthFailure(AUTH_FAILURE_WRONG_PASSWORD))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("Test", "TestPassword")
      authManager ! Authenticate("Test", "TestPassword")
      expectMsg(AuthSuccess)
    }
  }
}

object AskSpec {
  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    private def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key, value) =>
        log.info(s"Writing the value $value for the $key")
        context.become(online(kv + (key -> value)))
    }
  }

  // user authenticator actor
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "User not found!"
    val AUTH_FAILURE_WRONG_PASSWORD = "Password is wrong!"
    val AUTH_FAILURE_SYSTEM = "System Error!"
  }
  class AuthManager extends Actor with ActorLogging {
    // step 2 - logistics
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher
    protected val authDb: ActorRef = context.actorOf(Props[KVActor])
    override def receive: Receive = {
      case RegisterUser(username, password) => authDb ! Write(username, password)
      case Authenticate(username, password) => handleAuthentication(username, password)

    }

    protected def handleAuthentication(username: String, password: String): Unit = {
      val originalSender = sender()
      // step 3 - ask the actor
      val future = authDb ? Read(username)
      // Step 4 - handle the future for e.g. with onComplete
      future.onComplete {
        // Step - 5 - NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN COMPLETE
        // avoid closing over the actor instance or mutable state
        case Success(None) =>
          originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_WRONG_PASSWORD)
        case Failure(_) =>
          originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    override protected def handleAuthentication(username: String, password: String): Unit = {
      // step 3 - ask the actor
      val future = authDb ? Read(username)
      // step 4 - process the future until you get the response you will send back
      val passwordFuture = future.mapTo[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password)  AuthSuccess
          else AuthFailure(AUTH_FAILURE_WRONG_PASSWORD)
      } // Future[AuthSuccess/AuthFailure] = Future[Any]

      // step 5 - pipe the resulting to the actor you want to send the result to
      /* When the future completes, send the response to the actor ref in the arg list */
      responseFuture.pipeTo(sender())

    }
  }

}
