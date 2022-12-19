package scripts.kotlin.api

import com.allatori.annotations.DoNotRename
import org.tribot.script.sdk.*
import org.tribot.script.sdk.antiban.PlayerPreferences
import org.tribot.script.sdk.frameworks.behaviortree.*
import org.tribot.script.sdk.query.Query
import org.tribot.script.sdk.util.TribotRandom
import java.time.Instant

/**
 * @author Polymorphic
 */
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
    private var consecutiveFailures = 0

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

/**
 * @author Polymorphic
 */
data class ScriptBreakControlData(
    @DoNotRename
    val frequencyMeanMinutes: Double = PlayerPreferences.preference(
        "scripts.kotlin.api.ScriptControlNodes.scriptBreakControl.frequencyMeanMinutes"
    ) { g: PlayerPreferences.Generator ->
        g.uniform(120.0, 160.0)
    },
    @DoNotRename
    val frequencyStdMinutes: Double = PlayerPreferences.preference(
        "scripts.kotlin.api.ScriptControlNodes.scriptBreakControl.frequencyStdMinutes"
    ) { g: PlayerPreferences.Generator ->
        g.uniform(2.0, 12.0)
    },
    @DoNotRename
    val timeMeanMinutes: Double = PlayerPreferences.preference(
        "scripts.kotlin.api.ScriptControlNodes.scriptBreakControl.timeMeanMinutes"
    ) { g: PlayerPreferences.Generator ->
        g.uniform(20.0, 180.0)
    },
    @DoNotRename
    val timeStdMinutes: Double = PlayerPreferences.preference(
        "scripts.kotlin.api.ScriptControlNodes.scriptBreakControl.timeMeanStd"
    ) { g: PlayerPreferences.Generator ->
        g.uniform(3.0, 5.5)
    }
) {
    @DoNotRename
    private val conversion = 60

    val frequencyMeanSeconds: Double
        get() {
            return frequencyMeanMinutes * conversion
        }

    val frequencyStdSeconds: Double
        get() {
            return frequencyStdMinutes * conversion
        }

    val timeMeanSeconds: Double
        get() {
            return timeMeanMinutes * conversion
        }

    val timeStdSeconds: Double
        get() {
            return timeStdMinutes * conversion
        }
}

/**
 * @author Polymorphic
 */
fun IParentNode.scriptBreakControl(
    breakControlData: ScriptBreakControlData? = null,
    walkToBank: Boolean = false,
    init: IParentNode .() -> Unit
) = initNode(
    "Script Break Control",
    ScriptBreakControlNode(breakControlData, walkToBank),
    init
)

/**
 * @author Polymorphic
 */
class ScriptBreakControlNode(
    private val breakControlData: ScriptBreakControlData?,
    private val walkToBank: Boolean = false,
    override var name: String = "Script Break Control"
) : Decorator() {
    private var manageTimer: Instant? = null
    private var currentTimeSeconds: Double = 0.0
    private var currentFrequencySeconds: Double = 0.0

    private var startTime: Long = 0

    init {
        generateFrequencySeconds()
        generateTimeSeconds()
    }

    private fun getRemainder() = (currentTimeSeconds - (Instant.now().epochSecond - startTime)).toLong()

    override fun tick(): BehaviorTreeStatus {
        if (child == null) throw IllegalStateException("ScriptBreakControlNode must have a child node")
        if (breakControlData != null && shouldBreak()) {
            if (walkToBank) {
                val walkResult = walkToAndDepositInvBank().tick()
                if (walkResult != BehaviorTreeStatus.SUCCESS) return walkResult
            }
            Log.debug(
                "[ScriptBreakControl]" + " " +
                        "Taking break for: " +
                        currentTimeSeconds.toLong() + " " + "seconds"
            )
            val result = takeBreak()
            Log.debug(
                "[ScriptBreakControl]" + " "
                        + "Break has ended: " + getRemainder() + " "
                        + "remaining seconds"
            )
            update()
            return result
        }
        return child!!.tick()
    }

    private fun shouldBreak(): Boolean {
        if (manageTimer == null) generateManageTimer()
        return manageTimer?.let { currentFrequencySeconds < (Instant.now().epochSecond - it.epochSecond) } == true
    }

    private fun takeBreak(): BehaviorTreeStatus {
        startTime = Instant.now().epochSecond

        val result = behaviorTree {
            repeatUntil({ this@ScriptBreakControlNode.getRemainder() < 1 }) {
                perform { Waiting.waitNormal(750, 50) }
            }
        }.tick()

        return result
    }

    private fun update() {
        generateTimeSeconds()
        generateFrequencySeconds()
        generateManageTimer()
    }

    private fun getTimeSeconds() = TribotRandom.normal(
        breakControlData?.timeMeanSeconds ?: 0.0,
        breakControlData?.timeStdSeconds ?: 1.0
    )

    private fun getFrequencySeconds() = TribotRandom.normal(
        breakControlData?.frequencyMeanSeconds ?: 0.0,
        breakControlData?.frequencyStdSeconds ?: 1.0
    )

    private fun generateTimeSeconds() {
        currentTimeSeconds = getTimeSeconds()
    }

    private fun generateFrequencySeconds() {
        currentFrequencySeconds = getFrequencySeconds()
    }

    private fun generateManageTimer() {
        manageTimer = Instant.now()
    }
}

