package aia.state

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.funspec.AnyFunSpecLike

class InventorySpec extends ScalaTestWithActorTestKit with AnyFunSpecLike {

  describe("Inventory") {
    it("follow the flow") {
      val publisher = spawn(Publisher(2, 2))
      val inventory = spawn(Inventory(publisher))
      val prove     = createTestProbe[Inventory.Response]()

      inventory ! Inventory.BookRequest(prove.ref)
      prove.expectMessage(Inventory.BookReply(Right(1)))
      inventory ! Inventory.BookRequest(prove.ref)
      prove.expectMessage(Inventory.BookReply(Right(2)))
      inventory ! Inventory.BookRequest(prove.ref)
      prove.expectMessage(Inventory.BookReply(Left("SoldOut")))
    }
  }
}
