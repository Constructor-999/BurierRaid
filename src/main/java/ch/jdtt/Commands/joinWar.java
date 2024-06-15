package ch.jdtt.Commands;

import ch.jdtt.BurierRaid.BurierRaid;
import ch.jdtt.BurierRaid.FactionRaid;
import ch.jdtt.BurierRaid.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class joinWar implements CommandExecutor {
    BurierRaid plugin;
    File FactionRaidListF = new File("./plugins/BurierRaid/FactionRaid.json");
    Map<String, FactionRaid> FactionRaids = new LinkedHashMap<>();
    Type FactionRaidMapType = new TypeToken<Map<String, FactionRaid>>(){}.getType();
    Utils utils = new Utils();
    public joinWar(BurierRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("joinWar")){
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage( ChatColor.RED + "You fool don't have directly access to this command");
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
        for (int i = 0; i < FactionRaids.size(); i++) {
            if (FactionRaids.get(i).getWarHash().equals(args[0])) {
                sender.sendMessage( ChatColor.RED + "You thought you could beat a robot? Wrong HASH you stupid");
                return false;
            }
        }
        return true;
    }
}