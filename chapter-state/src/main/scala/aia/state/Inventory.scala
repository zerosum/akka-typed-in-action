package aia.state

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Inventory {

  sealed trait Response
  case object PublisherRequest                         extends Response
  case class BookReply(reserveId: Either[String, Int]) extends Response

  sealed trait Event
  case class BookRequest(target: ActorRef[Response]) extends Event
  case object PendingRequest                         extends Event
  case class BookSupply(nrBooks: Int)                extends Event
  case object BookSupplySoldOut                      extends Event
  case object Done                                   extends Event

  case class StateData(
      self: ActorRef[Event],
      publisher: ActorRef[Response],
      reserveId: Int,
      nrBooksInStore: Int,
      pendingRequests: Seq[BookRequest]
  )

  def apply(): Behavior[Event] = {
    waitForRequests(StateData(???, ???, 0, 0, Nil))
  }

  def waitForRequests(state: StateData): Behavior[Event] = {
    if (state.pendingRequests.nonEmpty) {
      state.self ! PendingRequest
    }

    Behaviors.receiveMessage[Event] {
      case request: BookRequest =>
        val newStateData = state.copy(pendingRequests = state.pendingRequests :+ request)
        if (newStateData.nrBooksInStore > 0) {
          processRequest(newStateData)
        } else {
          waitForPublisher(newStateData)
        }

      case PendingRequest =>
        if (state.pendingRequests.isEmpty) {
          Behaviors.same
        } else if (state.nrBooksInStore > 0) {
          processRequest(state)
        } else {
          waitForPublisher(state)
        }
    }
  }

  def waitForPublisher(data: StateData): Behavior[Event] = {
    data.publisher ! PublisherRequest

    Behaviors.receiveMessage[Event] {
      case supply: BookSupply =>
        processRequest(data.copy(nrBooksInStore = supply.nrBooks))

      case BookSupplySoldOut =>
        processSoldOut(data)
    }
  }

  def processRequest(data: StateData): Behavior[Event] = {
    val request      = data.pendingRequests.head
    val newReserveId = data.reserveId + 1
    request.target ! BookReply(Right(newReserveId))

    Behaviors.receiveMessage[Event] { case Done =>
      waitForRequests(
        data.copy(
          reserveId = newReserveId,
          nrBooksInStore = data.nrBooksInStore - 1,
          pendingRequests = data.pendingRequests.tail
        )
      )
    }
  }

  def soldOut(data: StateData): Behavior[Event] = Behaviors.receiveMessage[Event] { case request: BookRequest =>
    processSoldOut(StateData(data.self, data.publisher, data.reserveId, 0, Seq(request)))
  }

  def processSoldOut(data: StateData): Behavior[Event] = {
    data.pendingRequests.foreach { request =>
      request.target ! BookReply(Left("SoldOut"))
    }

    Behaviors.receiveMessage[Event] { case Done =>
      soldOut(StateData(data.self, data.publisher, data.reserveId, 0, Nil))
    }
  }
}
