package ch.jdtt.Commands;

import ch.jdtt.BurierRaid.BurierRaid;
import ch.jdtt.BurierRaid.FactionRaid;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.saicone.rtag.RtagEntity;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class placeTotem implements CommandExecutor {
    BurierRaid plugin;
    File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
    Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
    Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>(){}.getType();
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
        World w = Bukkit.getPlayer(sender.getName()).getWorld();
        Location playerLoc = player.getLocation();
        Chunk totemChunk = w.getChunkAt(playerLoc);
        List<Chunk> totemClaimedChunks = new ArrayList<>();
        List<Chunk> totemWildernessChunks = new ArrayList<>();
        List<Chunk> factonChunkList = new ArrayList<>();
        faction.getAllClaims().forEach(fLocation -> {
            factonChunkList.add(fLocation.getChunk());
        });
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <=1 ; j++) {
                Chunk checkChunk = w.getChunkAt((totemChunk.getX()+i), totemChunk.getZ()+j);
                if (faction.getAllClaims().stream().anyMatch(fLocation -> (fLocation.getChunk().equals(checkChunk)))){
                    totemClaimedChunks.add(checkChunk);
                }
            }
        }
        totemClaimedChunks.forEach(totemClaimedcChunk -> {
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <=2 ; j++) {
                    if (Math.round(Math.sqrt(i*i+j*j)) <= 2) {
                        Chunk wildernessChunk = w.getChunkAt((totemClaimedcChunk.getX()+i), totemClaimedcChunk.getZ()+j);
                        if (totemWildernessChunks.stream().noneMatch(chunk -> (chunk.equals(wildernessChunk))) &&
                                totemClaimedChunks.stream().noneMatch(chunk -> (chunk.equals(wildernessChunk)))){
                            totemWildernessChunks.add(wildernessChunk);
                        }
                    }
                }
            }
        });
        sender.sendMessage(String.valueOf(totemClaimedChunks.size()));
        sender.sendMessage(String.valueOf(totemWildernessChunks.size()));
        sender.sendMessage(String.valueOf(CollectionUtils.containsAny(totemWildernessChunks, factonChunkList)));
        sender.sendMessage(String.valueOf(faction.getAllClaims().stream().anyMatch(fLocation -> (fLocation.getChunk().equals(totemChunk)))));

        if (!FactionRaids.isEmpty()) {
            if(FactionRaids.containsKey(faction.getId())) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You ALREADY have a totem!");
                return false;
            }
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