package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.language.postfixOps
import scala.util.{Failure, Success}

class EchoActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import EchoActor._

  "An EchoActor" must {
    "Reply with the same message it receives" in {
      import akka.actor.typed.scaladsl.AskPattern.Askable
      import scala.concurrent.duration._

      implicit val timeout   = Timeout(3 seconds)
      implicit val ec        = testKit.system.executionContext
      implicit val scheduler = testKit.system.scheduler

      val echo   = testKit.spawn(EchoActor())
      val future = echo.ask(Message("some message", _)).mapTo[String]
      future.onComplete {
        case Failure(_)   => // 失敗時の制御
        case Success(msg) => // 成功時の制御
      }

      Await.ready(future, timeout.duration)
    }

    "Reply with the same message it receives without ask" in {
      val echo  = testKit.spawn(EchoActor())
      val probe = testKit.createTestProbe[String]()

      echo ! Message("some message", probe.ref)
      probe.expectMessage("some message")

    }

  }
}

object EchoActor {

  case class Message(msg: String, sender: ActorRef[String])

  def apply(): Behavior[Message] = Behaviors
    .receiveMessagePartial { case Message(msg, sender) =>
      sender ! msg
      Behaviors.same
    }
}
