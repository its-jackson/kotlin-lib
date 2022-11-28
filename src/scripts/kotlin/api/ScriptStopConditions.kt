package scripts.kotlin.api

import org.tribot.script.sdk.Skill
import java.util.concurrent.TimeUnit

interface Satisfiable {
    fun isSatisfied(): Boolean
}

abstract class StopCondition : Satisfiable {
    enum class ConditionType(val con: String) {
        TIME("Time condition"),
        RESOURCE_GAINED("Resource gained condition"),
        SKILL_LEVELS_REACHED("Skill levels reached condition");
    }
}

class TimeStopCondition(
    days: Long = 0,
    hours: Long = 0,
    minutes: Long = 0,
    seconds: Long = 0
) : StopCondition() {
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
        if (startTime == -1L) return "(hours:00:minutes:00:seconds:00)"
        val remain = toMillis - (System.currentTimeMillis() - startTime)
        val hours = remain / 1000 / 60 / 60
        val minutes = remain / 1000 / 60 % 60
        val seconds = remain / 1000 % 60
        return "Time (hours:$hours:minutes:$minutes:seconds:$seconds)"
    }
}

class ResourceGainedCondition(
    val id: Int,
    val amount: Int = -1 // default value is infinity
) : StopCondition() {
    val remainder: Int
        get() = amount - sum

    private var sum = 0

    /**
     * It is expected that anyone who creates an instance must
     * invoke this function manually to update the remainder and sum.
     * Or else the resource amount won't ever satisfy.
     */
    fun updateSum(amt: Int) {
        if (amount < 1 || amt < 1) return
        sum = sum.plus(amt)
    }

    override fun isSatisfied(): Boolean =
        amount > 0 && (remainder < 1 && sum >= amount)

    override fun toString() = "Resource gained (id=$id, amount=$amount, remainder=$remainder, sum=$sum)"
}

class SkillLevelsReachedCondition(
    private val skills: Map<Skill, Int>, // multiple skills, multiple skill level goals
) : StopCondition() {
    override fun isSatisfied() = skills.entries.all {
        val skill = it.key
        val goal = it.value
        skill.actualLevel >= goal
    }

    override fun toString() = "Levels reached " +
            "(${
                skills.entries.fold("")
                { acc, s -> "$acc${s.key}=${s.value}, actual=${s.key.actualLevel}" }
            })"
}