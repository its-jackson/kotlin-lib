package scripts.kotlin.api

import org.tribot.script.sdk.frameworks.behaviortree.BehaviorTreeStatus
import org.tribot.script.sdk.frameworks.behaviortree.Decorator
import org.tribot.script.sdk.frameworks.behaviortree.IBehaviorNode
import org.tribot.script.sdk.frameworks.behaviortree.IParentNode

fun IParentNode.performKill(name: String = "", func: () -> Unit) {
    val node = object : IBehaviorNode {
        override var name: String = ""

        override fun tick(): BehaviorTreeStatus {
            func()
            return BehaviorTreeStatus.KILL
        }
    }

    this.initNode("[Perform Cease] $name", node) {}
}

/**
 * @author High Order Scripts
 */
fun IParentNode.scriptControl(
    maxConsecutiveFailures: Int = 10,
    endConditions: List<() -> Boolean> = emptyList(),
    endScript: () -> Unit = { },
    init: IParentNode.() -> Unit
) = initNode("Script Control", ScriptControlNode(maxConsecutiveFailures, endConditions, endScript), init)

/**
 * @author High Order Scripts
 */
class ScriptControlNode(
    private val maxConsecutiveFailures: Int = 10,
    private val endConditions: List<() -> Boolean> = emptyList(),
    val endScript: () -> Unit,
) : Decorator() {
    override var name: String = "Script Control"
    var consecutiveFailures = 0

    override fun tick(): BehaviorTreeStatus {
        if (child == null) throw IllegalStateException("ScriptControlNode must have a child node")

        if (consecutiveFailures >= maxConsecutiveFailures || endConditions.any { it() }) {
            endScript()
            return BehaviorTreeStatus.KILL
        }

        val result = child!!.tick()

        when (result) {
            BehaviorTreeStatus.KILL -> {
                endScript()
            }

            BehaviorTreeStatus.FAILURE -> {
                consecutiveFailures++
            }

            BehaviorTreeStatus.SUCCESS -> {
                consecutiveFailures = 0
            }
        }

        return result
    }
}