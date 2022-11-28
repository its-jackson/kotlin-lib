package scripts.kotlin.api

import org.tribot.script.sdk.*
import org.tribot.script.sdk.antiban.Antiban
import org.tribot.script.sdk.frameworks.behaviortree.*
import org.tribot.script.sdk.interfaces.Positionable
import org.tribot.script.sdk.query.Query
import org.tribot.script.sdk.walking.GlobalWalking
import org.tribot.script.sdk.walking.LocalWalking
import org.tribot.script.sdk.walking.WalkState
import scripts.kotlin.api.antiban.PolyAntiban

/**
Composite nodes: sequence and selector, the sequence node behaves as an AND gate.
The selector node behaves as an OR gate.

Decorative nodes: inverter, repeatUntil, succeeder, condition.
inverter: invert the result of the child. A child fails, and it will return success to its parent,
or a child succeeds, and it will return failure to the parent.

condition: ensures that we can skip steps we don't need to do. So if the condition is satisfied,
great! We move on. If not, we do something to satisfy it.

Leaf nodes: perform, terminal node that will always return success.
 */

/**
 * This behavior tree ensures the user is logged in first.
 * Then it will ensure the inventory is empty, before entering the main script logic.
 */
fun initializeScriptBehaviorTree() = behaviorTree {
    repeatUntil(BehaviorTreeStatus.KILL) {
        sequence {
            selector {
                inverter { condition { !Login.isLoggedIn() } }
                condition { Login.login() }
            }
            selector {
                inverter { condition { !Inventory.isEmpty() } }
                walkToAndDepositInvBank()
            }
            performKill { Camera.setZoomPercent(0.00) }
        }
    }
}

/**
 * This behavior tree is the main logic tree for the script.
 * It decides the behavior of the character based on the active script task.
 * The task session will end if the character fails too many times consecutively each tick.
 */
fun scriptLogicBehaviorTree(
    abstractBehavior: IParentNode.() -> Unit = { scriptAbstractBehavior() },
    specificBehavior: IParentNode.() -> Unit
) = behaviorTree {
    sequence {
        scriptControl { abstractBehavior() }
        scriptControl { specificBehavior() }
    }
}

/**
 * Carryout the high level character abstraction behaviors.
 *
 * Logging in, turning on character run, eating food.
 */
fun IParentNode.scriptAbstractBehavior() = sequence {
    loginAction()
    enableRunAction()
    eatingAction()
}

fun IParentNode.loginAction() = selector {
    condition { Login.isLoggedIn() }
    condition { Login.login() }
}

fun IParentNode.enableRunAction() = selector {
    condition { !Antiban.shouldTurnOnRun() || Options.isRunEnabled() }
    perform { Options.setRunEnabled(true) }
}

fun IParentNode.eatingAction() = selector {
    condition { MyPlayer.getCurrentHealthPercent() > PolyAntiban.currentEatPercent }
    condition { !getEatableInventoryQuery().isAny }
    perform {
        getEatableInventoryQuery().findClosestToMouse()
            .ifPresent { it.click() }
    }
    perform { PolyAntiban.generateEatPercent() }
}

fun getEatableInventoryQuery() = Query.inventory()
    .actionContains("Eat")
    .isNotNoted

fun canReach(p: Positionable) = LocalWalking.createMap()
    .canReach(p)

fun walkTo(entity: Positionable) = GlobalWalking.walkTo(entity) {
    if (Antiban.shouldTurnOnRun() && !Options.isRunEnabled())
        Options.setRunEnabled(true)
    WalkState.CONTINUE
}

fun lootItems(dist: Double = 3.0) = getLootableItemsQuery(dist)
    .toList()
    .fold(0) { runningSum, item ->
        if (Inventory.isFull())
            return runningSum

        val before = Inventory.getCount(item.id)

        if (!item.interact("Take"))
            return runningSum

        if (!Waiting.waitUntil(2500) { Inventory.getCount(item.id) > before })
            return runningSum

        val result = runningSum + item.stack
        result
    }

fun isLootableItemsFound(dist: Double = 3.0) = getLootableItemsQuery(dist).isAny

private fun getLootableItemsQuery(
    dist: Double = 3.0
) = Query.groundItems()
    .maxDistance(dist)
    .isReachable
