package ch.jdtt.Commands;

import ch.jdtt.BurierRaid.BurierRaid;
import ch.jdtt.BurierRaid.FactionRaid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

public class moveTotem implements CommandExecutor {
    BurierRaid plugin;
    File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
    Collection<FactionRaid> FactionRaids = new ArrayList<>();
    public moveTotem(BurierRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("moveTotem")){
            return false;
        }
        if (!FactionRaidListF.exists()) {
            plugin.onEnable();
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (FactionRaidListF.length() != 0) {
            try {
                JsonArray FactionRaidsRAW = gson.fromJson(Files.readString(FactionRaidListF.toPath()), JsonArray.class);
                FactionRaidsRAW.forEach(factionsJSON -> FactionRaids.add(gson.fromJson(factionsJSON.getAsJsonObject(), FactionRaid.class)));
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
        if (!fplayer.isInOwnTerritory()){
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are NOT in a claim of your faction!");
            return false;
        }
        if (!Bukkit.getPlayer(sender.getName()).getWorld().getEnvironment().equals(World.Environment.NORMAL)){
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You NEED to be in the OverWorld!");
            return false;
        }
        if (!FactionRaids.isEmpty()) {
            JsonArray FactionsInfoArray = gson.fromJson(gson.toJson(FactionRaids), JsonArray.class);
            boolean noTotem = true;
            boolean inWar = true;
            for (int i = 0; i < FactionsInfoArray.size(); i++) {
                if(!FactionsInfoArray.get(i).getAsJsonObject().get("facID").getAsString().equals(faction.getId())) {
                    sender.sendMessage(ChatColor.RED + "You DON'T have a totem!");
                    sender.sendMessage( ChatColor.BLUE + "You can place one with: "+ChatColor.BOLD+"/placeTotem");
                    noTotem = true;
                    break;
                } else {
                    noTotem = false;
                }
                if(FactionsInfoArray.get(i).getAsJsonObject().get("facID").getAsString().equals(faction.getId())) {
                    inWar = FactionsInfoArray.get(i).getAsJsonObject().get("isInWar").getAsBoolean();
                    if (inWar){
                        sender.sendMessage( ChatColor.RED + "You are in a "+ChatColor.BOLD+"WAR!");
                        break;
                    }
                }
            }
            if (noTotem || inWar){
                return false;
            }
        }

        World w = Bukkit.getPlayer(sender.getName()).getWorld();
        Location playerLoc = player.getLocation();
        sender.sendMessage(ChatColor.RED + "t1");
        sender.sendMessage(String.valueOf(FactionRaids.size()));
        for (JsonElement fac : gson.fromJson(gson.toJson(FactionRaids), JsonArray.class)) {
            if (fac.getAsJsonObject().get("facID").getAsString().equals(faction.getId())) {
                String totemUUID = fac.getAsJsonObject().get("totemUUID").getAsString();
                sender.sendMessage(ChatColor.RED + "t2");
                for (Entity ArmorStandTotem : w.getEntities()) {
                    if (ArmorStandTotem.getUniqueId().toString().equals(totemUUID)) {
                        sender.sendMessage(ChatColor.RED + "t3");
                        ArmorStandTotem.teleport(new Location(w, playerLoc.getBlockX() + 0.5,
                                playerLoc.getBlockY(), playerLoc.getBlockZ() + 0.5));
                        Location totemLocation = ArmorStandTotem.getLocation();
                        sender.sendMessage(ChatColor.RED + "t4");
                        FactionRaids.removeIf(factionRaid -> (factionRaid.getFacID().equals(faction.getId())));
                        sender.sendMessage(ChatColor.RED + "t5");
                        FactionRaids.add(new FactionRaid(faction.getTag(),
                                faction.getId(), ArmorStandTotem.getUniqueId().toString(), false,
                                totemLocation.getX(), totemLocation.getY(), totemLocation.getZ()));

                    }
                }
                break;
            }
        }
        sender.sendMessage(ChatColor.RED + "t6");
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