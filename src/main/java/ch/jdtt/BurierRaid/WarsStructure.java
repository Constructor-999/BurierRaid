package ch.jdtt.BurierRaid;

import com.massivecraft.factions.Faction;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

public class WarsStructure {
    private Faction starter;
    private List<Faction> requested;
    private WarObservableArrayList<Faction> joined;
    private Objective objective;

    public WarsStructure(Faction starter, List<Faction> requested, String warHash, WarChangeListener listener, Objective objective) {
        this.starter = starter;
        this.requested = requested;
        joined = new WarObservableArrayList<>(listener, warHash);
        this.objective = objective;
    }

    public Objective getObjective() {
        return objective;
    }
    public void joinWar(Faction joiningFaction) {
        this.joined.add(joiningFaction);
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
