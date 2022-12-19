package scripts.kotlin.api

import org.tribot.script.sdk.Login
import org.tribot.script.sdk.MyPlayer
import org.tribot.script.sdk.Waiting
import java.lang.System.currentTimeMillis

/**
 * This method will wait until the character isn't animating.
 * And it will take into account the amount of time the player has briefly stop animating.
 * You can adjust the end time of which to stop if the player hasn't been animating for,
 * by default this is 1000 milliseconds.
 */
fun waitUntilNotAnimating(
    end: Long = 1000,
    step: Int = 10,
    actions: List<() -> Unit> = listOf(),
    interrupt: () -> Boolean = { false }
): Boolean {
    val timeout: Long = (currentTimeMillis() +  60000) * 3
    var runningTime: Long = currentTimeMillis()
    var currentTime: Long = currentTimeMillis()

    while (currentTime - runningTime <= end) {
        if (interrupt() || !Login.isLoggedIn() || timeout < currentTimeMillis()) break
        actions.forEach { it.invoke() }
        if (MyPlayer.isAnimating()) runningTime = currentTimeMillis()
        currentTime = currentTimeMillis()
        Waiting.wait(step)
    }

    return end <= currentTime - runningTime
}

fun waitAvgHumanReactionTime() = Waiting.waitNormal(250, 15)