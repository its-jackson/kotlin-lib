package scripts.kotlin.api.random.event

import org.tribot.script.sdk.ChatScreen
import org.tribot.script.sdk.Inventory
import org.tribot.script.sdk.Skill
import org.tribot.script.sdk.Waiting
import org.tribot.script.sdk.frameworks.behaviortree.*
import org.tribot.script.sdk.query.Query

abstract class AbstractRandomEvent(
    val eventName: String,
    val npcId: Int,
) {
    abstract fun shouldSolve(): Boolean
    abstract fun getSolution(): IBehaviorNode

    fun executeSolution(): BehaviorTreeStatus = getSolution().tick()

    override fun toString(): String = "AbstractRandomEvent(eventName='$eventName', npcId=$npcId)"
}

class GenieRandomEvent(
    private val lamp: Skill = Skill.AGILITY
) : AbstractRandomEvent(
    eventName = "Genie Random Event",
    npcId = 1111
) {
    override fun shouldSolve() = getGenieNpcQuery()
        .isAny

    override fun getSolution() = behaviorTree {
        repeatUntil({ !getGenieNpcQuery().isAny && !Inventory.contains("Lamp") }) {
            selector {
                talkToGenie()
                rubLamp()
            }
        }
    }

    private fun getGenieNpcQuery() = Query.npcs()
        .idEquals(this.npcId)
        .isVisible
        .isFacingMe
        .isInteractingWithMe

    private fun IParentNode.talkToGenie() = sequence {
        condition { getGenieNpcQuery().isAny }
        selector {
            condition { ChatScreen.isOpen() }
            condition {
                getGenieNpcQuery().findFirst()
                    .map {
                        it.interact("Talk-to") &&
                                Waiting.waitUntil { ChatScreen.isOpen() }
                    }
                    .orElse(false)
            }
        }
        perform {
            ChatScreen.clickContinue()
        }
    }

    private fun IParentNode.rubLamp() = selector {
        condition { !Inventory.contains("Lamp") }
        sequence {
            // TODO rub lamp, click which skill, wait until experience gained.
        }
    }

    override fun toString(): String = "GenieRandomEvent(lampSkill=$lamp)"
}

class RandomEventHandler(
    private val genie: GenieRandomEvent? = GenieRandomEvent()
) {
    fun handle(): BehaviorTreeStatus? =
        if (genie?.shouldSolve() == true)
            genie.executeSolution()
        else
            null
}

fun main() {
    // your script...
    val scriptTree = behaviorTree {
        repeatUntil(BehaviorTreeStatus.KILL) {
            randomEventControl(genieEvent = GenieRandomEvent(lamp = Skill.MINING)) {
                sequence {
                    // your logic...
                }
            }
        }
    }

    // run script...
    val scriptResult = scriptTree.tick()
}

fun IParentNode.randomEventControl(
    genieEvent: GenieRandomEvent?,
    init: IParentNode.() -> Unit
) = initNode("Random Event Control", RandomEventControlNode(genieEvent), init)

class RandomEventControlNode(
    genieEvent: GenieRandomEvent? = GenieRandomEvent()
) : Decorator() {
    private val eventHandler = RandomEventHandler(genie = genieEvent)

    override var name: String = "Random Event Control"

    override fun tick(): BehaviorTreeStatus {
        if (child == null) throw IllegalStateException("RandomEventControlNode must have a child node")

        val eventResult = eventHandler.handle()

        if (eventResult != null) return eventResult

        return child!!.tick()
    }
}