fun IParentNode.scriptDeathControl(init: IParentNode.() -> Unit) = initNode(
    "Script Death Control",
    ScriptDeathControlNode(),
    init
)

class ScriptDeathControlNode : Decorator() {
    override var name: String = "Script Death Control"

    private val deathNpcId = 9855
    private val deathPortalGameObjectId = 39549

    // DO NOT CHANGE THE ORDER
    private val selectOptions = arrayOf(
        "Tell me about gravestones again.",
        "How do I pay a gravestone fee?",
        "How long do I have to return to my gravestone?",
        "How do I know what will happen to my items when I die?",
        "I think I'm done here."
    )

    private val selectOptionRootWidgetAddress = WidgetAddress.create(219) {
        it.text.map { t ->
            t.contains("Select an Option")
        }.orElse(false)
    }

    private val getChatScreenConfig = ChatScreen.Config.builder()
        .holdSpaceForContinue(true)
        .build()

    private fun handleNewCharacterDeathOptions(): Boolean {
        ChatScreen.setConfig(getChatScreenConfig)
        return ChatScreen.handle(*selectOptions)
    }

    private fun getDeathNpcQuery() = Query.npcs()
        .idEquals(deathNpcId)
        .isReachable

    private fun getDeathPortalGameObjectQuery() = Query.gameObjects()
        .idEquals(deathPortalGameObjectId)
        .isReachable

    private fun getGraveGameObject() = Query.gameObjects()
        .nameEquals("Grave")
        .actionContains("Check", "Loot")

    override fun tick(): BehaviorTreeStatus {
        if (child == null) throw IllegalStateException("ScriptDeathControlNode must have a child node")

        return if (MyPlayer.isDiseased() || MyPlayer.getCurrentHealth() < 1 || getDeathNpcQuery().isAny) {
            val grave = MyPlayer.getTile()

            val waitForDeathResult = Waiting.waitUntil(10000) {
                this@ScriptDeathControlNode.getDeathNpcQuery().isAny
            }

            if (!waitForDeathResult) return BehaviorTreeStatus.FAILURE

            val handleDeathResult = behaviorTree {
                repeatUntil({
                    !this@ScriptDeathControlNode.getDeathNpcQuery().isAny
                }) {
                    sequence {
                        selector {
                            inverter { condition { ChatScreen.isOpen() } }
                            condition { this@ScriptDeathControlNode.handleNewCharacterDeathOptions() }
                        }
                        selector {
                            inverter {
                                condition { this@ScriptDeathControlNode.getDeathPortalGameObjectQuery().isAny }
                            }
                            sequence {
                                condition {
                                    this@ScriptDeathControlNode.getDeathPortalGameObjectQuery()
                                        .findFirst()
                                        .map { it.interact("Use") }
                                        .orElse(false)
                                }
                                condition {
                                    Waiting.waitUntil {
                                        !this@ScriptDeathControlNode.getDeathPortalGameObjectQuery().isAny
                                    }
                                }
                            }
                        }
                    }
                }
            }.tick()

            if (handleDeathResult != BehaviorTreeStatus.SUCCESS) return BehaviorTreeStatus.FAILURE

            val walkToGraveResult = behaviorTree {
                repeatUntil({
                    grave.isVisible && grave.distance() < 2 && canReach(grave)
                }) {
                    condition { walkTo(grave) }
                }
            }.tick()

            if (walkToGraveResult != BehaviorTreeStatus.SUCCESS) return BehaviorTreeStatus.FAILURE

            val lootGraveResult = behaviorTree {
                repeatUntil({
                    !this@ScriptDeathControlNode.getGraveGameObject().isAny
                }) {
                    sequence {
                        condition {
                            this@ScriptDeathControlNode.getGraveGameObject()
                                .findFirst()
                                .map { it.interact("Loot") }
                                .orElse(false)
                        }
                    }
                }
            }.tick()

            if (lootGraveResult != BehaviorTreeStatus.SUCCESS) return BehaviorTreeStatus.FAILURE

            behaviorTree { walkToAndDepositInvBank() }.tick()
        }
        else {
            child!!.tick()
        }
    }
}