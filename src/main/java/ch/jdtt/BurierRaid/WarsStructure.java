package ch.jdtt.BurierRaid;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import org.bukkit.Chunk;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WarsStructure {
    private Faction starter;
    private WarObservableArrayList<Faction> requested;
    private WarObservableArrayList<Faction> joined;
    private Objective objective;
    private List<Chunk> wildernessChunks = new ArrayList<>();
    private Map<String, List<FLocation>> totemChunks = new LinkedHashMap<>();
    private Map<String, Map<String, Integer>> allies = new LinkedHashMap<>();
    private Faction winnerFaction;
    private Faction defeatedFaction;
    private Map<String, Faction> reward = new LinkedHashMap<>();
    private Integer time;

    public WarsStructure(Faction starter, List<FLocation> totemChunks, List<Chunk> wildernessChunks, List<Faction> requested, WarChangeListener requestedListener, String warHash, WarChangeListener joiningListener, Objective objective) {
        this.starter = starter;
        this.requested = new WarObservableArrayList<>(requested, requestedListener, warHash);
        joined = new WarObservableArrayList<>(joiningListener, warHash);
        this.objective = objective;
        this.totemChunks.put(starter.getId(), totemChunks);
        this.wildernessChunks.addAll(wildernessChunks);
    }

    public Objective getObjective() {
        return objective;
    }
    public void joinWar(Faction joiningFaction, List<FLocation> totemChunks, List<Chunk> wildernessChunks) {
        this.joined.add(joiningFaction);
        this.totemChunks.put(joiningFaction.getId(), totemChunks);
        this.wildernessChunks.addAll(wildernessChunks);
    }
    public void declineWar(Faction factionDeclining) {
        this.requested.remove(factionDeclining);
    }
    public WarObservableArrayList<Faction> getJoined() {
        return joined;
    }
    public Faction getStarter() {
        return starter;
    }
    public List<Faction> getRequested() {
        return requested;
    }
    public List<Chunk> getWildernessChunks() {
        return wildernessChunks;
    }
    public Map<String, List<FLocation>> getTotemChunks() {
        return totemChunks;
    }
    public void joinWarAsAlly(String warHash, Faction allyFaction, String assaultedFaction) {
        Map<String, Integer> participantsByFaction = new LinkedHashMap<>();
        if (allies.containsKey(warHash)) {
            if (allies.get(warHash).containsKey(assaultedFaction)) {
                allies.get(warHash).replace(assaultedFaction, allies.get(warHash).get(assaultedFaction) + allyFaction.getOnlinePlayers().size());
            } else {
                allies.get(warHash).put(assaultedFaction, allyFaction.getOnlinePlayers().size());
            }
        } else {
            participantsByFaction.put(assaultedFaction, allyFaction.getOnlinePlayers().size());
            allies.put(warHash, participantsByFaction);
        }
    }
    public Integer getAllies(String warHash, String assaultedFac) {
        if (allies.containsKey(warHash)) {
            return allies.get(warHash).getOrDefault(assaultedFac, 0);
        } else return 0;

    }
    public void setDefeatedFaction(Faction defeatedFaction) {
        this.defeatedFaction = defeatedFaction;
    }
    public void setWinnerFaction(Faction winnerFaction) {
        this.winnerFaction = winnerFaction;
    }
    public Faction getDefeatedFaction() {
        return defeatedFaction;
    }
    public Faction getWinnerFaction() {
        return winnerFaction;
    }
    public void setTime(Integer time) {
        this.time = time;
    }
    public Integer getTime() {
        return time;
    }
}
