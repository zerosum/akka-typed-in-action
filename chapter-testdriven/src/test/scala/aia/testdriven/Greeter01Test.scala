package aia.testdriven
import aia.testdriven.Greeter.Greeting
import akka.actor.testkit.typed.scaladsl.{LoggingTestKit, ScalaTestWithActorTestKit}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.wordspec.AnyWordSpecLike

class Greeter01Test extends ScalaTestWithActorTestKit(Greeter01Test.config) with AnyWordSpecLike {

  "The Greeter" must {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {

      val greeter = testKit.spawn(Greeter())

      LoggingTestKit
        .info("Hello World!")
        .withOccurrences(1)
        .expect {
          greeter ! Greeting("World")
        }
    }
  }
}

object Greeter01Test {
  val config: Config = ConfigFactory.parseString("""
         akka.loggers = [akka.testkit.TestEventListener]
      """)
}
