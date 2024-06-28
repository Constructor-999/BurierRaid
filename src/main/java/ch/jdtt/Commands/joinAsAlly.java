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
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class joinAsAlly implements CommandExecutor {
    BurierRaid plugin;
    File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
    Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
    Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>(){}.getType();
    Utils utils = new Utils();
    WarConstructor warConstructor;

    public joinAsAlly(BurierRaid plugin, WarConstructor warConstructor) {
        this.plugin = plugin;
        this.warConstructor = warConstructor;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("joinAsAlly")){
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
        if (!fplayer.hasFaction()) {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are NOT in a faction!");
            return false;
        }
        Faction faction = fplayer.getFaction();
        if (FactionRaids.get(faction.getId()).getInWar()) {
            sender.sendMessage( ChatColor.RED + "You are in a "+ChatColor.BOLD+"WAR!");
            return false;
        }

        warConstructor.joinAsAlly(faction, args[1], args[0]);
        sender.sendMessage(ChatColor.GREEN + "You successfully helped the war of "+ChatColor.DARK_RED+args[1]);

        Player player = Bukkit.getPlayer(sender.getName());
        Location totemLocation = player.getLocation();
        World w = Bukkit.getPlayer(sender.getName()).getWorld();
        for (Entity ArmorStandTotem : w.getEntities()) {
            if (ArmorStandTotem.getUniqueId().toString().equals(FactionRaids.get(faction.getId()).getTotemUUID())) {
                totemLocation = ArmorStandTotem.getLocation();
            }
        }

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
        return true;
    }
}
