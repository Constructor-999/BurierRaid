package ch.jdtt.BurierRaid;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.shade.xseries.messages.Titles;
import com.saicone.rtag.RtagEntity;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
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

public class WarConstructor implements Listener {
    Map<String, WarsStructure> wars = new LinkedHashMap<>();
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Map<String, Set<String>> preRequestedUpdate = new LinkedHashMap<>();
    Map<String, Set<String>> initialRequest = new LinkedHashMap<>();
    EndChronometer endChronometerListener;

    @EventHandler
    public void onEDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            if (event.getEntity().getType().equals(EntityType.ARMOR_STAND)) {
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
                FactionRaids.values().forEach(factionRaid -> {
                    if (factionRaid.getInWar()) {
                        if (event.getEntity().equals(Bukkit.getEntity(UUID.fromString(factionRaid.getTotemUUID())))) {
                            endChronometerListener.setEndTimer(factionRaid.getWarHash());
                            endChronometerListener.setDefeatedFaction(factionRaid.getWarHash(), factionRaid.getFaction());
                        }
                    }
                });
            }
        }
    }
    WarChangeListener joiningListener = warHash -> {
        WarsStructure war = wars.get(warHash);
        Set<String> requested = new HashSet<>();
        Set<String> joined = new HashSet<>();

        war.getRequested().forEach(factionRequested -> requested.add(factionRequested.getTag()));
        war.getJoined().forEach(factionJoined -> joined.add(factionJoined.getTag()));
        requested.removeAll(joined);

        war.getObjective().getScoreboard().resetScores(ChatColor.BLUE+String.join(" ", preRequestedUpdate.get(warHash)));
        if (!war.getRequested().equals(war.getJoined())) {
            war.getObjective().getScore(ChatColor.BLUE+String.join(" ", requested)).setScore(23);
            preRequestedUpdate.replace(warHash, requested);
        } else {
            war.getObjective().getScoreboard().resetScores("Waiting for :");
            Thread warThread = new Thread(new War(warHash, war, endChronometerListener));
            warThread.start();
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
            war.getObjective().getScore(ChatColor.BLUE+String.join(" ", newRequestedSet)).setScore(23);
            preRequestedUpdate.replace(warHash, newRequestedSet);
        }

        if (newRequestedSet.isEmpty()) {
            endWar("DECLINED", warHash);
            war.getObjective().getScoreboard().resetScores("Waiting for :");
        }
    };

    public String create(Faction starter, List<Faction> requested, List<FLocation> totemChunks, List<Chunk> totemsWilderness) {
        String warHash = Hashing.sha384().hashInt(requested.hashCode()).toString();
        Scoreboard board = manager.getNewScoreboard();
        Objective warObjective = board.registerNewObjective("war", "dummy");

        Set<String> initialRequestedSet = new HashSet<>();
        requested.forEach(faction -> initialRequestedSet.add(faction.getTag()));
        initialRequest.put(warHash, initialRequestedSet);

        warObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        warObjective.setDisplayName(ChatColor.DARK_RED+ChatColor.BOLD.toString()+starter.getTag()+"'s"+ChatColor.DARK_RED+" WAR");
        preRequestedUpdate.put(warHash, initialRequest.get(warHash));

        warObjective.getScore(" ").setScore(25);
        warObjective.getScore("Waiting for :").setScore(24);
        warObjective.getScore(ChatColor.BLUE+String.join(" ", initialRequest.get(warHash))).setScore(23);

        starter.getOnlinePlayers().forEach(player -> player.setScoreboard(board));
        wars.put(warHash, new WarsStructure(starter, totemChunks, totemsWilderness, requested, decliningListener, warHash, joiningListener, warObjective));
        return warHash;
    }
    public String join(Faction joiner, String warHash, List<Chunk> totemsWilderness, List<FLocation> totemChunks) {
        if (!wars.containsKey(warHash)) {
            return "false";
        }
        wars.get(warHash).joinWar(joiner, totemChunks, totemsWilderness);
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
    EndChronometer endChronometer;
    public War(String warHash, WarsStructure war, EndChronometer endChronometer) {
        this.warHash = warHash;
        this.objective = war.getObjective();
        this.war = war;
        this.endChronometer = endChronometer;
    }
    @Override
    public void run() {
        war.getStarter().getOnlinePlayers().forEach(player -> {
            Titles.sendTitle(player, 1, 3, 1, ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME", ChatColor.DARK_PURPLE+ChatColor.ITALIC.toString()+"You have 5 minutes");
            player.sendMessage(ChatColor.LIGHT_PURPLE+"Regenerating Chunks...");
        });
        war.getJoined().forEach(faction -> faction.getOnlinePlayers().forEach(player -> {
            Titles.sendTitle(player, 1, 3, 1, ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME", ChatColor.DARK_PURPLE+ChatColor.ITALIC.toString()+"You have 5 minutes");
            player.sendMessage(ChatColor.LIGHT_PURPLE+"Regenerating Chunks...");
        }));
        war.getWildernessChunks().forEach(chunk -> war.getStarter().getFPlayerLeader().getPlayer().getWorld().regenerateChunk(chunk.getX(), chunk.getZ()));
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
            objective.getScore(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(time) % 60, TimeUnit.SECONDS.toSeconds(time) % 60)).setScore(23);
            if (time <= 0) {
                objective.getScoreboard().resetScores(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(time) % 60, TimeUnit.SECONDS.toSeconds(time) % 60));
                break;
            }
        }

        objective.getScoreboard().resetScores(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(time) % 60, TimeUnit.SECONDS.toSeconds(time) % 60));
        war.getStarter().getOnlinePlayers().forEach(player -> Titles.sendTitle(player, 1, 3, 1, ChatColor.DARK_RED+ChatColor.BOLD.toString()+"END OF PREPARATION TIME", ChatColor.GREEN+"Now totems are VULNERABLE"));
        war.getJoined().forEach(faction -> faction.getOnlinePlayers().forEach(player -> Titles.sendTitle(player, 1, 3, 1, ChatColor.DARK_RED+ChatColor.BOLD.toString()+"END OF PREPARATION TIME", ChatColor.GREEN+"Now totems are VULNERABLE")));

        World w = war.getStarter().getFPlayerLeader().getPlayer().getWorld();
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

        for (Entity ArmorStandTotem : w.getEntities()) {
            if (ArmorStandTotem.getUniqueId().toString().equals(FactionRaids.get(war.getStarter().getId()).getTotemUUID())) {
                RtagEntity.edit(ArmorStandTotem, tag -> {
                    tag.set(ChatColor.DARK_RED+war.getStarter().getTag()+"'s raid TOTEM", "CustomName");
                    tag.set(true, "CustomNameVisible");
                    tag.set(true, "PersistenceRequired");
                    tag.set(true, "NoGravity");
                    tag.set(false, "Invulnerable");
                });
            }
        }

        war.getTotemChunks().get(war.getStarter().getId()).forEach(fLocation -> war.getStarter().clearClaimOwnership(fLocation));
        war.getJoined().forEach(joidedFac -> war.getTotemChunks().get(joidedFac.getId()).forEach(joidedFac::clearClaimOwnership));
        objective.getScore(ChatColor.AQUA+ChatColor.BOLD.toString()+"TIME").setScore(24);
        objective.getScore(" ").setScore(22);
        objective.getScore(ChatColor.BLUE+war.getStarter().getTag()+"'s TOTEM location").setScore(21);
        objective.getScore(ChatColor.RED+FactionRaids.get(war.getStarter().getId()).getLocation().getX().toString()+ " " +
                FactionRaids.get(war.getStarter().getId()).getLocation().getY().toString()+ " " +
                FactionRaids.get(war.getStarter().getId()).getLocation().getZ().toString()).setScore(20);
        int score = 19;
        for (Faction faction : war.getJoined()) {
            objective.getScore(ChatColor.BLUE+faction.getTag()+"'s TOTEM location").setScore(score);
            objective.getScore(ChatColor.RED+FactionRaids.get(faction.getId()).getLocation().getX().toString()+ " " +
                    FactionRaids.get(faction.getId()).getLocation().getY().toString()+ " " +
                    FactionRaids.get(faction.getId()).getLocation().getZ().toString()).setScore(score -1);
            score = score -2;
        }

        int chronometer = 0;
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            chronometer = chronometer +1;
            if (chronometer != 0) {
                objective.getScoreboard().resetScores(ChatColor.DARK_PURPLE + String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(chronometer + 1) % 60, TimeUnit.SECONDS.toSeconds(chronometer + 1) % 60));
            }
            objective.getScore(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(chronometer) % 60, TimeUnit.SECONDS.toSeconds(chronometer) % 60)).setScore(23);
            if (endChronometer.isTimeToEnd(warHash)) {
                objective.getScoreboard().resetScores(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(chronometer) % 60, TimeUnit.SECONDS.toSeconds(chronometer) % 60));
                break;
            }
        }


    }
}
