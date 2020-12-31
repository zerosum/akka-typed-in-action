package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.wordspec.AnyWordSpecLike

class SilentActor01Test extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A Silent Actor" must {

    "change state when it receives a message, single threaded" in {
      // テストを書くと最初は失敗する
      fail("not implemented yet")
    }
    "change state when it receives a message, multi-threaded" in {
      // テストを書くと最初は失敗する
      fail("not implemented yet")
    }
  }
}

object SilentActor {
  def apply(): Behavior[Nothing] = Behaviors.empty
}
