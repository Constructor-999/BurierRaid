package ch.jdtt.Commands;

import ch.jdtt.BurierRaid.BurierRaid;
import ch.jdtt.BurierRaid.FactionRaid;
import ch.jdtt.BurierRaid.Utils;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.saicone.rtag.RtagEntity;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import com.massivecraft.factions.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class placeTotem implements CommandExecutor {
    BurierRaid plugin;
    File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
    Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
    Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>(){}.getType();
    Utils utils = new Utils();
    public placeTotem(BurierRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("placeTotem")){
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
        if (!fplayer.isInOwnTerritory()){
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are NOT in a claim of your faction!");
            return false;
        }
        if (!Bukkit.getPlayer(sender.getName()).getWorld().getEnvironment().equals(World.Environment.NORMAL)){
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You NEED to be in the OverWorld!");
            return false;
        }
        if (!FactionRaids.isEmpty()) {
            if(FactionRaids.containsKey(faction.getId())) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You ALREADY have a totem!");
                return false;
            }
        }
        int baseClaimRadius = 1;
        int wildernessRadius = 4;
        World w = Bukkit.getPlayer(sender.getName()).getWorld();
        Location playerLoc = player.getLocation();
        Chunk totemChunk = w.getChunkAt(playerLoc);
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
        totemClaimedChunks.forEach(chunk -> {
            if ((utils.getDensity(chunk, playerLoc, Material.AIR) + utils.getDensity(chunk, playerLoc, Material.GRASS) +
                    utils.getDensity(chunk, playerLoc, Material.GRASS_PATH) + utils.getDensity(chunk, playerLoc, Material.LONG_GRASS)
                    + utils.getDensity(chunk, playerLoc, Material.DIRT)) <= 0.6 || chunk.equals(totemChunk)) {
                totemChunksObsidianDensity[0] = totemChunksObsidianDensity[0] +  utils.getDensity(chunk, playerLoc, Material.OBSIDIAN);
                totemChunksWaterDensity[0] = totemChunksWaterDensity[0] +  utils.getDensity(chunk, playerLoc, Material.WATER);
                totemChunksWaterDensity[0] = totemChunksWaterDensity[0] +  utils.getDensity(chunk, playerLoc, Material.WATER_BUCKET);
                totemChunksWaterDensity[0] = totemChunksWaterDensity[0] +  utils.getDensity(chunk, playerLoc, Material.STATIONARY_WATER);
                totemChunksLavaDensity[0] = totemChunksLavaDensity[0] +  utils.getDensity(chunk, playerLoc, Material.LAVA);
                totemChunksLavaDensity[0] = totemChunksLavaDensity[0] +  utils.getDensity(chunk, playerLoc, Material.LAVA_BUCKET);
                totemChunksLavaDensity[0] = totemChunksLavaDensity[0] +  utils.getDensity(chunk, playerLoc, Material.STATIONARY_LAVA);
                int anyBlockCounter = 0;
                for (int i = 0; i <= 15; i++) {
                    for (int j = 0; j <= 15; j++) {
                        for (int k = playerLoc.getBlockY()-2; k <= playerLoc.getBlockY()+3; k++) {
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
        Entity ArmorStandTotem = w.spawnEntity(new Location(w, playerLoc.getBlockX()+0.5,
                playerLoc.getBlockY(), playerLoc.getBlockZ()+0.5), EntityType.ARMOR_STAND);
        RtagEntity.edit(ArmorStandTotem, tag -> {
            tag.set(ChatColor.DARK_RED+faction.getTag()+"'s raid TOTEM", "CustomName");
            tag.set(true, "CustomNameVisible");
            tag.set(true, "PersistenceRequired");
            tag.set(true, "NoGravity");
            tag.set(true, "Invulnerable");
        });
        Location totemLocation = ArmorStandTotem.getLocation();
        FactionRaids.put(faction.getId(),new FactionRaid(faction.getTag(),
                ArmorStandTotem.getUniqueId().toString(),
                false,
                totemLocation.getX(), totemLocation.getY(), totemLocation.getZ()));
        try {
            FileWriter JSONwriter = new FileWriter(FactionRaidListF);
            JSONwriter.write(gson.toJson(FactionRaids));
            JSONwriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sender.sendMessage(ChatColor.GREEN + "Now you CAN start "+ChatColor.BOLD+"WARS !!!");
        sender.sendMessage(ChatColor.BLUE + "Wrong place ?, you can always move it with: "+ChatColor.BOLD+"/movetotem");
        return true;
    }
}