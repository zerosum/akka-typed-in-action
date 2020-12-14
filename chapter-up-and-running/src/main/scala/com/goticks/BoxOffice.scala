package com.goticks

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object BoxOffice {
  def name = "boxOffice"

  case class Event(name: String, tickets: Int)
  case class Events(events: Vector[Event])

  sealed trait Command
  final case class CreateEvent(name: String, tickets: Int, replyTo: ActorRef[Response])              extends Command
  final case class GetTickets(event: String, tickets: Int, replyTo: ActorRef[TicketSeller.Response]) extends Command
  final case class GetEvent(name: String, replyTo: ActorRef[Option[Event]])                          extends Command
  final case class GetEvents(replyTo: ActorRef[Events])                                              extends Command
  final case class CancelEvent(name: String, replyTo: ActorRef[Option[Event]])                       extends Command

  sealed trait Response
  final case class EventCreated(event: Event) extends Response
  final case object EventExists               extends Response

  def apply(): Behavior[Command] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case CreateEvent(name, tickets, sender) =>
        ctx.child(name) match {
          case Some(_) =>
            sender ! EventExists
          case _ =>
            val eventTickets = createTicketSeller(name, ctx)
            val newTickets   = (1 to tickets).map(id => TicketSeller.Ticket(id)).toVector
            eventTickets ! TicketSeller.Add(newTickets)
            sender ! EventCreated(Event(name, tickets))
        }

        Behaviors.same

      case GetTickets(event, tickets, sender) =>
        ctx.child(event) match {
          case Some(ticketSeller: ActorRef[TicketSeller.Command]) =>
            ticketSeller ! TicketSeller.Buy(tickets, sender)
          case _ =>
            sender ! TicketSeller.Tickets(event)
        }
        Behaviors.same

      case GetEvent(event, sender) =>
        ctx.child(event) match {
          case Some(ticketSeller: ActorRef[TicketSeller.Command]) =>
            ticketSeller ! TicketSeller.GetEvent(sender)
          case _ =>
            sender ! None
        }
        Behaviors.same

      case GetEvents(sender) =>
        import akka.actor.typed.scaladsl.AskPattern.Askable
        import akka.util.Timeout

        implicit val ec: ExecutionContext = ctx.system.executionContext
        implicit val scheduler: Scheduler = ctx.system.scheduler
        implicit val timeout: Timeout     = 3.seconds

        def getEvents: Iterable[Future[Option[Event]]] = ctx.children.map { child =>
          ctx.self
            .ask { replyTo: ActorRef[Option[Event]] => GetEvent(child.path.name, replyTo) }
            .mapTo[Option[Event]]
        }

        def convertToEvents(f: Future[Iterable[Option[Event]]]): Future[Events] = {
          f.map(_.flatten).map(l => Events(l.toVector))
        }

        convertToEvents(Future.sequence(getEvents)).onComplete {
          case Success(events) => sender ! events
          case Failure(ex)     => // brah brah brah
        }
        Behaviors.same

      case CancelEvent(event, sender) =>
        ctx.child(event) match {
          case Some(ticketSeller: ActorRef[TicketSeller.Command]) =>
            ticketSeller ! TicketSeller.Cancel(sender)
          case _ =>
            sender ! None
        }
        Behaviors.same
    }
  }

  private[this] def createTicketSeller(name: String, ctx: ActorContext[Command]): ActorRef[TicketSeller.Command] = {
    ctx.spawn(TicketSeller(name), name)
  }

}
