package actortyped

import actortyped.VendingMachineTypedFunctional._
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.time.{Second, Span}
import org.scalatest.wordspec.AnyWordSpecLike

class VendingMachineTypedFunctionalSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(scaled(Span(1, Second)))

  "report a product not available" in {
    val vendingMachine = spawn(VendingMachineTypedFunctional())
    val requester = createTestProbe[Command]()

    vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
    vendingMachine ! RequestProduct(requester.ref, "sandwich")

    requester.expectMessage(VendingError(PRODUCT_NOT_AVAILABLE))
  }

  "throw a timeout if I don't insert money" in {
    val vendingMachine = spawn(VendingMachineTypedFunctional())
    val requester = createTestProbe[Command]()

    vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
    vendingMachine ! RequestProduct(requester.ref, "coke")

    requester.expectMessage(Instruction("Please insert 1 euros"))

    eventually {
      requester.expectMessage(VendingError(REQUEST_TIMED_OUT))
    }
  }

  "handle the reception of partial money" in {
    val vendingMachine = spawn(VendingMachineTypedFunctional())
    val requester = createTestProbe[Command]()

    vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
    vendingMachine ! RequestProduct(requester.ref, "coke")

    requester.expectMessage(Instruction("Please insert 3 euros"))
    vendingMachine ! ReceiveMoney(1)
    requester.expectMessage(Instruction("Please insert 2 euros"))

    eventually {
      requester.expectMessage(VendingError(REQUEST_TIMED_OUT))
      requester.expectMessage(GiveBackChange(1))
    }
  }

  "deliver the product if I insert all the money" in {
    val vendingMachine = spawn(VendingMachineTypedFunctional())
    val requester = createTestProbe[Command]()

    vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
    vendingMachine ! RequestProduct(requester.ref, "coke")

    requester.expectMessage(Instruction("Please insert 3 euros"))
    vendingMachine ! ReceiveMoney(3)
    requester.expectMessage(Deliver("coke"))
  }

  "give back change and be able to request money for a new product" in {
    val vendingMachine = spawn(VendingMachineTypedFunctional())
    val requester = createTestProbe[Command]()

    vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
    vendingMachine ! RequestProduct(requester.ref, "coke")

    requester.expectMessage(Instruction("Please insert 3 euros"))
    vendingMachine ! ReceiveMoney(4)
    requester.expectMessage(Deliver("coke"))
    requester.expectMessage(GiveBackChange(1))

    vendingMachine ! RequestProduct(requester.ref, "coke")
    requester.expectMessage(Instruction("Please insert 3 euros"))
  }
}
