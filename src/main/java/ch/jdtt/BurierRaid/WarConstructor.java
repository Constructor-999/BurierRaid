package ch.jdtt.BurierRaid;

import com.google.common.hash.Hashing;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.*;

public class WarConstructor {
    Map<String, WarsStructure> wars = new LinkedHashMap<>();
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Map<String, Set<String>> preRequestedUpdate = new LinkedHashMap<>();

    WarChangeListener warListener = warHash -> {
        WarsStructure war = wars.get(warHash);
        Set<String> requested = new HashSet<>();
        Set<String> joined = new HashSet<>();

        war.getRequested().forEach(factionRequested -> requested.add(factionRequested.getTag()));
        war.getJoined().forEach(factionJoined -> joined.add(factionJoined.getTag()));
        requested.removeAll(joined);

        war.getObjective().getScoreboard().resetScores(ChatColor.BLUE+String.join(" ", preRequestedUpdate.get(warHash)));
        if (!war.getRequested().equals(war.getJoined())) {
            war.getObjective().getScore(ChatColor.BLUE+String.join(" ", requested)).setScore(13);
            preRequestedUpdate.replace(warHash, requested);
            war.getObjective().getScoreboard().resetScores("Waiting for :");
        }
    };

    public String create(Faction starter, List<Faction> requested) {
        String warHash = Hashing.sha384().hashInt(requested.hashCode()).toString();
        Scoreboard board = manager.getNewScoreboard();
        Objective warObjective = board.registerNewObjective("war", "dummy");

        Set<String> requestedSet = new HashSet<>();
        requested.forEach(faction -> requestedSet.add(faction.getTag()));

        warObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        warObjective.setDisplayName(ChatColor.DARK_RED+ChatColor.BOLD.toString()+starter.getTag()+"'s"+ChatColor.DARK_RED+" WAR");
        preRequestedUpdate.put(warHash, requestedSet);

        warObjective.getScore(" ").setScore(15);
        warObjective.getScore("Waiting for :").setScore(14);
        warObjective.getScore(ChatColor.BLUE+String.join(" ", requestedSet)).setScore(13);

        starter.getOnlinePlayers().forEach(player -> player.setScoreboard(board));
        wars.put(warHash, new WarsStructure(starter, requested, warHash, warListener, warObjective));
        return warHash;
    }
    public String join(Faction joiner, String warHash) {
        if (!wars.containsKey(warHash)) {
            return "false";
        }
        wars.get(warHash).joinWar(joiner);
        joiner.getOnlinePlayers().forEach(player -> player.setScoreboard(wars.get(warHash).getObjective().getScoreboard()));
        return wars.get(warHash).getStarter().getTag();
    }
}
