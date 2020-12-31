package aia.testdriven

import aia.testdriven.Greeter.Greeting
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

class Greeter02Test extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "The Greeter" must {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {

      val probe   = testKit.createTestProbe[String]()
      val greeter = testKit.spawn(Greeter02(Some(probe.ref)), "greeter02-1")

      greeter ! Greeting("World")
      probe.expectMessage("Hello World!")
    }
//    "say something else and see what happens" in {
//      val props   = Greeter02.props(Some(testActor))
//      val greeter = system.actorOf(props, "greeter02-2")
//      system.eventStream.subscribe(testActor, classOf[UnhandledMessage])
//      greeter ! "World"
//      expectMsg(UnhandledMessage("World", system.deadLetters, greeter))
//    }
  }
}

object Greeter02 {
  def apply(listener: Option[ActorRef[String]] = None): Behavior[Greeting] = Behaviors.receivePartial {
    case (ctx, Greeting(who)) =>
      val message = "Hello " + who + "!"
      ctx.log.info(message)
      listener.foreach(_ ! message)

      Behaviors.same
  }
}
