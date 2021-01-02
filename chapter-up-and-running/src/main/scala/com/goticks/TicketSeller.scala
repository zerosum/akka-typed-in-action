package com.goticks

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object TicketSeller {

  sealed trait Command
  final case class Add(tickets: Vector[Ticket])                         extends Command
  final case class Buy(tickets: Int, replyTo: ActorRef[Response])       extends Command
  final case class GetEvent(replyTo: ActorRef[Option[BoxOffice.Event]]) extends Command
  final case class Cancel(replyTo: ActorRef[Option[BoxOffice.Event]])   extends Command

  sealed trait Response
  final case class Tickets(event: String, entries: Vector[Ticket] = Vector.empty[Ticket]) extends Response

  final case class Ticket(id: Int)

  def apply(name: String): Behavior[Command] = updateTicketSeller(name)

  private def updateTicketSeller(event: String, tickets: Vector[Ticket] = Vector.empty[Ticket]): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Add(newTickets) =>
          updateTicketSeller(event, tickets ++ newTickets)

        case Buy(nrOfTickets, sender) =>
          val entries = tickets.take(nrOfTickets)
          if (entries.size >= nrOfTickets) {
            sender ! Tickets(event, entries)
            updateTicketSeller(event, tickets.drop(nrOfTickets))
          } else {
            sender ! Tickets(event)
            Behaviors.same
          }

        case GetEvent(sender) =>
          sender ! Some(BoxOffice.Event(event, tickets.size))
          Behaviors.same

        case Cancel(sender) =>
          sender ! Some(BoxOffice.Event(event, tickets.size))
          Behaviors.stopped
      }
    }
  }
}
