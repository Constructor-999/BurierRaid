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

public class declineWar implements CommandExecutor {
    BurierRaid plugin;
    File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
    Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
    Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>(){}.getType();
    Utils utils = new Utils();
    WarConstructor warConstructor;

    public declineWar(BurierRaid plugin, WarConstructor warConstructor) {
        this.plugin = plugin;
        this.warConstructor = warConstructor;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("declineWar")){
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
        String warStarter = warConstructor.decline(faction, args[0]);
        sender.sendMessage(ChatColor.GREEN + "You successfully avoided the war of "+ChatColor.DARK_RED+warStarter);
        return true;
    }
}