package aia.testdriven

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Greeter {

  sealed trait Command
  case class Greeting(message: String) extends Command

  def apply(): Behavior[Command] = Behaviors
    .receivePartial { case (ctx, Greeting(message)) =>
      ctx.log.info(s"Hello $message!")
      Behaviors.same
    }
}
