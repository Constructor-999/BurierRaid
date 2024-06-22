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
}
