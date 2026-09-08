package io.hamlook.aetheria.utils.compat

import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.Scoreboard

object ScoreboardCompat {

    @JvmStatic
    fun getSidebarObjective(scoreboard: Scoreboard): ScoreObjective? =
        scoreboard.getObjectiveInDisplaySlot(1)
}
