package ch.jdtt.BurierRaid;

import java.util.LinkedHashMap;
import java.util.Map;

public interface EndChronometer {
    Map<String, Boolean> endTimer = new LinkedHashMap<>();
    Map<String, String> defeatedFaction = new LinkedHashMap<>();

    default void setEndTimer(String warHash) {
        endTimer.put(warHash, true);
    }

    default Boolean isTimeToEnd(String warHash) {
        Boolean endTimer = this.endTimer.getOrDefault(warHash, false);
        if (endTimer) {
            this.endTimer.remove(warHash);
        }
        return endTimer;
    }

    default void setDefeatedFaction(String warHash, String defeatedFactionName){
        defeatedFaction.put(warHash, defeatedFactionName);
    }

    default String getDefeatedFaction(String warHash) {
        if (defeatedFaction.containsKey(warHash)) {
            String factionDefeatedName = defeatedFaction.get(warHash);
            defeatedFaction.remove(warHash);
            return factionDefeatedName;
        } else {
            return "";
        }
    }
}
