package scripts.kotlin.api

import org.tribot.script.sdk.MakeScreen
import org.tribot.script.sdk.Waiting
import org.tribot.script.sdk.util.TribotRandom

fun makeAllAvailableItems(names: Array<String> = arrayOf()) = Waiting.waitUntil {
    MakeScreen.makeAll {
        (names.isNotEmpty() && names.any { n ->
            it.definition.name.contains(n, true)
        }) || (it.isVisible && it.actions.isNotEmpty())
    }
} && (
        Waiting.waitUntil { !MakeScreen.isOpen() } &&
                Waiting.waitUntilAnimating(10000) &&
                waitUntilNotAnimating(end = TribotRandom.normal(2000, 124).toLong())
        )
