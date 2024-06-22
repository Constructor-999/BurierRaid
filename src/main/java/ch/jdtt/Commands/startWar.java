package ch.jdtt.Commands;

import ch.jdtt.BurierRaid.*;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.*;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class startWar implements CommandExecutor {
    BurierRaid plugin;
    File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
    Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
    Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>(){}.getType();
    Utils utils = new Utils();
    WarConstructor warConstructor;
    public startWar(BurierRaid plugin, WarConstructor warConstructor) {
        this.plugin = plugin;
        this.warConstructor = warConstructor;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("startWar")){
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage( ChatColor.RED + "You need to add one or multiple factions that you are in ENEMY");
            return false;
        }
        if (!FactionRaidListF.exists()) {
            plugin.onEnable();
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (FactionRaidListF.length() != 0) {
            try {
                FactionRaids = gson.fromJson(Files.readString(FactionRaidListF.toPath()), FactionRaidMapType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            sender.sendMessage(ChatColor.RED+"You don't have a totem, like every other faction :>");
            return false;
        }
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(Bukkit.getPlayer(sender.getName()));
        Player player = Bukkit.getPlayer(sender.getName());
        if (!fplayer.hasFaction()) {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are NOT in a faction!");
            return false;
        }
        Faction faction = fplayer.getFaction();
        if (!faction.getFPlayerLeader().getName().equalsIgnoreCase(sender.getName())){
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are NOT the creator of this faction!");
            return false;
        }
        if (!FactionRaids.isEmpty()) {
            if(!FactionRaids.containsKey(faction.getId())) {
                sender.sendMessage(ChatColor.RED + "You DON'T have a totem!");
                sender.sendMessage( ChatColor.BLUE + "You can place one with: "+ChatColor.BOLD+"/placeTotem");
                return false;
            }
            if (FactionRaids.get(faction.getId()).getInWar()) {
                sender.sendMessage( ChatColor.RED + "You are already in a "+ChatColor.BOLD+"WAR!");
                return false;
            }
        }
        for (String arg : args) {
            if (Factions.getInstance().getAllFactions().stream().noneMatch(factionCheck -> (factionCheck.getTag().equals(arg)))){
                sender.sendMessage(ChatColor.RED+"The faction "+ChatColor.BOLD+arg+ChatColor.RED+" does NOT exist");
                return false;
            }
            if (!FactionRaids.containsKey(Factions.getInstance().getByTag(arg).getId())){
                sender.sendMessage(ChatColor.RED+"The faction "+ChatColor.BOLD+arg+ChatColor.RED+" does NOT have a totem");
                return false;
            }
            if (FactionRaids.get(Factions.getInstance().getByTag(arg).getId()).getInWar()){
                sender.sendMessage(ChatColor.RED+"The faction "+ChatColor.BOLD+arg+ChatColor.RED+" is already in a "+ChatColor.BOLD+"WAR!");
                return false;
            }
            if (Factions.getInstance().getByTag(arg).getFPlayerLeader().isOffline()) {
                sender.sendMessage(ChatColor.RED+"The the owner of "+ChatColor.BOLD+arg+ChatColor.RED+" is OFFLINE!");
                return false;
            }
        }
        int baseClaimRadius = 1;
        int wildernessRadius = 4;
        World w = Bukkit.getPlayer(sender.getName()).getWorld();
        Location totemLocation = player.getLocation();
        for (Entity ArmorStandTotem : w.getEntities()) {
            if (ArmorStandTotem.getUniqueId().toString().equals(FactionRaids.get(faction.getId()).getTotemUUID())) {
                totemLocation = ArmorStandTotem.getLocation();
            }
        }
        Chunk totemChunk = w.getChunkAt(totemLocation);
        List<Chunk> totemClaimedChunks = new ArrayList<>();
        List<Chunk> totemWildernessChunks = new ArrayList<>();
        List<Chunk> factionChunkList = new ArrayList<>();
        faction.getAllClaims().forEach(fLocation -> factionChunkList.add(fLocation.getChunk()));
        for (int i = -baseClaimRadius; i <= baseClaimRadius; i++) {
            for (int j = -baseClaimRadius; j <=baseClaimRadius ; j++) {
                Chunk checkChunk = w.getChunkAt((totemChunk.getX()+i), totemChunk.getZ()+j);
                if (faction.getAllClaims().stream().anyMatch(fLocation -> (fLocation.getChunk().equals(checkChunk)))){
                    totemClaimedChunks.add(checkChunk);
                }
            }
        }
        totemClaimedChunks.forEach(totemClaimedcChunk -> {
            for (int i = -wildernessRadius; i <= wildernessRadius; i++) {
                for (int j = -wildernessRadius; j <=wildernessRadius ; j++) {
                    if (Math.round(Math.sqrt(i*i+j*j)) <= wildernessRadius) {
                        Chunk wildernessChunk = w.getChunkAt((totemClaimedcChunk.getX()+i), totemClaimedcChunk.getZ()+j);
                        if (totemWildernessChunks.stream().noneMatch(chunk -> (chunk.equals(wildernessChunk))) &&
                                totemClaimedChunks.stream().noneMatch(chunk -> (chunk.equals(wildernessChunk)))){
                            totemWildernessChunks.add(wildernessChunk);
                        }
                    }
                }
            }
        });
        if (totemWildernessChunks.stream().anyMatch(factionChunkList::contains)) {
            sender.sendMessage(ChatColor.RED+"The Totem CAN'T be moved here, it needs to follow that pattern:");
            sender.sendMessage(ChatColor.GRAY+"AAA"+ChatColor.GREEN+"/////"+ChatColor.GRAY+"AAA");
            sender.sendMessage(ChatColor.GRAY+"AA"+ChatColor.GREEN+"///////"+ChatColor.GRAY+"AA");
            sender.sendMessage(ChatColor.GRAY+"A"+ChatColor.GREEN+"/////////"+ChatColor.GRAY+"A");
            sender.sendMessage(ChatColor.GREEN+"///////////");
            sender.sendMessage(ChatColor.GREEN+"////"+ChatColor.BLUE+"CCC"+ChatColor.GREEN+"////");
            sender.sendMessage(ChatColor.GREEN+"////"+ChatColor.BLUE+"CCC"+ChatColor.GREEN+"////");
            sender.sendMessage(ChatColor.GREEN+"////"+ChatColor.BLUE+"CCC"+ChatColor.GREEN+"////");
            sender.sendMessage(ChatColor.GREEN+"///////////");
            sender.sendMessage(ChatColor.GRAY+"A"+ChatColor.GREEN+"/////////"+ChatColor.GRAY+"A");
            sender.sendMessage(ChatColor.GRAY+"AA"+ChatColor.GREEN+"///////"+ChatColor.GRAY+"AA");
            sender.sendMessage(ChatColor.GRAY+"AAA"+ChatColor.GREEN+"/////"+ChatColor.GRAY+"AAA");
            sender.sendMessage("A:any claim, /:Wilderness, C:claimed by the totem's faction");
            return false;
        }
        final double[] totemChunksObsidianDensity = {0.0};
        final double[] totemChunksWaterDensity = {0.0};
        final double[] totemChunksLavaDensity = {0.0};
        final double[] totemChunksOthersDensity = {0.0};
        AtomicInteger thresholdChunk = new AtomicInteger();
        Location finalTotemLocation = totemLocation;
        totemClaimedChunks.forEach(chunk -> {
            if ((utils.getDensity(chunk, finalTotemLocation, Material.AIR) + utils.getDensity(chunk, finalTotemLocation, Material.GRASS) +
                    utils.getDensity(chunk, finalTotemLocation, Material.GRASS_PATH) + utils.getDensity(chunk, finalTotemLocation, Material.LONG_GRASS)
                    + utils.getDensity(chunk, finalTotemLocation, Material.DIRT)) <= 0.6 || chunk.equals(totemChunk)) {
                totemChunksObsidianDensity[0] = totemChunksObsidianDensity[0] +  utils.getDensity(chunk, finalTotemLocation, Material.OBSIDIAN);
                totemChunksWaterDensity[0] = totemChunksWaterDensity[0] +  utils.getDensity(chunk, finalTotemLocation, Material.WATER);
                totemChunksWaterDensity[0] = totemChunksWaterDensity[0] +  utils.getDensity(chunk, finalTotemLocation, Material.WATER_BUCKET);
                totemChunksWaterDensity[0] = totemChunksWaterDensity[0] +  utils.getDensity(chunk, finalTotemLocation, Material.STATIONARY_WATER);
                totemChunksLavaDensity[0] = totemChunksLavaDensity[0] +  utils.getDensity(chunk, finalTotemLocation, Material.LAVA);
                totemChunksLavaDensity[0] = totemChunksLavaDensity[0] +  utils.getDensity(chunk, finalTotemLocation, Material.LAVA_BUCKET);
                totemChunksLavaDensity[0] = totemChunksLavaDensity[0] +  utils.getDensity(chunk, finalTotemLocation, Material.STATIONARY_LAVA);
                int anyBlockCounter = 0;
                for (int i = 0; i <= 15; i++) {
                    for (int j = 0; j <= 15; j++) {
                        for (int k = finalTotemLocation.getBlockY()-2; k <= finalTotemLocation.getBlockY()+3; k++) {
                            if (!chunk.getBlock(i, k, j).getType().equals(Material.OBSIDIAN) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.WATER) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.WATER_BUCKET) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.STATIONARY_WATER) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.LAVA) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.LAVA_BUCKET) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.STATIONARY_LAVA) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.DIRT) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.GRASS) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.GRASS_PATH) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.LONG_GRASS) &&
                                    !chunk.getBlock(i, k, j).getType().equals(Material.AIR)){
                                anyBlockCounter++;
                            }
                        }
                    }
                }
                totemChunksOthersDensity[0] = totemChunksOthersDensity[0] + (double) anyBlockCounter / (16 * 16 * 6 - 2);
                thresholdChunk.getAndIncrement();
            }
        });
        totemChunksObsidianDensity[0] = totemChunksObsidianDensity[0] / thresholdChunk.get();
        totemChunksWaterDensity[0] = totemChunksWaterDensity[0] / thresholdChunk.get();
        totemChunksLavaDensity[0] = totemChunksLavaDensity[0] / thresholdChunk.get();
        totemChunksOthersDensity[0] = totemChunksOthersDensity[0] / thresholdChunk.get();
        double totemProtectionDensity = totemChunksObsidianDensity[0] * 0.65 +
                totemChunksWaterDensity[0] * 0.1 +
                totemChunksLavaDensity[0] * 0.2 +
                totemChunksOthersDensity[0] * 0.05;
        if (totemProtectionDensity >= 0.5) {
            sender.sendMessage(ChatColor.RED+"Sorry but you need to remove blocks you have a density of :");
            sender.sendMessage(ChatColor.DARK_RED+String.valueOf(totemProtectionDensity));
            sender.sendMessage(ChatColor.BLUE+"The maximum density is 0.65");
            return false;
        }

        List<FLocation> totemClaims = new ArrayList<>();
        factionChunkList.forEach(chunk -> {
            totemClaims.add(FLocation.wrap(chunk));
        });

        List<Faction> factionsRequested = new ArrayList<>();
        for (String arg : args) {
            factionsRequested.add(Factions.getInstance().getByTag(arg));
        }

        String warHash = warConstructor.create(faction, factionsRequested, totemClaims, totemWildernessChunks);
        BaseComponent[] JoinMessage = new ComponentBuilder("Join the WAR!").color(ChatColor.GOLD.asBungee()).bold(true).underlined(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/joinWar " + warHash)).create();
        BaseComponent[] DeclineMessage = new ComponentBuilder("I decline").color(ChatColor.GOLD.asBungee()).bold(true).underlined(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/declineWar " + warHash)).create();
        for (String arg : args) {
            Factions.getInstance().getByTag(arg).getFPlayerLeader().getPlayer().sendMessage(ChatColor.RED+""+ChatColor.BOLD+faction.getTag()+" started a war with your faction.");
            Factions.getInstance().getByTag(arg).getFPlayerLeader().getPlayer().spigot().sendMessage(JoinMessage);
            Factions.getInstance().getByTag(arg).getFPlayerLeader().getPlayer().spigot().sendMessage(DeclineMessage);
        }
        for (Entity ArmorStandTotem : w.getEntities()) {
            if (ArmorStandTotem.getUniqueId().toString().equals(FactionRaids.get(faction.getId()).getTotemUUID())) {
                FactionRaids.replace(faction.getId(), new FactionRaid(faction.getTag(),
                        ArmorStandTotem.getUniqueId().toString(),
                        true, warHash,
                        totemLocation.getX(), totemLocation.getY(), totemLocation.getZ()));
            }
        }
        try {
            FileWriter JSONwriter = new FileWriter(FactionRaidListF);
            JSONwriter.write(gson.toJson(FactionRaids));
            JSONwriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}