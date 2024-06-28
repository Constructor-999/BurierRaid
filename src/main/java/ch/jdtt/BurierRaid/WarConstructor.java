package ch.jdtt.BurierRaid;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.shade.xseries.messages.Titles;
import com.saicone.rtag.RtagEntity;
import com.saicone.rtag.RtagItem;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.security.interfaces.DSAPublicKey;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class WarConstructor implements Listener {
    Map<String, WarsStructure> wars = new LinkedHashMap<>();
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Map<String, Set<String>> preRequestedUpdate = new LinkedHashMap<>();
    Map<String, Set<String>> initialRequest = new LinkedHashMap<>();
    Map<String, War> runningWars = new LinkedHashMap<>();
    EntityDamageByEntityEvent damageByEntityEvent;

    @EventHandler
    public void onEDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType().equals(EntityType.ARMOR_STAND)) {
            damageByEntityEvent = event;
            File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
            Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
            Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>() {
            }.getType();
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
                        if (Factions.getInstance().getByTag(factionRaid.getFaction()).equals(FPlayers.getInstance().getByPlayer(Bukkit.getPlayer(event.getDamager().getUniqueId())).getFaction())) {
                            Bukkit.getPlayer(event.getDamager().getUniqueId()).sendMessage(ChatColor.RED + "R u stupid, that's YOUR OWN totem");
                            event.setCancelled(true);
                        }
                    }
                }
            });
        }
    }

    @EventHandler
    public void onEDeath(EntityDeathEvent event) {
        if (damageByEntityEvent.getEntity().getUniqueId().equals(event.getEntity().getUniqueId())) {
            if (event.getEntity().getType().equals(EntityType.ARMOR_STAND)) {
                File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
                Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
                Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>() {
                }.getType();
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
                            wars.get(factionRaid.getWarHash()).setDefeatedFaction(Factions.getInstance().getByTag(factionRaid.getFaction()));
                            wars.get(factionRaid.getWarHash()).setWinnerFaction(FPlayers.getInstance().getByPlayer(Bukkit.getPlayer(damageByEntityEvent.getDamager().getUniqueId())).getFaction());
                            runningWars.get(factionRaid.getWarHash()).setEndTimer(factionRaid.getWarHash());
                            wars.get(factionRaid.getWarHash()).getStarter().getOnlinePlayers().forEach(player -> player.setScoreboard(manager.getNewScoreboard()));
                            wars.get(factionRaid.getWarHash()).getJoined().forEach(faction -> faction.getOnlinePlayers().forEach(player -> player.setScoreboard(manager.getNewScoreboard())));
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

        war.getObjective().getScoreboard().resetScores(ChatColor.BLUE + String.join(" ", preRequestedUpdate.get(warHash)));
        if (!war.getRequested().equals(war.getJoined())) {
            war.getObjective().getScore(ChatColor.BLUE + String.join(" ", requested)).setScore(23);
            preRequestedUpdate.replace(warHash, requested);
        } else {
            war.getObjective().getScoreboard().resetScores("Waiting for :");
            War theWar = new War(warHash, war);
            runningWars.put(warHash, theWar);
            Thread warThread = new Thread(theWar);
            warThread.start();
            Bukkit.broadcastMessage(String.valueOf(war.getWildernessChunks().size()));
            war.getWildernessChunks().forEach(chunk -> Bukkit.getServer().getWorld(war.getStarter().getFPlayerLeader().getPlayer().getWorld().getUID()).regenerateChunk(chunk.getX(), chunk.getZ()));
        }
    };

    WarChangeListener decliningListener = warHash -> {
        WarsStructure war = wars.get(warHash);
        Set<String> newRequestedSet = new HashSet<>();
        war.getRequested().forEach(faction -> newRequestedSet.add(faction.getTag()));
        Set<String> declined = new HashSet<>(initialRequest.get(warHash));
        declined.removeAll(newRequestedSet);

        war.getStarter().getFPlayerLeader().sendMessage(ChatColor.RED + "The faction(s) " + ChatColor.BOLD + String.join(" ", declined) + ChatColor.RED + "ran AWAY, maybe you where " + ChatColor.DARK_RED + ChatColor.BOLD.toString() + "TOO POWERFUL!!!!");

        war.getObjective().getScoreboard().resetScores(ChatColor.BLUE + String.join(" ", preRequestedUpdate.get(warHash)));
        if (!war.getJoined().isEmpty()) {
            war.getObjective().getScore(ChatColor.BLUE + String.join(" ", newRequestedSet)).setScore(23);
            preRequestedUpdate.replace(warHash, newRequestedSet);
        }

        if (newRequestedSet.isEmpty()) {

            war.getObjective().getScoreboard().resetScores("Waiting for :");
            File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
            Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
            Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>() {
            }.getType();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            if (FactionRaidListF.length() != 0) {
                try {
                    FactionRaids = gson.fromJson(Files.readString(FactionRaidListF.toPath()), FactionRaidMapType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            war.getStarter().getFPlayerLeader().getPlayer().sendMessage(ChatColor.RED + "Everybody ran AWAY, you where definitely " + ChatColor.DARK_RED + ChatColor.BOLD.toString() + "TOO POWERFUL!!!!");
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
    };

    public String create(Faction starter, List<Faction> requested, List<FLocation> totemChunks, List<Chunk> totemsWilderness) {
        String warHash = Hashing.sha384().hashInt(requested.hashCode()).toString();
        Scoreboard board = manager.getNewScoreboard();
        Objective warObjective = board.registerNewObjective("war", "dummy");

        Set<String> initialRequestedSet = new HashSet<>();
        requested.forEach(faction -> initialRequestedSet.add(faction.getTag()));
        initialRequest.put(warHash, initialRequestedSet);

        warObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        warObjective.setDisplayName(ChatColor.DARK_RED + ChatColor.BOLD.toString() + starter.getTag() + "'s" + ChatColor.DARK_RED + " WAR");
        preRequestedUpdate.put(warHash, initialRequest.get(warHash));

        warObjective.getScore(" ").setScore(25);
        warObjective.getScore("Waiting for :").setScore(24);
        warObjective.getScore(ChatColor.BLUE + String.join(" ", initialRequest.get(warHash))).setScore(23);

        starter.getOnlinePlayers().forEach(player -> player.setScoreboard(board));
        wars.put(warHash, new WarsStructure(starter, totemChunks, totemsWilderness, requested, decliningListener, warHash, joiningListener, warObjective));
        Bukkit.broadcastMessage(String.valueOf(wars.get(warHash).getWildernessChunks().size()));
        return warHash;
    }

    public String join(Faction joiner, String warHash, List<Chunk> totemsWilderness, List<FLocation> totemChunks) {
        if (!wars.containsKey(warHash)) {
            return "false";
        }
        wars.get(warHash).joinWar(joiner, totemChunks, totemsWilderness);
        joiner.getOnlinePlayers().forEach(player -> player.setScoreboard(wars.get(warHash).getObjective().getScoreboard()));
        Bukkit.broadcastMessage(String.valueOf(wars.get(warHash).getWildernessChunks().size()));
        return wars.get(warHash).getStarter().getTag();
    }

    public String decline(Faction denier, String warHash) {
        if (!wars.containsKey(warHash)) {
            return "false";
        }
        wars.get(warHash).declineWar(denier);
        return wars.get(warHash).getStarter().getTag();
    }

    public void joinAsAlly(Faction helper, String helped, String warHash) {
        wars.get(warHash).joinWarAsAlly(warHash, helper, helped);
    }

    public Boolean claimReward(Faction winner, String warHash) {
        if (wars.containsKey(warHash)) {
            return false;
        }
        WarsStructure war = wars.get(warHash);
        if (!war.getWinnerFaction().equals(winner)) {
            return false;
        }
        Random rand = new Random();
        Inventory rewardChest = Bukkit.createInventory(null, 9, war.getWinnerFaction().getTag()+"'s rewards");
        int enemies;
        if (winner.equals(war.getStarter())) {
            enemies = war.getAllies(warHash, winner.getTag()) + war.getDefeatedFaction().getOnlinePlayers().size();
        } else {
            enemies = war.getAllies(warHash, winner.getTag()) + war.getDefeatedFaction().getOnlinePlayers().size() + war.getStarter().getOnlinePlayers().size();
        }
        double ratio = (double) enemies / winner.getOnlinePlayers().size() - ((double) enemies / winner.getOnlinePlayers().size())*war.getTime()*0.05;

        int nbDiamonds = Math.min(Math.toIntExact(Math.round(Math.max(20 + 8.61 * Math.log(ratio), 1.0))), 64);
        rewardChest.setItem(0, new ItemStack(Material.DIAMOND, nbDiamonds));

        for (int i = 1; i < 5; i++) {
            int rewardID = ThreadLocalRandom.current().nextInt(0, 5);
            switch (rewardID) {
                case 0:
                    ItemStack reward = new ItemStack(Material.POTION, 1);
                    RtagItem.edit(reward, tag -> {
                        tag.set("Goliath", "display", "Name");
                        tag.set(7603975, "CustomPotionColor");
                    });
                    rewardChest.setItem(i, reward);
                    break;
            }
        }


        winner.getFPlayerLeader().getPlayer().openInventory(rewardChest);
        wars.remove(warHash);
        return true;
    }
}
class War implements Runnable  {
    String warHash;
    int time = 10;
    Objective objective;
    WarsStructure war;
    Map<String, Boolean> endTimer = new LinkedHashMap<>();

    private volatile boolean stopCounting = true;

    public War(String warHash, WarsStructure war) {
        this.warHash = warHash;
        this.objective = war.getObjective();
        this.war = war;
    }
    @Override
    public void run() {
        war.getStarter().getOnlinePlayers().forEach(player -> {
            Titles.sendTitle(player, 10, 30, 10, ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME", ChatColor.DARK_PURPLE+ChatColor.ITALIC.toString()+"You have 5 minutes");
            player.sendMessage(ChatColor.LIGHT_PURPLE+"Regenerating Chunks...");
        });
        war.getJoined().forEach(faction -> faction.getOnlinePlayers().forEach(player -> {
            Titles.sendTitle(player, 10, 30, 10, ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME", ChatColor.DARK_PURPLE+ChatColor.ITALIC.toString()+"You have 5 minutes");
            player.sendMessage(ChatColor.LIGHT_PURPLE+"Regenerating Chunks...");
        }));
        objective.getScore(ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME").setScore(24);
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
        war.getStarter().getOnlinePlayers().forEach(player -> Titles.sendTitle(player, 10, 30, 10, ChatColor.DARK_RED+ChatColor.BOLD.toString()+"FIGHT !!!!", ChatColor.GREEN+"Now totems are VULNERABLE"));
        war.getJoined().forEach(faction -> faction.getOnlinePlayers().forEach(player -> Titles.sendTitle(player, 10, 30, 10, ChatColor.DARK_RED+ChatColor.BOLD.toString()+"END OF PREPARATION TIME", ChatColor.GREEN+"Now totems are VULNERABLE")));

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
                    float health = (float) Math.min(-0.73 * (war.getStarter().getPower() + war.getStarter().getOnlinePlayers().size()) + 96.3, 16.0);
                    tag.setHealth(health);
                });
            }
        }

        Map<String, FactionRaid> finalFactionRaids1 = FactionRaids;
        war.getJoined().forEach(faction -> {
            Entity aliveTotem = Bukkit.getEntity(UUID.fromString(finalFactionRaids1.get(faction.getId()).getTotemUUID()));
            RtagEntity.edit(aliveTotem, tag -> {
                tag.set(ChatColor.DARK_RED+faction.getTag()+"'s raid TOTEM", "CustomName");
                tag.set(true, "CustomNameVisible");
                tag.set(true, "PersistenceRequired");
                tag.set(true, "NoGravity");
                tag.set(false, "Invulnerable");
                float health = (float) Math.min(-0.73 * (faction.getPower() + faction.getOnlinePlayers().size()) + 96.3, 16.0);
                tag.setHealth(health);
            });
        });

        war.getTotemChunks().get(war.getStarter().getId()).forEach(fLocation -> {
            war.getStarter().getFPlayerLeader().attemptUnclaim(war.getStarter(), fLocation, false);
        });

        war.getJoined().forEach(joidedFac -> {
            war.getTotemChunks().get(joidedFac.getId()).forEach(fLocation -> {
                joidedFac.getFPlayerLeader().attemptUnclaim(joidedFac, fLocation, false);
            });
        });

        objective.getScoreboard().resetScores(ChatColor.YELLOW+ChatColor.BOLD.toString()+"PREPARATION TIME");
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
        while (stopCounting) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (chronometer != 0) {
                objective.getScoreboard().resetScores(ChatColor.DARK_PURPLE + String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(chronometer - 1) % 60, TimeUnit.SECONDS.toSeconds(chronometer - 1) % 60));
            }
            objective.getScore(ChatColor.DARK_PURPLE+String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(chronometer) % 60, TimeUnit.SECONDS.toSeconds(chronometer) % 60)).setScore(23);
            chronometer = chronometer +1;
            isStopCounting();
        }

        war.setTime(chronometer);

        if (Bukkit.getEntity(UUID.fromString(FactionRaids.get(war.getStarter().getId()).getTotemUUID())).isDead()) {
            Entity deadTotem = Bukkit.getEntity(UUID.fromString(FactionRaids.get(war.getStarter().getId()).getTotemUUID()));
            Entity ArmorStandTotem = w.spawnEntity(deadTotem.getLocation(), EntityType.ARMOR_STAND);
            RtagEntity.edit(ArmorStandTotem, tag -> {
                tag.set(ChatColor.DARK_RED+war.getStarter().getTag()+"'s raid TOTEM", "CustomName");
                tag.set(true, "CustomNameVisible");
                tag.set(true, "PersistenceRequired");
                tag.set(true, "NoGravity");
                tag.set(true, "Invulnerable");
            });
            Location totemLocation = ArmorStandTotem.getLocation();
            FactionRaids.replace(war.getStarter().getId(),new FactionRaid(war.getStarter().getTag(),
                    ArmorStandTotem.getUniqueId().toString(),
                    false, "",
                    totemLocation.getX(), totemLocation.getY(), totemLocation.getZ()));
        }

        Map<String, FactionRaid> finalFactionRaids = FactionRaids;
        war.getJoined().forEach(faction -> {
            if (Bukkit.getEntity(UUID.fromString(finalFactionRaids.get(faction.getId()).getTotemUUID())).isDead()) {
                Entity deadTotem = Bukkit.getEntity(UUID.fromString(finalFactionRaids.get(faction.getId()).getTotemUUID()));
                Entity ArmorStandTotem = w.spawnEntity(deadTotem.getLocation(), EntityType.ARMOR_STAND);
                RtagEntity.edit(ArmorStandTotem, tag -> {
                    tag.set(ChatColor.DARK_RED+faction.getTag()+"'s raid TOTEM", "CustomName");
                    tag.set(true, "CustomNameVisible");
                    tag.set(true, "PersistenceRequired");
                    tag.set(true, "NoGravity");
                    tag.set(true, "Invulnerable");
                });
                Location totemLocation = ArmorStandTotem.getLocation();
                finalFactionRaids.replace(faction.getId(),new FactionRaid(faction.getTag(),
                        ArmorStandTotem.getUniqueId().toString(),
                        false, "",
                        totemLocation.getX(), totemLocation.getY(), totemLocation.getZ()));
            } else {
                Entity aliveTotem = Bukkit.getEntity(UUID.fromString(finalFactionRaids.get(faction.getId()).getTotemUUID()));
                RtagEntity.edit(aliveTotem, tag -> {
                    tag.set(ChatColor.DARK_RED+faction.getTag()+"'s raid TOTEM", "CustomName");
                    tag.set(true, "CustomNameVisible");
                    tag.set(true, "PersistenceRequired");
                    tag.set(true, "NoGravity");
                    tag.set(true, "Invulnerable");
                });
                Location totemLocation = aliveTotem.getLocation();
                finalFactionRaids.replace(faction.getId(),new FactionRaid(faction.getTag(),
                        aliveTotem.getUniqueId().toString(),
                        false, "",
                        totemLocation.getX(), totemLocation.getY(), totemLocation.getZ()));
            }
        });
        try {
            FileWriter JSONwriter = new FileWriter(FactionRaidListF);
            JSONwriter.write(gson.toJson(FactionRaids));
            JSONwriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        war.getDefeatedFaction().getFPlayerLeader().getPlayer().sendMessage(ChatColor.RED+"Bruh. You lost, better next time");
        war.getWinnerFaction().getFPlayerLeader().getPlayer().sendMessage(ChatColor.RED+"You WON, click there to claim YOUR reward (EMPTY your INVENTORY before claiming it)");
        war.getStarter().getOnlinePlayers().forEach(player -> {
            Titles.sendTitle(player, 10, 30, 10, ChatColor.WHITE+ChatColor.BOLD.toString()+"WAR IS FINISHED", "");
        });
        war.getJoined().forEach(faction -> faction.getOnlinePlayers().forEach(player -> {
            Titles.sendTitle(player, 10, 30, 10, ChatColor.WHITE+ChatColor.BOLD.toString()+"WAR IS FINISHED", "");
        }));

        BaseComponent[] claimRewardMessage = new ComponentBuilder("Claim MY MONEY").color(ChatColor.GREEN.asBungee()).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claimReward "+warHash)).create();
        war.getWinnerFaction().getFPlayerLeader().getPlayer().spigot().sendMessage(claimRewardMessage);
    }

    public void setEndTimer(String warHash) {
        endTimer.put(warHash, false);
    }

    public void isStopCounting() {
        stopCounting = this.endTimer.getOrDefault(warHash, true);
        if (stopCounting) {
            this.endTimer.remove(warHash);
        }
    }
}