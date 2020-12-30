package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Random

class SendingActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A Sending Actor" must {
    "send a message to another actor when it has finished processing" in {
      import SendingActor._

      val probe        = testKit.createTestProbe[SortedEvents]()
      val sendingActor = testKit.spawn(SendingActor())

      val size         = 1000
      val maxInclusive = 100000

      def randomEvents() = (0 until size).map { _ =>
        Event(Random.nextInt(maxInclusive))
      }.toVector

      val unsorted   = randomEvents()
      val sortEvents = SortEvents(unsorted, probe.ref)
      sendingActor ! sortEvents

      val events = probe.receiveMessage().sorted
      events.size should be(size)
      unsorted.sortBy(_.id) should be(events)
    }
  }
}

object SendingActor {

  sealed trait Command
  case class Event(id: Long)                                                       extends Command
  case class SortEvents(unsorted: Vector[Event], receiver: ActorRef[SortedEvents]) extends Command
  case class SortedEvents(sorted: Vector[Event])                                   extends Command

  def apply(): Behavior[SendingActor.Command] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case SortEvents(unsorted, receiver) =>
        receiver ! SortedEvents(unsorted.sortBy(_.id))
        Behaviors.same
    }
  }
}
