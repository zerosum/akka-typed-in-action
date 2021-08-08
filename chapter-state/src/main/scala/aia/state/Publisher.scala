package aia.state

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Publisher {

  sealed trait Event
  case class Request(replyTo: ActorRef[Inventory.Event]) extends Event

  def apply(nrLeft: Int, nrBooksPerRequest: Int): Behavior[Event] = {
    updatePublisher(nrLeft, nrBooksPerRequest)
  }

  private def updatePublisher(nrLeft: Int, nrBooksPerRequest: Int): Behavior[Event] = {
    Behaviors.receiveMessage[Event] {
      case Request(replyTo) if nrLeft == 0 =>
        replyTo ! Inventory.BookSupplySoldOut
        Behaviors.same
      case Request(replyTo) =>
        val supply = math.min(nrBooksPerRequest, nrLeft)
        replyTo ! Inventory.BookSupply(supply)
        updatePublisher(nrLeft - supply, nrBooksPerRequest)
    }
  }
}
