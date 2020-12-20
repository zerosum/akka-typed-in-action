package com.goticks

import akka.actor.typed._
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

object RestApi {

  def apply(ctx: ActorContext[_], timeout: Timeout): RestApi = {
    val boxOffice = ctx.spawn(BoxOffice(), BoxOffice.name)
    new RestApi(ctx.system, boxOffice)(timeout)
  }
}

class RestApi(
    val system: ActorSystem[Nothing],
    val boxOffice: ActorRef[BoxOffice.Command]
)(implicit
    val requestTimeout: Timeout
) extends BoxOfficeApi
    with EventMarshalling {

  override implicit def executionContext: ExecutionContext = system.executionContext
  override implicit def scheduler: Scheduler               = system.scheduler

  def routes: Route = eventsRoute ~ eventRoute ~ ticketsRoute

  def eventsRoute =
    pathPrefix("events") {
      pathEndOrSingleSlash {
        get {
          // GET /events
          onSuccess(getEvents()) { events =>
            complete(OK, events)
          }
        }
      }
    }

  def eventRoute =
    pathPrefix("events" / Segment) { event =>
      pathEndOrSingleSlash {
        post {
          // POST /events/:event
          entity(as[EventDescription]) { ed =>
            onSuccess(createEvent(event, ed.tickets)) {
              case BoxOffice.EventCreated(event) => complete(Created, event)
              case BoxOffice.EventExists =>
                val err = Error(s"$event event exists already.")
                complete(BadRequest, err)
            }
          }
        } ~
          get {
            // GET /events/:event
            onSuccess(getEvent(event)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          } ~
          delete {
            // DELETE /events/:event
            onSuccess(cancelEvent(event)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          }
      }
    }

  def ticketsRoute =
    pathPrefix("events" / Segment / "tickets") { event =>
      post {
        pathEndOrSingleSlash {
          // POST /events/:event/tickets
          entity(as[TicketRequest]) { request =>
            onSuccess(requestTickets(event, request.tickets)) { tickets =>
              if (tickets.entries.isEmpty) complete(NotFound)
              else complete(Created, tickets)
            }
          }
        }
      }
    }

}

trait BoxOfficeApi {
  import BoxOffice._

  val system: ActorSystem[Nothing]
  val boxOffice: ActorRef[BoxOffice.Command]

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout
  implicit def scheduler: Scheduler

  def createEvent(event: String, nrOfTickets: Int): Future[BoxOffice.Response] =
    boxOffice
      .ask(CreateEvent(event, nrOfTickets, _))
      .mapTo[EventCreated]

  def getEvents(): Future[Events] =
    boxOffice.ask(GetEvents).mapTo[Events]

  def getEvent(event: String): Future[Option[Event]] =
    boxOffice
      .ask(GetEvent(event, _))
      .mapTo[Option[Event]]

  def cancelEvent(event: String): Future[Option[Event]] =
    boxOffice
      .ask(CancelEvent(event, _))
      .mapTo[Option[Event]]

  def requestTickets(event: String, tickets: Int) =
    boxOffice
      .ask(GetTickets(event, tickets, _))
      .mapTo[TicketSeller.Tickets]

}
