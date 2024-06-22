package ch.jdtt.BurierRaid;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.shade.xseries.messages.Titles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WarConstructor {
    Map<String, WarsStructure> wars = new LinkedHashMap<>();
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Map<String, Set<String>> preRequestedUpdate = new LinkedHashMap<>();
    Map<String, Set<String>> initialRequest = new LinkedHashMap<>();


    WarChangeListener joiningListener = warHash -> {
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
        } else {
            war.getObjective().getScoreboard().resetScores("Waiting for :");
            Thread preparationTimeThread = new Thread(new War(warHash, war));
            preparationTimeThread.start();
        }
    };

    WarChangeListener decliningListener = warHash -> {
        WarsStructure war = wars.get(warHash);
        Set<String> newRequestedSet = new HashSet<>();
        war.getRequested().forEach(faction -> newRequestedSet.add(faction.getTag()));
        Set<String> declined = new HashSet<>(initialRequest.get(warHash));
        declined.removeAll(newRequestedSet);

        war.getStarter().getFPlayerLeader().sendMessage(ChatColor.RED+"The faction(s) "+ChatColor.BOLD+String.join(" ", declined)+ChatColor.RED+"ran AWAY, maybe you where "+ChatColor.DARK_RED+ChatColor.BOLD.toString()+"TOO POWERFUL!!!!");

        war.getObjective().getScoreboard().resetScores(ChatColor.BLUE+String.join(" ", preRequestedUpdate.get(warHash)));
        if (!war.getJoined().isEmpty()) {
            war.getObjective().getScore(ChatColor.BLUE+String.join(" ", newRequestedSet)).setScore(13);
            preRequestedUpdate.replace(warHash, newRequestedSet);
        }

        if (newRequestedSet.isEmpty()) {
            endWar("DECLINED", warHash);
            war.getObjective().getScoreboard().resetScores("Waiting for :");
        }
    };

    public String create(Faction starter, List<Faction> requested) {
        String warHash = Hashing.sha384().hashInt(requested.hashCode()).toString();
        Scoreboard board = manager.getNewScoreboard();
        Objective warObjective = board.registerNewObjective("war", "dummy");

        Set<String> initialRequestedSet = new HashSet<>();
        requested.forEach(faction -> initialRequestedSet.add(faction.getTag()));
        initialRequest.put(warHash, initialRequestedSet);

        warObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        warObjective.setDisplayName(ChatColor.DARK_RED+ChatColor.BOLD.toString()+starter.getTag()+"'s"+ChatColor.DARK_RED+" WAR");
        preRequestedUpdate.put(warHash, initialRequest.get(warHash));

        warObjective.getScore(" ").setScore(15);
        warObjective.getScore("Waiting for :").setScore(14);
        warObjective.getScore(ChatColor.BLUE+String.join(" ", initialRequest.get(warHash))).setScore(13);

        starter.getOnlinePlayers().forEach(player -> player.setScoreboard(board));
        wars.put(warHash, new WarsStructure(starter, requested, decliningListener, warHash, joiningListener, warObjective));
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
    public String decline(Faction denier, String warHash) {
        if (!wars.containsKey(warHash)) {
            return "false";
        }
        wars.get(warHash).declineWar(denier);
        return wars.get(warHash).getStarter().getTag();
    }
    public void endWar(String reason, String warHash) {
        File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
        Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
        Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>(){}.getType();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (FactionRaidListF.length() != 0) {
            try {
                FactionRaids = gson.fromJson(Files.readString(FactionRaidListF.toPath()), FactionRaidMapType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (Objects.equals(reason, "DECLINED")){
            WarsStructure war = wars.get(warHash);
            war.getStarter().getFPlayerLeader().getPlayer().sendMessage(ChatColor.RED+"Everybody ran AWAY, you where definitely "+ChatColor.DARK_RED+ChatColor.BOLD.toString()+"TOO POWERFUL!!!!");
            war.getStarter().getOnlinePlayers().forEach(player -> player.setScoreboard(manager.getNewScoreboard()));
            war.getJoined().forEach(faction -> faction.getOnlinePlayers().forEach(player -> player.setScoreboard(manager.getNewScoreboard())));

            World w = war.getStarter().getFPlayerLeader().getPlayer().getWorld();
            TotemLocation StartertotemLocation = FactionRaids.get(war.getStarter().getId()).getLocation();

            for (Entity ArmorStandTotem : w.getEntities()) {
                if (ArmorStandTotem.getUniqueId().toString().equals(FactionRaids.get(war.getStarter().getId()).getTotemUUID())) {
                    FactionRaids.replace(war.getStarter().getId(), new FactionRaid(war.getStarter().getTag(),
                            ArmorStandTotem.getUniqueId().toString(),
                            false, "",
                            StartertotemLocation.getX(), StartertotemLocation.getY(), StartertotemLocation.getZ()));
                }
            }

            if (!war.getJoined().isEmpty()) {
                for (Faction faction : war.getJoined()) {
                    TotemLocation JoinertotemLocation = FactionRaids.get(faction.getId()).getLocation();
                    for (Entity ArmorStandTotem : w.getEntities()) {
                        if (ArmorStandTotem.getUniqueId().toString().equals(FactionRaids.get(faction.getId()).getTotemUUID())) {
                            FactionRaids.replace(faction.getId(), new FactionRaid(faction.getTag(),
                                    ArmorStandTotem.getUniqueId().toString(),
                                    false, "",
                                    JoinertotemLocation.getX(), JoinertotemLocation.getY(), JoinertotemLocation.getZ()));
                        }
                    }
                }
            }

            try {
                FileWriter JSONwriter = new FileWriter(FactionRaidListF);
                JSONwriter.write(gson.toJson(FactionRaids));
                JSONwriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
class War implements Runnable {
    String warHash;
    int time = 300;
    Objective objective;
    WarsStructure war;
    public War(String warHash, WarsStructure war) {
        this.warHash = warHash;
        this.objective = war.getObjective();
        this.war = war;
    }
    @Override
    public void run() {
        war.getStarter().getOnlinePlayers().forEach(player -> {
            Titles.sendTitle(player, 1, 3, 1, ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME", ChatColor.DARK_PURPLE+"You have 5 minutes");
        });
        war.getJoined().forEach(faction -> {
            faction.getOnlinePlayers().forEach(player -> {
                Titles.sendTitle(player, 1, 3, 1, ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME", ChatColor.DARK_PURPLE+"You have 5 minutes");
            });
        });
        objective.getScore(ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME").setScore(14);
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            time = time -1;
            if (time != 300) {
                objective.getScoreboard().resetScores(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(time+1) % 60, TimeUnit.SECONDS.toSeconds(time+1) % 60));
            }
            objective.getScore(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(time) % 60, TimeUnit.SECONDS.toSeconds(time) % 60)).setScore(13);
            if (time <= 0) {
                objective.getScoreboard().resetScores(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(time) % 60, TimeUnit.SECONDS.toSeconds(time) % 60));
                break;
            }
        }
        objective.getScoreboard().resetScores(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(time) % 60, TimeUnit.SECONDS.toSeconds(time) % 60));
    }
}
