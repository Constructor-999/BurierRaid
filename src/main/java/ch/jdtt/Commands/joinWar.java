package ch.jdtt.Commands;

import ch.jdtt.BurierRaid.BurierRaid;
import ch.jdtt.BurierRaid.FactionRaid;
import ch.jdtt.BurierRaid.Utils;
import ch.jdtt.BurierRaid.WarConstructor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class joinWar implements CommandExecutor {
    BurierRaid plugin;
    File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
    Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
    Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>(){}.getType();
    Utils utils = new Utils();
    WarConstructor warConstructor;

    public joinWar(BurierRaid plugin, WarConstructor warConstructor) {
        this.plugin = plugin;
        this.warConstructor = warConstructor;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("joinWar")){
            return false;
        }
        if (!FactionRaidListF.exists()) {
            plugin.onEnable();
        }
        if (args.length == 0) {
            sender.sendMessage( ChatColor.RED + "You fool don't have directly access to this command");
            return false;
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

        if (FactionRaids.values().stream().noneMatch(factionRaid -> factionRaid.getWarHash().equals(args[0]))) {
            sender.sendMessage( ChatColor.RED + "You thought you could beat a robot? Wrong HASH you stupid");
            return false;
        }
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(Bukkit.getPlayer(sender.getName()));
        Player player = Bukkit.getPlayer(sender.getName());
        if (!fplayer.hasFaction()) {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are NOT in a faction!");
            return false;
        }
        Faction faction = fplayer.getFaction();
        if (FactionRaids.get(faction.getId()).getInWar()) {
            sender.sendMessage( ChatColor.RED + "You are in a "+ChatColor.BOLD+"WAR!");
            return false;
        }
        int baseClaimRadius = 1;
        int wildernessRadius = 4;
        Location totemLocation = player.getLocation();
        World w = Bukkit.getPlayer(sender.getName()).getWorld();
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
            sender.sendMessage(ChatColor.RED+"Sorry but you need to remove blocks to join that War, you have a density of :");
            sender.sendMessage(ChatColor.DARK_RED+String.valueOf(totemProtectionDensity));
            sender.sendMessage(ChatColor.BLUE+"The maximum density is 0.65");
            return false;
        }
        String warStarter = warConstructor.join(faction, args[0]);
        for (Entity ArmorStandTotem : w.getEntities()) {
            if (ArmorStandTotem.getUniqueId().toString().equals(FactionRaids.get(faction.getId()).getTotemUUID())) {
                FactionRaids.replace(faction.getId(), new FactionRaid(faction.getTag(),
                        ArmorStandTotem.getUniqueId().toString(),
                        true, args[0],
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
        sender.sendMessage(ChatColor.GREEN + "You successfully joined the war of "+ChatColor.DARK_RED+warStarter);
        return true;
    }
}