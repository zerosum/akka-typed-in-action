package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

package silentactor02 {

  import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
  import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
  import org.scalatest.matchers.should

  class SilentActorTest extends AnyWordSpecLike with should.Matchers {

    "A Silent Actor" must {

      "change internal state when it receives a message, single" in {
        import SilentActor._

        val silentActor = BehaviorTestKit(SilentActor())
        silentActor.run(SilentMessage("whisper"))
//        silentActor.underlyingActor.state should (contain("whisper"))
      }

    }
  }

  object SilentActor {

    sealed trait Command
    case class SilentMessage(data: String)          extends Command
    case class GetState(replyTo: ActorRef[Command]) extends Command

    def apply(): Behavior[Command] = Behaviors.setup(ctx => new SilentActor(ctx))
  }

  class SilentActor(ctx: ActorContext[SilentActor.Command]) extends AbstractBehavior[SilentActor.Command](ctx) {
    import SilentActor._

    private var internalState: Vector[String] = Vector.empty

    override def onMessage(msg: SilentActor.Command): Behavior[SilentActor.Command] = {
      msg match {
        case SilentMessage(data) =>
          internalState = internalState :+ data
          Behaviors.same
      }
    }

    def state: Vector[String] = internalState
  }
}

package silentactor03 {

  class SilentActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

    "A Silent Actor" must {

      "change internal state when it receives a message, multi" in {
        import SilentActor._

        val silentActor = testKit.spawn(SilentActor(), "s3")
        val probe       = testKit.createTestProbe[Vector[String]]()
        silentActor ! SilentMessage("whisper1")
        silentActor ! SilentMessage("whisper2")
        silentActor ! GetState(probe.ref)
        probe.expectMessage(Vector("whisper1", "whisper2"))
      }

    }

  }

  object SilentActor {
    sealed trait Command
    case class SilentMessage(data: String)                  extends Command
    case class GetState(receiver: ActorRef[Vector[String]]) extends Command

    def apply(): Behavior[Command] = update(Vector.empty[String])

    private def update(internalState: Vector[String]): Behavior[Command] = {
      Behaviors.receiveMessagePartial {
        case SilentMessage(data) =>
          update(internalState :+ data)

        case GetState(receiver) =>
          receiver ! internalState
          Behaviors.same
      }
    }
  }

}
