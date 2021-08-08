package aia.state

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

object Inventory {

  sealed trait Event
  case class BookRequest(replyTo: ActorRef[Response]) extends Event
  case object PendingRequest                          extends Event
  case class BookSupply(nrBooks: Int)                 extends Event
  case object BookSupplySoldOut                       extends Event
  case object Done                                    extends Event

  sealed trait Response
  case class BookReply(reserveId: Either[String, Int]) extends Response

  case class StateData(
      publisher: ActorRef[Publisher.Event],
      reserveId: Int,
      nrBooksInStore: Int,
      pendingRequests: Seq[BookRequest]
  )

  def apply(publisher: ActorRef[Publisher.Event]): Behavior[Event] = {
    waitForRequests(StateData(publisher, 0, 0, Nil))
  }

  private[this] type StateTransition = PartialFunction[Event, Behavior[Event]]

  private def waitForRequests(state: StateData): Behavior[Event] = Behaviors.setup { ctx =>
    // entry action
    if (state.pendingRequests.nonEmpty) {
      ctx.self ! PendingRequest
    }

    // transition conditions
    Behaviors.receiveMessage[Event] { message =>
      def transitionByCondOfState(_state: StateData): Behavior[Event] = {
        if (_state.nrBooksInStore > 0) {
          processRequest(_state)
        } else {
          waitForPublisher(_state)
        }
      }

      val transition: StateTransition = {
        case request: BookRequest =>
          val newStateData = state.copy(pendingRequests = state.pendingRequests :+ request)
          transitionByCondOfState(newStateData)
        case PendingRequest if state.pendingRequests.nonEmpty =>
          transitionByCondOfState(state)
        case PendingRequest =>
          Behaviors.same
      }

      transition
        .orElse(unhandledEventPF("waitForRequests", ctx, state))
        .apply(message)
    }
  }

  private def waitForPublisher(state: StateData): Behavior[Event] = Behaviors.setup { ctx =>
    // entry action
    state.publisher ! Publisher.Request(ctx.self)

    // transition conditions
    Behaviors.receiveMessage[Event] { message =>
      val transition: StateTransition = {
        case supply: BookSupply => processRequest(state.copy(nrBooksInStore = supply.nrBooks))
        case BookSupplySoldOut  => processSoldOut(state)
      }

      transition
        .orElse(unhandledBookRequestPF(waitForPublisher, state))
        .orElse(unhandledEventPF("waitForPublisher", ctx, state))
        .apply(message)
    }
  }

  private def processRequest(state: StateData): Behavior[Event] = Behaviors.setup { ctx =>
    // entry action
    val request      = state.pendingRequests.head
    val newReserveId = state.reserveId + 1
    request.replyTo ! BookReply(Right(newReserveId))
    ctx.self ! Done

    // transition conditions
    Behaviors.receiveMessage[Event] { message =>
      val transition: StateTransition = { case Done =>
        waitForRequests(
          state.copy(
            reserveId = newReserveId,
            nrBooksInStore = state.nrBooksInStore - 1,
            pendingRequests = state.pendingRequests.tail
          )
        )
      }

      transition
        .orElse(unhandledBookRequestPF(processRequest, state))
        .orElse(unhandledEventPF("processRequest", ctx, state))
        .apply(message)
    }
  }

  private def soldOut(state: StateData): Behavior[Event] = {
    // transition conditions
    Behaviors.receive[Event] { (ctx, message) =>
      val transition: StateTransition = { case request: BookRequest =>
        processSoldOut(StateData(state.publisher, state.reserveId, 0, Seq(request)))
      }

      transition
        .orElse(unhandledEventPF("soldOut", ctx, state))
        .apply(message)
    }
  }

  private def processSoldOut(state: StateData): Behavior[Event] = Behaviors.setup { ctx =>
    // entry action
    state.pendingRequests.foreach { _.replyTo ! BookReply(Left("SoldOut")) }
    ctx.self ! Done

    // transition conditions
    Behaviors.receiveMessage[Event] { message =>
      val transition: StateTransition = { case Done =>
        soldOut(StateData(state.publisher, state.reserveId, 0, Nil))
      }

      transition
        .orElse(unhandledBookRequestPF(processSoldOut, state))
        .orElse(unhandledEventPF("processSoldOut", ctx, state))
        .apply(message)
    }
  }

  // unhandled fallback
  private[this] def unhandledEventPF(stateName: String, ctx: ActorContext[Event], state: StateData): StateTransition = {
    case event: Event =>
      ctx.log.warn("received unhandled request {} in state {}/{}", event, stateName, state)
      Behaviors.same
  }

  private[this] def unhandledBookRequestPF(f: StateData => Behavior[Event], state: StateData): StateTransition = {
    case request: BookRequest =>
      val newStateData = state.copy(pendingRequests = state.pendingRequests :+ request)
      f(newStateData)
  }
}
