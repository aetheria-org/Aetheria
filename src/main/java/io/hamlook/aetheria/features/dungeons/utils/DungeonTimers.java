package io.hamlook.aetheria.features.dungeons.utils;

import lombok.Getter;
import lombok.Setter;

public class DungeonTimers {
    
    @Getter @Setter
    private DungeonFloor currentFloor = DungeonFloor.NONE;
    
    @Getter @Setter
    private boolean inDungeon = false;
    
    @Getter @Setter
    private boolean runFailed = false;
    
    @Getter @Setter
    private boolean runEnded = false;
    
    @Getter @Setter
    private int lastClearedPct = 0;
    
    @Getter @Setter
    private long runStart = 0;
    
    @Getter @Setter
    private long clearedTime = 0;
    
    @Getter @Setter
    private long bloodTime = 0;

    @Getter @Setter
    private boolean bloodCleared = false;

    @Getter @Setter
    private long bossTime = 0;
    
    @Getter @Setter
    private long bossDeadTime = 0;
    
    @Getter @Setter
    private long maxorStart = 0, maxorEnd = 0;
    
    @Getter @Setter
    private long stormStart = 0, stormEnd = 0;
    
    @Getter @Setter
    private long terminalStart = 0, goldorFight = 0, goldorEnd = 0;
    
    @Getter @Setter
    private long necronStart = 0, necronEnd = 0;
    
    @Getter @Setter
    private long witherStart = 0, witherEnd = 0;
    
    @Getter @Setter
    private long scarfP1Start = 0, scarfP1End = 0;
    
    @Getter @Setter
    private long scarfP2Start = 0, scarfP2End = 0;
    
    @Getter @Setter
    private long professorP1Start = 0, professorP1End = 0;
    
    @Getter @Setter
    private long professorP2Start = 0, professorP2End = 0;
    
    @Getter @Setter
    private long professorP3Start = 0, professorP3End = 0;
    
    @Getter @Setter
    private long terraStart = 0, terraEnd = 0;
    
    @Getter @Setter
    private long giantsStart = 0, giantsEnd = 0;
    
    @Getter @Setter
    private long sadanStart = 0, sadanEnd = 0;
    
    public long elapsed() {
        return runStart == 0 ? 0 : System.currentTimeMillis() - runStart;
    }
    
    public void reset() {
        inDungeon = false;
        runFailed = false;
        runEnded = false;
        currentFloor = DungeonFloor.NONE;
        runStart = 0;
        clearedTime = bloodTime = bossTime = bossDeadTime = 0;
        bloodCleared = false;
        maxorStart = maxorEnd = stormStart = stormEnd = 0;
        terminalStart = goldorFight = goldorEnd = necronStart = necronEnd = 0;
        witherStart = witherEnd = 0;
        scarfP1Start = scarfP1End = scarfP2Start = scarfP2End = 0;
        professorP1Start = professorP1End = professorP2Start = professorP2End = professorP3Start = professorP3End = 0;
        terraStart = terraEnd = giantsStart = giantsEnd = sadanStart = sadanEnd = 0;
        lastClearedPct = 0;
    }
    
    public void freezeOpenPhases() {
        long now = elapsed();
        if (currentFloor.isF2orM2()) {
            if (scarfP1Start > 0 && scarfP1End == 0) scarfP1End = now;
            if (scarfP2Start > 0 && scarfP2End == 0) scarfP2End = now;
        }
        if (currentFloor.isF3orM3()) {
            if (professorP1Start > 0 && professorP1End == 0) professorP1End = now;
            if (professorP2Start > 0 && professorP2End == 0) professorP2End = now;
            if (professorP3Start > 0 && professorP3End == 0) professorP3End = now;
        }
        if (currentFloor.isF6orM6()) {
            if (terraStart > 0 && terraEnd == 0) terraEnd = now;
            if (giantsStart > 0 && giantsEnd == 0) giantsEnd = now;
            if (sadanStart > 0 && sadanEnd == 0) sadanEnd = now;
        }
        if (currentFloor.isF7orM7()) {
            if (stormStart > 0 && stormEnd == 0) stormEnd = now;
            if (terminalStart > 0 && goldorFight == 0) goldorFight = now;
            if (goldorFight > 0 && goldorEnd == 0) goldorEnd = now;
            if (necronStart > 0 && necronEnd == 0) necronEnd = now;
            if (witherStart > 0 && witherEnd == 0) witherEnd = now;
        }
    }
}
