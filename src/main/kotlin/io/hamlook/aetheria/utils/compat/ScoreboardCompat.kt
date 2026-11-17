package io.hamlook.aetheria.utils.compat

import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.Scoreboard
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScorePlayerTeam
//#if MC > 1.21
//$$ import net.minecraft.scoreboard.ScoreboardDisplaySlot
//$$ import net.minecraft.scoreboard.ScoreboardEntry
//$$ import net.minecraft.text.Text
//#endif
import java.util.ConcurrentModificationException
import kotlin.math.max

object ScoreboardCompat {

    @JvmStatic
    fun getSidebarObjective(scoreboard: Scoreboard): ScoreObjective? =
        //#if MC < 1.21
        scoreboard.getObjectiveInDisplaySlot(1)
        //#else
        //$$ scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)
        //#endif

    @JvmStatic
    fun getSidebarLines(scoreboard: Scoreboard): List<String> {
        //#if MC < 1.21
        val objective = getSidebarObjective(scoreboard) ?: return emptyList()
        val scores = try {
            scoreboard.getSortedScores(objective).filter { s -> s != null && s.playerName != null && !s.playerName.startsWith("#") }
        } catch (e: ConcurrentModificationException) {
            return emptyList()
        }
        val size = scores.size
        return scores.drop(max(0, size - 15)).map { score ->
            ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.playerName), score.playerName)
        }
        //#else
        //$$ val objective = getSidebarObjective(scoreboard) ?: return emptyList()
        //$$ val entries = try {
        //$$     scoreboard.getScoreboardEntries(objective).filter { it != null && !it.hidden() && !it.owner().startsWith("#") }
        //$$ } catch (e: ConcurrentModificationException) {
        //$$     return emptyList()
        //$$ }
        //$$ val size = entries.size
        //$$ return entries.drop(max(0, size - 15)).map { e ->
        //$$     TextCompat.getFormattedText(Team.decorateName(scoreboard.getScoreHolderTeam(e.owner()), Text.literal(e.owner())))
        //$$ }
        //#endif
    }
}
