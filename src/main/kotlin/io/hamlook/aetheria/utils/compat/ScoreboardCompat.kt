package io.hamlook.aetheria.utils.compat

import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.Scoreboard
//#if MC > 1.21
//$$ import net.minecraft.scoreboard.ScoreboardDisplaySlot
//$$ import net.minecraft.scoreboard.ScoreboardEntry
//$$ import net.minecraft.text.Text
//#endif

object ScoreboardCompat {

    @JvmStatic
    fun getSidebarObjective(scoreboard: Scoreboard): ScoreObjective? =
        //#if MC < 1.21
        scoreboard.getObjectiveInDisplaySlot(1)
        //#else
        //$$ scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)
        //#endif
}
