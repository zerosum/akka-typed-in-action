package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

class FilteringActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A Filtering Actor" must {

    "filter out particular messages" in {
      import FilteringActor._

      val probe  = testKit.createTestProbe[Event]("probe-1")
      val filter = testKit.spawn(FilteringActor(5, probe.ref), "filter-1")

      filter ! Event(1)
      filter ! Event(2)
      filter ! Event(1)
      filter ! Event(3)
      filter ! Event(1)
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      filter ! Event(6)

      probe.receiveMessages(5).map(_.id) should be(List(1, 2, 3, 4, 5))
      probe.expectMessage(Event(6))

    }

    "filter out particular messages using expectNoMsg" in {
      import FilteringActor._

      val probe  = testKit.createTestProbe[Event]("probe-2")
      val filter = testKit.spawn(FilteringActor(5, probe.ref), "filter-2")

      filter ! Event(1)
      filter ! Event(2)
      probe.expectMessage(Event(1))
      probe.expectMessage(Event(2))
      filter ! Event(1)
      probe.expectNoMessage()
      filter ! Event(3)
      probe.expectMessage(Event(3))
      filter ! Event(1)
      probe.expectNoMessage()
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      probe.expectMessage(Event(4))
      probe.expectMessage(Event(5))
      probe.expectNoMessage()
    }

  }

}

object FilteringActor {

  case class Event(id: Long)

  def apply(bufferSize: Int, nextActor: ActorRef[Event]): Behavior[Event] = {
    Behaviors.setup(ctx => new FilteringActor(bufferSize, nextActor, ctx))
  }
}

class FilteringActor(
    bufferSize: Int,
    nextActor: ActorRef[FilteringActor.Event],
    ctx: ActorContext[FilteringActor.Event]
) extends AbstractBehavior[FilteringActor.Event](ctx) {
  import FilteringActor._

  private var lastMessages = Vector.empty[Event]

  override def onMessage(msg: Event): Behavior[Event] = {
    msg match {
      case Event(_) =>
        if (!lastMessages.contains(msg)) {
          lastMessages = lastMessages :+ msg
          nextActor ! msg
          if (lastMessages.size > bufferSize) {
            lastMessages = lastMessages.tail
          }
        }
        this
    }
  }
}
