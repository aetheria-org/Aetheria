package io.hamlook.aetheria.features.dungeons.utils;

import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhaseDetector {
    
    private static final Pattern BLOOD_DOOR = Pattern.compile("The BLOOD DOOR has been opened!");
    private static final Pattern RUN_FAILED = Pattern.compile("Warning! This dungeon will close in 10s");
    private static final Pattern BOSS_SLAIN = Pattern.compile("Defeated (.+) in \\d");
    private static final Pattern SCORE_LINE = Pattern.compile("Team Score: (\\d+) \\((.{1,2})\\) *(\\(NEW RECORD!\\))?");
    private static final Pattern XP_LINE = Pattern.compile("(\\+[\\d,.]+\\s?\\w+ Experience)(?:\\(.+\\))?");
    
    private static final Pattern MAXOR_START = Pattern.compile("BOSS.*Maxor.*WELL WELL WELL");
    private static final Pattern MAXOR_END = Pattern.compile("BOSS.*Maxor.*TOO YOUNG TO DIE");
    private static final Pattern STORM_START = Pattern.compile("BOSS.*Storm.*Pathetic Maxor");
    private static final Pattern STORM_END = Pattern.compile("BOSS.*Storm.*At least my son");
    private static final Pattern TERMINAL_START = Pattern.compile("BOSS.*Goldor.*Who dares trespass");
    private static final Pattern GOLDOR_FIGHT = Pattern.compile("The Core entrance is opening");
    private static final Pattern GOLDOR_END = Pattern.compile("BOSS.*Goldor.*You have done it");
    private static final Pattern NECRON_START = Pattern.compile("BOSS.*Necron.*Finally, I heard so much");
    private static final Pattern NECRON_END = Pattern.compile("BOSS.*Necron.*The Catacombs.*are no more");
    private static final Pattern WITHER_START = Pattern.compile("BOSS.*(?:WITHER KING.*You\\.\\.\\. again|The Wither King.*Ohh)");
    private static final Pattern WITHER_END = Pattern.compile("BOSS.*WITHER KING.*My strengths are depleting");
    
    private static final Pattern SCARF_P1_START = Pattern.compile("BOSS.*Scarf.*ARISE, MY CREATIONS!");
    private static final Pattern SCARF_P1_END = Pattern.compile("BOSS.*Scarf.*Those toys are not strong enough I see");
    private static final Pattern SCARF_P2_END = Pattern.compile("BOSS.*Scarf.*You'll never beat my teacher");
    
    private static final Pattern PROFESSOR_P1_START = Pattern.compile("BOSS.*The Professor.*I'll show you real power!");
    private static final Pattern PROFESSOR_P1_END = Pattern.compile("BOSS.*The Professor.*Oh\\? You found my Guardians' one weakness\\?");
    private static final Pattern PROFESSOR_P2_END = Pattern.compile("BOSS.*The Professor.*I see\\. You have forced me to use my ultimate technique");
    private static final Pattern PROFESSOR_P3_END = Pattern.compile("BOSS.*The Professor.*What\\?! My Guardian power is unbeatable!");
    
    private static final Pattern TERRA_START = Pattern.compile("BOSS.*Sadan.*I am the bridge between this realm");
    private static final Pattern TERRA_END = Pattern.compile("BOSS.*Sadan.*ENOUGH!");
    private static final Pattern GIANTS_END = Pattern.compile("BOSS.*Sadan.*I.m sorry but I need to concentrate");
    private static final Pattern SADAN_END = Pattern.compile("BOSS.*Sadan.*FATHER, FORGIVE ME");
    
    private final DungeonTimers timers;
    private final DungeonEndStats endStats;
    private final PBTracker pbTracker;
    
    public PhaseDetector(DungeonTimers timers, DungeonEndStats endStats, PBTracker pbTracker) {
        this.timers = timers;
        this.endStats = endStats;
        this.pbTracker = pbTracker;
    }
    
    public boolean handleGeneralEvents(String clean) {
        if (timers.getBloodTime() == 0 && BLOOD_DOOR.matcher(clean).find()) {
            timers.setBloodTime(timers.elapsed());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_blood", timers.getBloodTime(), "Blood Rush");
            return true;
        }
        
        if (RUN_FAILED.matcher(clean).find()) {
            timers.setRunFailed(true);
            timers.setRunEnded(true);
            DungeonMapOverlay.dungeonRunEnded = true;
            timers.setBossDeadTime(timers.elapsed());
            timers.freezeOpenPhases();
            return true;
        }
        
        Matcher m = BOSS_SLAIN.matcher(clean);
        if (m.find()) {
            timers.setBossDeadTime(timers.elapsed());
            timers.setRunEnded(true);
            DungeonMapOverlay.dungeonRunEnded = true;
            endStats.setBossName(m.group(1).trim());
            timers.freezeOpenPhases();
            pbTracker.checkAndSaveRunPb(timers);
            return true;
        }
        
        m = SCORE_LINE.matcher(clean);
        if (m.find()) {
            endStats.setScore(m.group(1));
            endStats.setGrade(m.group(2));
            endStats.setScorePB(m.group(3) != null && !m.group(3).isEmpty());
            return false;
        }
        
        m = XP_LINE.matcher(clean);
        if (m.find()) {
            endStats.addXp(m.group(1).replace("Experience", "EXP").replace("Catacombs", "Cata"));
            return false;
        }
        
        return false;
    }
    
    public void handleFloorPhases(String clean) {
        DungeonFloor floor = timers.getCurrentFloor();
        if (floor.isF2orM2()) {
            handleScarfPhases(clean);
        } else if (floor.isF3orM3()) {
            handleProfessorPhases(clean);
        } else if (floor.isF6orM6()) {
            handleSadanPhases(clean);
        } else if (floor.isF7orM7()) {
            handleNecronPhases(clean);
        }
    }
    
    private void handleScarfPhases(String clean) {
        if (timers.getBossTime() == 0) return;
        
        if (SCARF_P1_START.matcher(clean).find() && timers.getScarfP1Start() == 0) {
            timers.setScarfP1Start(timers.elapsed());
        }
        if (SCARF_P1_END.matcher(clean).find() && timers.getScarfP1End() == 0) {
            timers.setScarfP1End(timers.elapsed());
            timers.setScarfP2Start(timers.getScarfP1End());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p1", 
                timers.getScarfP1End() - timers.getScarfP1Start(), "Scarf P1");
        }
        if (SCARF_P2_END.matcher(clean).find() && timers.getScarfP2End() == 0) {
            timers.setScarfP2End(timers.elapsed());
            timers.setBossDeadTime(timers.getScarfP2End());
            timers.setRunEnded(true);
            DungeonMapOverlay.dungeonRunEnded = true;
            endStats.setBossName("Scarf");
            timers.freezeOpenPhases();
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p2", 
                timers.getScarfP2End() - timers.getScarfP2Start(), "Scarf P2");
            pbTracker.checkAndSaveRunPb(timers);
        }
    }
    
    private void handleProfessorPhases(String clean) {
        if (timers.getBossTime() == 0) return;
        
        if (PROFESSOR_P1_START.matcher(clean).find() && timers.getProfessorP1Start() == 0) {
            timers.setProfessorP1Start(timers.elapsed());
        }
        if (PROFESSOR_P1_END.matcher(clean).find() && timers.getProfessorP1End() == 0) {
            timers.setProfessorP1End(timers.elapsed());
            timers.setProfessorP2Start(timers.getProfessorP1End());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p1", 
                timers.getProfessorP1End() - timers.getProfessorP1Start(), "Professor P1");
        }
        if (PROFESSOR_P2_END.matcher(clean).find() && timers.getProfessorP2End() == 0) {
            timers.setProfessorP2End(timers.elapsed());
            timers.setProfessorP3Start(timers.getProfessorP2End());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p2", 
                timers.getProfessorP2End() - timers.getProfessorP2Start(), "Professor P2");
        }
        if (PROFESSOR_P3_END.matcher(clean).find() && timers.getProfessorP3End() == 0) {
            timers.setProfessorP3End(timers.elapsed());
            timers.setBossDeadTime(timers.getProfessorP3End());
            timers.setRunEnded(true);
            DungeonMapOverlay.dungeonRunEnded = true;
            endStats.setBossName("The Professor");
            timers.freezeOpenPhases();
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p3", 
                timers.getProfessorP3End() - timers.getProfessorP3Start(), "Professor P3");
            pbTracker.checkAndSaveRunPb(timers);
        }
    }
    
    private void handleSadanPhases(String clean) {
        if (timers.getBossTime() == 0) return;
        
        if (TERRA_START.matcher(clean).find() && timers.getTerraStart() == 0) {
            timers.setTerraStart(timers.elapsed());
        }
        if (TERRA_END.matcher(clean).find() && timers.getTerraEnd() == 0) {
            timers.setTerraEnd(timers.elapsed());
            timers.setGiantsStart(timers.getTerraEnd());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_terra", 
                timers.getTerraEnd() - timers.getTerraStart(), "Terracotta");
        }
        if (GIANTS_END.matcher(clean).find() && timers.getGiantsEnd() == 0) {
            timers.setGiantsEnd(timers.elapsed());
            timers.setSadanStart(timers.getGiantsEnd());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_giants", 
                timers.getGiantsEnd() - timers.getGiantsStart(), "Giants");
        }
        if (SADAN_END.matcher(clean).find() && timers.getSadanEnd() == 0) {
            timers.setSadanEnd(timers.elapsed());
            timers.setBossDeadTime(timers.getSadanEnd());
            timers.setRunEnded(true);
            DungeonMapOverlay.dungeonRunEnded = true;
            endStats.setBossName("Sadan");
            timers.freezeOpenPhases();
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_sadan", 
                timers.getSadanEnd() - timers.getSadanStart(), "Sadan");
            pbTracker.checkAndSaveRunPb(timers);
        }
    }
    
    private void handleNecronPhases(String clean) {
        if (MAXOR_START.matcher(clean).find() && timers.getMaxorStart() == 0) {
            timers.setMaxorStart(timers.elapsed());
        }
        if (MAXOR_END.matcher(clean).find() && timers.getMaxorEnd() == 0) {
            timers.setMaxorEnd(timers.elapsed());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p1", 
                timers.getMaxorEnd() - timers.getMaxorStart(), "P1 (Maxor)");
        }
        if (STORM_START.matcher(clean).find() && timers.getStormStart() == 0) {
            timers.setStormStart(timers.elapsed());
        }
        if (STORM_END.matcher(clean).find() && timers.getStormEnd() == 0) {
            timers.setStormEnd(timers.elapsed());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p2", 
                timers.getStormEnd() - timers.getStormStart(), "P2 (Storm)");
        }
        if (TERMINAL_START.matcher(clean).find() && timers.getTerminalStart() == 0) {
            timers.setTerminalStart(timers.elapsed());
        }
        if (GOLDOR_FIGHT.matcher(clean).find() && timers.getGoldorFight() == 0) {
            timers.setGoldorFight(timers.elapsed());
        }
        if (GOLDOR_END.matcher(clean).find() && timers.getGoldorEnd() == 0) {
            timers.setGoldorEnd(timers.elapsed());
            timers.setNecronStart(timers.getGoldorEnd());
            if (timers.getTerminalStart() > 0) {
                pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p3", 
                    timers.getGoldorEnd() - timers.getTerminalStart(), "P3 (Terminals + Goldor)");
            }
        }
        if (NECRON_START.matcher(clean).find() && timers.getNecronStart() == 0) {
            timers.setNecronStart(timers.elapsed());
        }
        if (NECRON_END.matcher(clean).find() && timers.getNecronEnd() == 0) {
            timers.setNecronEnd(timers.elapsed());
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p4", 
                timers.getNecronEnd() - timers.getNecronStart(), "P4 (Necron)");
            if (!timers.getCurrentFloor().isMasterMode()) {
                timers.setBossDeadTime(timers.getNecronEnd());
                timers.setRunEnded(true);
                DungeonMapOverlay.dungeonRunEnded = true;
                pbTracker.checkAndSaveRunPb(timers);
            }
        }
        if (WITHER_START.matcher(clean).find() && timers.getWitherStart() == 0) {
            timers.setWitherStart(timers.elapsed());
        }
        if (WITHER_END.matcher(clean).find() && timers.getWitherEnd() == 0) {
            timers.setWitherEnd(timers.elapsed());
            timers.setBossDeadTime(timers.getWitherEnd());
            timers.setRunEnded(true);
            DungeonMapOverlay.dungeonRunEnded = true;
            pbTracker.checkPhasePb(timers.getCurrentFloor().name() + "_p5", 
                timers.getWitherEnd() - timers.getWitherStart(), "P5 (Wither King)");
            pbTracker.checkAndSaveRunPb(timers);
        }
    }
}
