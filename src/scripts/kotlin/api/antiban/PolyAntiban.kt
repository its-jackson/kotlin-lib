package scripts.kotlin.api.antiban

import org.tribot.script.sdk.antiban.Antiban
import org.tribot.script.sdk.antiban.PlayerPreferences
import org.tribot.script.sdk.util.TribotRandom
import java.util.*

object PolyAntiban : Antiban() {
    private val eatPercentMean = PlayerPreferences.preference(
        "scripts.antiban.AntibanExtension.ExtensionInstance.eatPercentMean"
    ) { g: PlayerPreferences.Generator ->
        g.uniform(
            50.0,
            60.0
        )
    }
    private val eatPercentStd = PlayerPreferences.preference(
        "scripts.antiban.AntibanExtension.ExtensionInstance.eatPercentStd"
    ) { g: PlayerPreferences.Generator ->
        g.uniform(
            1.0,
            3.0
        )
    }

    private val drinkStaminaMean = PlayerPreferences.preference(
        "scripts.antiban.AntibanExtension.ExtensionInstance.drinkStaminaMean"
    ) { g: PlayerPreferences.Generator ->
        g.uniform(
            25.0,
            45.0
        )
    }
    private val drinkStaminaStd = PlayerPreferences.preference(
        "scripts.antiban.AntibanExtension.ExtensionInstance.drinkStaminaStd"
    ) { g: PlayerPreferences.Generator ->
        g.uniform(
            1.3,
            3.3
        )
    }

    var currentEatPercent = 0.0
    var currentDrinkStaminaPercent = 0.0

    init {
        generateEatPercent()
        generateDrinkStaminaPercent()
    }

    /**
     * possible outcomes
     *
     * 50 mean 3 std
     * - empirical rule: 41 low - 59 high
     *
     * 50 mean 1 std
     * - empirical rule: 47 low - 53 high
     *
     * 60 mean 3 std
     * - empirical rule: 51 low - 69 high
     *
     * 60 mean 1 std
     * - empirical rule: 57 low - 63 high
     *
     * @return The next eating percentage.
     */
    private fun getEatAtPercent(): Double {
        return TribotRandom.normal(eatPercentMean, eatPercentStd)
    }

    /**
     * @author Nullable
     * Call this and cache it somewhere and use that cached value to determine if you should eat
     * Once you eat, set the cache value to another invocation of this function.
     */
    fun generateEatPercent() {
        currentEatPercent = getEatAtPercent()
    }

    private fun getDrinkStaminaAtPercent(): Double {
        return TribotRandom.normal(drinkStaminaMean, drinkStaminaStd)
    }

    fun generateDrinkStaminaPercent() {
        currentDrinkStaminaPercent = getDrinkStaminaAtPercent()
    }

    /**
     * Four categories of player types:
     * 1) Never performs X
     * 2) Always performs X
     * 3) Sometimes performs X
     * 4) Usually performs X
     */
    fun qualitativeProbability(key: String): Boolean {
        var changeChance = 0.0
        val changeUserType = PlayerPreferences.preference(
            "$key.changeUserType"
        ) { generator: PlayerPreferences.Generator ->
            generator.uniform(
                0.0,
                1.0
            )
        }
        changeChance = if (changeUserType <= 0.12) {
            // 12% of users will not change teams
            0.0
        } else if (changeUserType <= 0.30) {
            // 18% of users will always change teams
            1.0
        } else if (changeUserType <= 0.65) {
            // 35% of players will sometimes change teams (around 35% chance)
            PlayerPreferences.preference(
                "$key.changeUserTypeRare"
            ) { generator: PlayerPreferences.Generator ->
                generator.normal(
                    0.005,
                    0.955,
                    0.35,
                    0.10
                )
            }
        } else {
            // 35% of players will usually change teams (around 65% chance)
            PlayerPreferences.preference(
                "$key.changeUserTypeRare"
            ) { generator: PlayerPreferences.Generator ->
                generator.normal(
                    0.005,
                    0.955,
                    0.65,
                    0.12
                )
            }
        }
        return Random().nextDouble() <= changeChance
    }
}

