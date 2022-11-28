package scripts.kotlin.api

import org.tribot.script.sdk.MakeScreen
import org.tribot.script.sdk.Waiting

fun makeAllAvailableItems() =
    Waiting.waitUntil { MakeScreen.makeAll { it.isVisible && it.actions.isNotEmpty() } } &&
            Waiting.waitUntil { !MakeScreen.isOpen() } && Waiting.waitUntilAnimating(10000) &&
            waitUntilNotAnimating(end = 2000)