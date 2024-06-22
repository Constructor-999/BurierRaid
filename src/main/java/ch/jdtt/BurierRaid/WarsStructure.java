package ch.jdtt.BurierRaid;

import com.massivecraft.factions.Faction;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

public class WarsStructure {
    private Faction starter;
    private WarObservableArrayList<Faction> requested;
    private WarObservableArrayList<Faction> joined;
    private Objective objective;

    public WarsStructure(Faction starter, List<Faction> requested, WarChangeListener requestedListener, String warHash, WarChangeListener joiningListener, Objective objective) {
        this.starter = starter;
        this.requested = new WarObservableArrayList<>(requested, requestedListener, warHash);
        joined = new WarObservableArrayList<>(joiningListener, warHash);
        this.objective = objective;
    }

    public Objective getObjective() {
        return objective;
    }
    public void joinWar(Faction joiningFaction) {
        this.joined.add(joiningFaction);
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
}
