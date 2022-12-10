package scripts.kotlin.api

import org.tribot.script.sdk.Bank
import org.tribot.script.sdk.Inventory
import org.tribot.script.sdk.Skill
import org.tribot.script.sdk.Waiting
import org.tribot.script.sdk.frameworks.behaviortree.IParentNode
import org.tribot.script.sdk.frameworks.behaviortree.condition
import org.tribot.script.sdk.frameworks.behaviortree.selector
import org.tribot.script.sdk.frameworks.behaviortree.sequence
import java.util.concurrent.TimeUnit

interface ISatisfiable {
    fun isSatisfied(): Boolean
}

abstract class AbstractStopCondition : ISatisfiable {
    enum class ConditionType(val con: String) {
        TIME("Time condition"),
        RESOURCE_GAINED("Resource gained condition"),
        SKILL_LEVELS_REACHED("Skill levels reached condition");
    }
}

class TimeStopCondition(
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0
) : AbstractStopCondition() {
    private var startTime: Long = -1

    private val toMillis = TimeUnit.DAYS.toMillis(days)
        .plus(TimeUnit.HOURS.toMillis(hours))
        .plus(TimeUnit.MINUTES.toMillis(minutes))
        .plus(TimeUnit.SECONDS.toMillis(seconds))

    // true if the time has surpassed
    override fun isSatisfied(): Boolean {
        if (startTime == -1L) startTime = System.currentTimeMillis()
        return System.currentTimeMillis() - startTime >= toMillis
    }

    override fun toString(): String {
        if (startTime == -1L) return "(days:$days:hours:$hours:minutes:$minutes:seconds:$seconds)"
        val remain = toMillis - (System.currentTimeMillis() - startTime)
        val hoursRemaining = remain / 1000 / 60 / 60
        val minutesRemaining = remain / 1000 / 60 % 60
        val secondsRemaining = remain / 1000 % 60
        return "Time (hours:$hoursRemaining:minutes:$minutesRemaining:seconds:$secondsRemaining)"
    }
}

class ResourceGainedCondition(
    val id: Int,
    val amount: Int = -1 // infinity
) : AbstractStopCondition() {
    val remainder: Int
        get() = amount - sum

    private var sum = 0

    fun updateSumDirectly(amt: Int) {
        if (amount < 1 || amt < 1) return
        sum = sum.plus(amt)
    }

    internal fun withdrawResourceFromBank() =
        if (remainder in 1..28)
            Bank.withdraw(
                id,
                remainder
            ) && Waiting.waitUntil { Inventory.contains(id) }
        else
            Bank.withdrawAll(id) && Waiting.waitUntil { Inventory.contains(id) }

    internal fun bankHasResource() = Bank.contains(id)

    internal fun inventoryHasResource() = Inventory.contains(id)

    override fun isSatisfied() = amount > 0 && (remainder < 1 && sum >= amount)

    override fun toString() = "Resource gained (id=$id, amount=$amount, remainder=$remainder, sum=$sum)"
}

class SkillLevelsReachedCondition(
    val skills: Map<Skill, Int>, // multiple skills, multiple skill level goals
) : AbstractStopCondition() {
    override fun isSatisfied() = skills.entries.all {
        val skill = it.key
        val goal = it.value
        skill.actualLevel >= goal
    }

    override fun toString() = "Levels reached " +
            "(${
                skills.entries.fold("")
                { acc, s -> "$acc{${s.key}=${s.value}, actual=${s.key.actualLevel}}" }
            })"
}

fun IParentNode.fetchResourceFromBank(gainedCondition: ResourceGainedCondition?) = selector {
    condition { gainedCondition?.inventoryHasResource() }

    sequence {
        walkToAndDepositInvBank(closeBank = false)
        condition { gainedCondition?.bankHasResource() }
        condition { gainedCondition?.withdrawResourceFromBank() }
        condition { Bank.close() }
    }
}